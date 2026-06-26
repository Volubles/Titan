package com.voluble.titanMC.auctions;

import com.voluble.titanMC.display.notice.MessageDefaults;
import com.voluble.titanMC.display.notice.PluginMessageService;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

final class AuctionDeliveryService {
	private final Plugin plugin;
	private final AuctionStorage storage;
	private final PluginMessageService messages;
	private final NamespacedKey receiptKey;
	private final Set<Long> inFlight = new HashSet<>();

	AuctionDeliveryService(Plugin plugin, AuctionStorage storage, PluginMessageService messages) {
		this.plugin = plugin;
		this.storage = storage;
		this.messages = messages;
		this.receiptKey = new NamespacedKey(plugin, "auction_delivery");
	}

	void recover(Player player) {
		storage.loadDeliveries(player.getUniqueId()).whenComplete((deliveries, failure) ->
			Bukkit.getScheduler().runTask(plugin, () -> {
				if (failure != null) {
					plugin.getLogger().log(Level.SEVERE, "Could not recover auction deliveries for " + player.getName(), failure);
					return;
				}
				Map<Long, ItemStack> receipts = receipts(player);
				for (AuctionDelivery delivery : deliveries) {
					ItemStack existing = receipts.get(delivery.id());
					if (delivery.state() == AuctionDelivery.State.DELIVERED) {
						if (existing != null) removeReceipt(existing);
					} else if (existing != null) {
						if (inFlight.add(delivery.id())) complete(player, delivery);
					} else {
						deliver(player, delivery);
					}
				}
			})
		);
	}

	void deliver(Player player, AuctionDelivery delivery) {
		if (!inFlight.add(delivery.id())) return;
		if (!player.isOnline()) {
			inFlight.remove(delivery.id());
			return;
		}
		ItemStack item = ItemStack.deserializeBytes(delivery.itemData());
		if (!fits(player)) {
			inFlight.remove(delivery.id());
			messages.send(player, MessageDefaults.AUCTIONS_DELIVERY_FULL);
			return;
		}
		addReceipt(item, delivery.id());
		Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
		if (!leftovers.isEmpty()) {
			inFlight.remove(delivery.id());
			plugin.getLogger().severe("Could not place reserved auction delivery " + delivery.id() + " in player inventory");
			return;
		}
		complete(player, delivery);
	}

	boolean hasReceipt(ItemStack item) {
		return receiptId(item) != null;
	}

	private void complete(Player player, AuctionDelivery delivery) {
		storage.completeDelivery(delivery.id(), player.getUniqueId()).whenComplete((ignored, failure) ->
			Bukkit.getScheduler().runTask(plugin, () -> {
				inFlight.remove(delivery.id());
				if (failure != null) {
					plugin.getLogger().log(Level.SEVERE, "Could not complete auction delivery " + delivery.id(), failure);
					return;
				}
				for (ItemStack item : player.getInventory().getContents()) {
					if (java.util.Objects.equals(receiptId(item), delivery.id())) removeReceipt(item);
				}
			})
		);
	}

	private Map<Long, ItemStack> receipts(Player player) {
		Map<Long, ItemStack> receipts = new HashMap<>();
		for (ItemStack item : player.getInventory().getContents()) {
			Long id = receiptId(item);
			if (id != null) receipts.put(id, item);
		}
		return receipts;
	}

	private void addReceipt(ItemStack item, long deliveryId) {
		var meta = item.getItemMeta();
		meta.getPersistentDataContainer().set(receiptKey, PersistentDataType.LONG, deliveryId);
		item.setItemMeta(meta);
	}

	private void removeReceipt(ItemStack item) {
		if (item == null || item.getType().isAir()) return;
		var meta = item.getItemMeta();
		meta.getPersistentDataContainer().remove(receiptKey);
		item.setItemMeta(meta);
	}

	private Long receiptId(ItemStack item) {
		if (item == null || item.getType().isAir()) return null;
		return item.getItemMeta().getPersistentDataContainer().get(receiptKey, PersistentDataType.LONG);
	}

	private static boolean fits(Player player) {
		for (ItemStack existing : player.getInventory().getStorageContents()) {
			if (existing == null || existing.getType().isAir()) return true;
		}
		return false;
	}
}
