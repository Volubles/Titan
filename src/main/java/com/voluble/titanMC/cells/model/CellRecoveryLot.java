package com.voluble.titanMC.cells.model;

import java.util.List;
import java.util.UUID;

public record CellRecoveryLot(long id, UUID ownerId, List<byte[]> items) {
	public CellRecoveryLot {
		if (id < 1) throw new IllegalArgumentException("recovery lot id must be positive");
		items = List.copyOf(items);
	}
}
