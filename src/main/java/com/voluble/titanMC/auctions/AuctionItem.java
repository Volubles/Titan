package com.voluble.titanMC.auctions;

import java.util.Objects;

public record AuctionItem(long id, int slot, byte[] data) {
	public AuctionItem {
		if (id < 1) throw new IllegalArgumentException("id must be positive");
		if (slot < 0 || slot >= 27) throw new IllegalArgumentException("slot must be between 0 and 26");
		data = Objects.requireNonNull(data, "data").clone();
	}

	@Override
	public byte[] data() {
		return data.clone();
	}
}
