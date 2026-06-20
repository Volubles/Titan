package com.voluble.titanMC.regions.model;

import com.voluble.titanMC.regions.index.RegionIndexOptions;
import com.voluble.titanMC.regions.index.RegionIndexSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionGeometryTest {

	@Test
	void polygonPrismIncludesEdgesAndRejectsPointsOutsideItsShape() throws Exception {
		PolygonPrismGeometry triangle = new PolygonPrismGeometry(
			List.of(new BlockPoint2(0, 0), new BlockPoint2(10, 0), new BlockPoint2(0, 10)),
			-5,
			5
		);

		assertTrue(triangle.contains(0, 0, 0));
		assertTrue(triangle.contains(5, 0, 5));
		assertFalse(triangle.contains(9, 0, 9));
		assertFalse(triangle.contains(1, 6, 1));
		assertEquals(new BlockBox(0, -5, 0, 11, 6, 11), triangle.bounds());

		WorldId world = new WorldId(new UUID(1L, 1L));
		RegionDefinition region = new RegionDefinition(
			new RegionId(new UUID(1L, 2L)), RegionKey.of("custom", "triangle"), world, 100,
			triangle, Instant.EPOCH, Instant.EPOCH
		);
		RegionIndexSnapshot snapshot = RegionIndexSnapshot.build(
			1L, List.of(region), RegionIndexOptions.defaults()
		);
		assertEquals(List.of(region), snapshot.findAll(world, 2, 0, 2));
		assertEquals(List.of(), snapshot.findAll(world, 9, 0, 9));
	}

	@Test
	void convexPolyhedronRequiresEveryFacePlane() {
		ConvexPolyhedronGeometry cube = new ConvexPolyhedronGeometry(
			new BlockBox(0, 0, 0, 11, 11, 11),
			List.of(
				new PolyhedronPlane(1, 0, 0, 10),
				new PolyhedronPlane(-1, 0, 0, 0),
				new PolyhedronPlane(0, 1, 0, 10),
				new PolyhedronPlane(0, -1, 0, 0),
				new PolyhedronPlane(0, 0, 1, 10),
				new PolyhedronPlane(0, 0, -1, 0)
			)
		);

		assertTrue(cube.contains(0, 0, 0));
		assertTrue(cube.contains(10, 10, 10));
		assertFalse(cube.contains(11, 5, 5));
		assertFalse(cube.contains(-1, 5, 5));
	}
}
