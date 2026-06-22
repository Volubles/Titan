package com.voluble.titanMC.auctions;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.LinkedHashMap;
import java.util.Map;

final class AuctionInventoryHolder implements InventoryHolder {
	private final long auctionId;
	private final Map<Integer, Long> itemIdsBySlot = new LinkedHashMap<>();
	private Inventory inventory;

	AuctionInventoryHolder(long auctionId) {
		this.auctionId = auctionId;
	}

	long auctionId() {
		return auctionId;
	}

	void bind(int slot, long itemId) {
		itemIdsBySlot.put(slot, itemId);
	}

	Long itemId(int slot) {
		return itemIdsBySlot.get(slot);
	}

	void remove(int slot) {
		itemIdsBySlot.remove(slot);
	}

	void inventory(Inventory inventory) {
		this.inventory = inventory;
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}
}
