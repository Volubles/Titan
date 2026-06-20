package com.voluble.titanMC.regions.index;

import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.RegionId;
import com.voluble.titanMC.regions.model.RegionKey;
import com.voluble.titanMC.regions.model.WorldId;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class RegionReadView {

	private final RegionIndexSnapshot snapshot;

	RegionReadView(RegionIndexSnapshot snapshot) {
		this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
	}

	public long version() {
		return snapshot.version();
	}

	public Collection<RegionDefinition> definitions() {
		return snapshot.definitions();
	}

	public RegionDefinition find(RegionId id) {
		return snapshot.find(id);
	}

	public RegionDefinition find(WorldId worldId, RegionKey key) {
		return snapshot.find(worldId, key);
	}

	public List<RegionDefinition> findAll(WorldId worldId, int x, int y, int z) {
		return snapshot.findAll(worldId, x, y, z);
	}

	public RegionVisitResult visitAll(
		WorldId worldId,
		int x,
		int y,
		int z,
		RegionQueryCursor cursor,
		RegionVisitor visitor
	) {
		return snapshot.visitAll(worldId, x, y, z, cursor, visitor);
	}

	public RegionVisitResult visitBatch(
		Iterable<BlockPosition> positions,
		RegionQueryCursor cursor,
		RegionBatchVisitor visitor
	) {
		return snapshot.visitBatch(positions, cursor, visitor);
	}
}
