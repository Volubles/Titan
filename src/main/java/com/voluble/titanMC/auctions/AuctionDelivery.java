package com.voluble.titanMC.auctions;

import java.util.Objects;
import java.util.UUID;

public record AuctionDelivery(long id, long auctionId, long itemId, UUID playerId, byte[] itemData, State state) {
	public AuctionDelivery {
		if (id < 1 || auctionId < 1 || itemId < 1) throw new IllegalArgumentException("delivery ids must be positive");
		Objects.requireNonNull(playerId, "playerId");
		itemData = Objects.requireNonNull(itemData, "itemData").clone();
		Objects.requireNonNull(state, "state");
	}

	@Override
	public byte[] itemData() {
		return itemData.clone();
	}

	public enum State {
		PENDING,
		DELIVERED
	}
}
