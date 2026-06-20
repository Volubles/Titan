package com.voluble.titanMC.regions.index;

import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.WorldId;

import java.util.List;

public final class RegionQueryCursor {

	long snapshotVersion = Long.MIN_VALUE;
	WorldId worldId;
	long chunkKey;
	List<RegionDefinition> candidates = List.of();
	boolean cached;

	public void reset() {
		snapshotVersion = Long.MIN_VALUE;
		worldId = null;
		chunkKey = 0L;
		candidates = List.of();
		cached = false;
	}

	void resetFor(long version) {
		reset();
		snapshotVersion = version;
	}
}
