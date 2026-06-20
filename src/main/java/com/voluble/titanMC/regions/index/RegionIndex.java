package com.voluble.titanMC.regions.index;

import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.RegionId;
import com.voluble.titanMC.regions.model.RegionKey;
import com.voluble.titanMC.regions.model.WorldId;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class RegionIndex {

	private final AtomicReference<RegionIndexSnapshot> current = new AtomicReference<>(RegionIndexSnapshot.empty());

	public RegionIndexSnapshot snapshot() {
		return current.get();
	}

	public RegionReadView readView() {
		return new RegionReadView(current.get());
	}

	public RegionDefinition find(RegionId id) {
		return current.get().find(id);
	}

	public RegionDefinition find(WorldId worldId, RegionKey key) {
		return current.get().find(worldId, key);
	}

	public List<RegionDefinition> findAll(WorldId worldId, int x, int y, int z) {
		return current.get().findAll(worldId, x, y, z);
	}

	public boolean publish(RegionIndexSnapshot expected, RegionIndexSnapshot replacement) {
		Objects.requireNonNull(expected, "expected");
		Objects.requireNonNull(replacement, "replacement");
		if (replacement.version() <= expected.version()) {
			throw new IllegalArgumentException("replacement version must increase");
		}
		return current.compareAndSet(expected, replacement);
	}
}
