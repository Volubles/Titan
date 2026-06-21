package com.voluble.titanMC.auctions;

import java.util.List;
import java.util.UUID;

public record AuctionLot(
	long id,
	long sourceLotId,
	int batchIndex,
	String positionId,
	long price,
	AuctionState state,
	UUID buyerId,
	String buyerName,
	long saleExpiresAt,
	long claimExpiresAt,
	List<byte[]> items
) {
	public AuctionLot {
		items = List.copyOf(items);
	}

	public AuctionLot atPosition(String positionId, long saleExpiresAt) {
		return new AuctionLot(id, sourceLotId, batchIndex, positionId, price, AuctionState.FOR_SALE, null, null, saleExpiresAt, 0, items);
	}

	public AuctionLot claimed(UUID buyerId, String buyerName, long claimExpiresAt) {
		return new AuctionLot(id, sourceLotId, batchIndex, positionId, price, AuctionState.CLAIMED, buyerId, buyerName, saleExpiresAt, claimExpiresAt, items);
	}

	public AuctionLot publicAccess() {
		return new AuctionLot(id, sourceLotId, batchIndex, positionId, price, AuctionState.PUBLIC, buyerId, buyerName, saleExpiresAt, claimExpiresAt, items);
	}
}
