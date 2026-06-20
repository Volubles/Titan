package com.voluble.titanMC.regions.selection;

import com.voluble.titanMC.regions.model.RegionGeometry;

import java.util.Objects;
import java.util.UUID;

public record SelectedRegion(UUID worldId, RegionGeometry geometry) {

	public SelectedRegion {
		Objects.requireNonNull(worldId, "worldId");
		Objects.requireNonNull(geometry, "geometry");
	}
}
