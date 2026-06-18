package com.voluble.titanMC.regions.model;

public record BlockPosition(WorldId worldId, int x, int y, int z) {

	public BlockPosition {
		if (worldId == null) throw new IllegalArgumentException("worldId must not be null");
	}
}
