package com.voluble.titanMC.cells.region;

import com.voluble.titanMC.cells.model.CellDefinition;
import com.voluble.titanMC.regions.model.BlockBox;
import com.voluble.titanMC.regions.model.CuboidGeometry;
import com.voluble.titanMC.regions.model.RegionAccessSet;
import com.voluble.titanMC.regions.model.RegionKey;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.service.RegionEngine;
import com.voluble.titanMC.ranks.model.WardId;
import com.voluble.titanMC.util.RegionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CellRegionServiceTest {
	@TempDir
	Path directory;

	@Test
	void reconcilesTheCompleteCellRegionProjection() throws Exception {
		UUID worldId = UUID.randomUUID();
		WorldId world = new WorldId(worldId);
		UUID owner = UUID.randomUUID();
		UUID member = UUID.randomUUID();
		CellDefinition retained = cell("retained", worldId, 10, 20);
		CellDefinition created = cell("created", worldId, 30, 40);

		try (RegionEngine engine = RegionEngine.open(directory.resolve("regions.db"))) {
			engine.create(
				RegionKey.of("cell", "retained"), world, 5,
				new CuboidGeometry(BlockBox.inclusive(0, 0, 0, 1, 1, 1))
			).join();
			engine.create(
				RegionKey.of("cell", "orphan"), world, CellRegionService.PRIORITY,
				new CuboidGeometry(BlockBox.inclusive(50, 0, 50, 55, 5, 55))
			).join();
			engine.create(
				RegionKey.of("custom", "keep"), world, 100,
				new CuboidGeometry(BlockBox.inclusive(60, 0, 60, 65, 5, 65))
			).join();

			CellRegionService service = new CellRegionService(engine);
			service.reconcile(
				List.of(retained, created),
				Map.of(
					retained.id(), RegionAccessSet.of(Set.of(owner), Set.of(member)),
					created.id(), RegionAccessSet.empty()
				)
			);

			var retainedRegion = engine.find(world, RegionKey.of("cell", "retained"));
			assertNotNull(retainedRegion);
			assertEquals(CellRegionService.PRIORITY, retainedRegion.priority());
			assertEquals(new CuboidGeometry(BlockBox.inclusive(10, 0, 10, 20, 5, 20)), retainedRegion.geometry());
			assertEquals(RegionAccessSet.of(Set.of(owner), Set.of(member)), retainedRegion.access());
			assertNotNull(engine.find(world, RegionKey.of("cell", "created")));
			assertNull(engine.find(world, RegionKey.of("cell", "orphan")));
			assertNotNull(engine.find(world, RegionKey.of("custom", "keep")));
		}
	}

	@Test
	void emptyProjectionDeletesOnlyManagedCellRegions() throws Exception {
		WorldId world = new WorldId(UUID.randomUUID());
		try (RegionEngine engine = RegionEngine.open(directory.resolve("empty.db"))) {
			engine.create(
				RegionKey.of("cell", "old"), world, 200,
				new CuboidGeometry(BlockBox.inclusive(0, 0, 0, 5, 5, 5))
			).join();
			engine.create(
				RegionKey.of("mine", "old"), world, 100,
				new CuboidGeometry(BlockBox.inclusive(10, 0, 10, 15, 5, 15))
			).join();
			CellRegionService service = new CellRegionService(engine);
			assertTrue(service.hasManagedRegions());

			service.reconcile(List.of(), Map.of());

			assertFalse(service.hasManagedRegions());
			assertNotNull(engine.find(world, RegionKey.of("mine", "old")));
		}
	}

	private static CellDefinition cell(String id, UUID worldId, int minimum, int maximum) {
		return new CellDefinition(
			id,
			WardId.of("e"),
			new RegionUtils.Cuboid(worldId, minimum, 0, minimum, maximum, 5, maximum),
			500,
			86400,
			604800,
			true
		);
	}
}
