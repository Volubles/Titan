package com.voluble.titanMC.regions.model;

public sealed interface RegionGeometry permits ConvexPolyhedronGeometry, CuboidGeometry, PolygonPrismGeometry {

	BlockBox bounds();

	boolean contains(int x, int y, int z);

	int complexity();
}
