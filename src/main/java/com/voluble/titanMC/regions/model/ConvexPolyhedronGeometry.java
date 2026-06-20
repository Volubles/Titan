package com.voluble.titanMC.regions.model;

import java.util.List;
import java.util.Objects;

public record ConvexPolyhedronGeometry(BlockBox bounds, List<PolyhedronPlane> planes) implements RegionGeometry {

	public ConvexPolyhedronGeometry {
		Objects.requireNonNull(bounds, "bounds");
		planes = List.copyOf(planes);
		if (planes.size() < 4) throw new IllegalArgumentException("convex polyhedron must contain at least four planes");
		if (planes.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("polyhedron planes must not contain null");
		}
	}

	@Override
	public boolean contains(int x, int y, int z) {
		return bounds.contains(x, y, z) && planes.stream().allMatch(plane -> plane.contains(x, y, z));
	}

	@Override
	public int complexity() {
		return planes.size();
	}
}
