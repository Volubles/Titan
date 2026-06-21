package com.voluble.titanMC.cells.model;

import com.voluble.titanMC.ranks.model.WardId;

import java.util.List;
import java.util.UUID;

public record CellRecoveryLot(long id, UUID ownerId, WardId wardId, List<byte[]> items) {
	public CellRecoveryLot {
		if (id < 1) throw new IllegalArgumentException("recovery lot id must be positive");
		java.util.Objects.requireNonNull(ownerId, "ownerId");
		java.util.Objects.requireNonNull(wardId, "wardId");
		items = List.copyOf(items);
	}
}
