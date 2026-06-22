package com.voluble.titanMC.auctions;

import com.voluble.titanMC.ranks.model.WardId;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AuctionLot(
	long id,
	long sourceLotId,
	int batchIndex,
	WardId wardId,
	String positionId,
	long price,
	AuctionState state,
	UUID buyerId,
	String buyerName,
	long saleExpiresAt,
	long claimExpiresAt,
	List<AuctionItem> items
) {
	public AuctionLot {
		Objects.requireNonNull(wardId, "wardId");
		Objects.requireNonNull(state, "state");
		items = List.copyOf(Objects.requireNonNull(items, "items"));
	}

	public AuctionLot atPosition(String positionId, long saleExpiresAt) {
		return new AuctionLot(id, sourceLotId, batchIndex, wardId, positionId, price, AuctionState.FOR_SALE, null, null, saleExpiresAt, 0, items);
	}

	public AuctionLot claimed(UUID buyerId, String buyerName, long claimExpiresAt) {
		return new AuctionLot(id, sourceLotId, batchIndex, wardId, positionId, price, AuctionState.CLAIMED, buyerId, buyerName, saleExpiresAt, claimExpiresAt, items);
	}

	public AuctionLot publicAccess() {
		return new AuctionLot(id, sourceLotId, batchIndex, wardId, positionId, price, AuctionState.PUBLIC, buyerId, buyerName, saleExpiresAt, claimExpiresAt, items);
	}
}
