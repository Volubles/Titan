package com.voluble.titanMC.auctions;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.EquipmentSlot;

import java.util.logging.Level;

public final class AuctionListener implements Listener {
	private final Plugin plugin;
	private final AuctionService auctions;

	public AuctionListener(Plugin plugin, AuctionService auctions) {
		this.plugin = plugin;
		this.auctions = auctions;
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) return;
		Block block = event.getClickedBlock();
		if (block == null) return;
		AuctionLot signLot = auctions.atSign(block);
		if (signLot != null) {
			event.setCancelled(true);
			auctions.purchase(event.getPlayer(), signLot);
			return;
		}
		AuctionLot chestLot = auctions.atChest(block);
		if (chestLot == null) return;
		event.setCancelled(true);
		if (!auctions.canOpen(event.getPlayer(), chestLot)) {
			event.getPlayer().sendMessage(chestLot.state() == AuctionState.FOR_SALE
				? "Buy this mystery chest using its sign."
				: "This chest is temporarily reserved for its buyer.");
			return;
		}
		auctions.open(event.getPlayer(), chestLot);
	}

	@EventHandler
	public void onBreak(BlockBreakEvent event) {
		if (!protectedBlock(event.getBlock())) return;
		event.setCancelled(true);
		if (!mayDiscard(event.getPlayer())) {
			event.getPlayer().sendMessage("Auction blocks can only be removed by an auction administrator in creative mode.");
			return;
		}
		try {
			if (auctions.discardAt(event.getBlock())) {
				event.getPlayer().sendMessage("Auction permanently discarded.");
			}
		} catch (IllegalStateException exception) {
			plugin.getLogger().log(Level.SEVERE, "Could not discard auction", exception);
			event.getPlayer().sendMessage("The auction could not be removed. Check the server log.");
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onClick(InventoryClickEvent event) {
		if (auctions.hasDeliveryReceipt(event.getCurrentItem()) || auctions.hasDeliveryReceipt(event.getCursor())) {
			event.setCancelled(true);
			return;
		}
		Inventory top = event.getView().getTopInventory();
		if (!(top.getHolder() instanceof AuctionInventoryHolder holder)) return;
		event.setCancelled(true);
		if (!(event.getWhoClicked() instanceof Player player)) return;
		if (event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) return;
		auctions.claim(player, holder, event.getRawSlot());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onDrag(InventoryDragEvent event) {
		if (event.getView().getTopInventory().getHolder() instanceof AuctionInventoryHolder
			|| auctions.hasDeliveryReceipt(event.getOldCursor())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		auctions.recoverDeliveries(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDrop(PlayerDropItemEvent event) {
		if (auctions.hasDeliveryReceipt(event.getItemDrop().getItemStack())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onSwapHands(PlayerSwapHandItemsEvent event) {
		if (auctions.hasDeliveryReceipt(event.getMainHandItem())
			|| auctions.hasDeliveryReceipt(event.getOffHandItem())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onDeath(PlayerDeathEvent event) {
		var iterator = event.getDrops().iterator();
		while (iterator.hasNext()) {
			var item = iterator.next();
			if (!auctions.hasDeliveryReceipt(item)) continue;
			iterator.remove();
			event.getItemsToKeep().add(item);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onMove(InventoryMoveItemEvent event) {
		if (physicalLot(event.getSource()) != null || physicalLot(event.getDestination()) != null
			|| auctions.hasDeliveryReceipt(event.getItem())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		event.blockList().removeIf(block -> auctions.atChest(block) != null || auctions.atSign(block) != null);
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		event.blockList().removeIf(block -> auctions.atChest(block) != null || auctions.atSign(block) != null);
	}

	@EventHandler
	public void onPistonExtend(BlockPistonExtendEvent event) {
		if (event.getBlocks().stream().anyMatch(block -> protectedBlock(block)
			|| protectedBlock(block.getRelative(event.getDirection())))) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPistonRetract(BlockPistonRetractEvent event) {
		if (event.getBlocks().stream().anyMatch(this::protectedBlock)) event.setCancelled(true);
	}

	private AuctionLot physicalLot(Inventory inventory) {
		return inventory.getHolder() instanceof Chest chest ? auctions.atChest(chest.getBlock()) : null;
	}

	private boolean protectedBlock(Block block) {
		return auctions.atChest(block) != null || auctions.atSign(block) != null;
	}

	static boolean mayDiscard(Player player) {
		return player.getGameMode() == GameMode.CREATIVE && player.hasPermission("titanmc.auction.admin");
	}
}
