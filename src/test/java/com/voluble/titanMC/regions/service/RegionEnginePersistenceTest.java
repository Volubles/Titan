package com.voluble.titanMC.regions.service;

import com.voluble.titanMC.regions.model.BlockBox;
import com.voluble.titanMC.regions.model.BlockPoint2;
import com.voluble.titanMC.regions.model.ConvexPolyhedronGeometry;
import com.voluble.titanMC.regions.model.CuboidGeometry;
import com.voluble.titanMC.regions.model.PolygonPrismGeometry;
import com.voluble.titanMC.regions.model.PolyhedronPlane;
import com.voluble.titanMC.regions.model.RegionGeometry;
import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.RegionKey;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.RegionFlagSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionEnginePersistenceTest {

	@TempDir
	Path temporaryDirectory;

	@Test
	void concurrentSubmissionsAreSerializedAndSurviveRestart() throws Exception {
		Path database = temporaryDirectory.resolve("regions.db");
		WorldId world = new WorldId(UUID.randomUUID());
		List<CompletableFuture<RegionMutationResult>> writes = new ArrayList<>();

		try (RegionEngine engine = RegionEngine.open(database)) {
			for (int index = 0; index < 100; index++) {
				writes.add(engine.create(
					RegionKey.of("cell", "cell_" + index),
					world,
					index % 5,
					new CuboidGeometry(new BlockBox(index * 32, 0, 0, index * 32 + 16, 16, 16))
				));
			}
			CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).join();
			assertTrue(writes.stream().map(CompletableFuture::join).allMatch(RegionMutationResult::successful));
			assertEquals(100, engine.snapshot().definitions().size());
		}

		try (RegionEngine reloaded = RegionEngine.open(database)) {
			assertEquals(100, reloaded.snapshot().definitions().size());
			assertEquals("cell_42", reloaded.findAll(world, 42 * 32, 1, 1).getFirst().key().name());
		}
	}

	@Test
	void duplicateKeyFailureDoesNotMutateSnapshotOrDatabase() throws Exception {
		Path database = temporaryDirectory.resolve("duplicate.db");
		WorldId world = new WorldId(UUID.randomUUID());
		RegionKey key = RegionKey.of("cell", "alpha");

		try (RegionEngine engine = RegionEngine.open(database)) {
			RegionMutationResult first = engine.create(key, world, 0, new CuboidGeometry(new BlockBox(0, 0, 0, 16, 16, 16))).join();
			RegionMutationResult duplicate = engine.create(key, world, 0, new CuboidGeometry(new BlockBox(32, 0, 0, 48, 16, 16))).join();
			assertInstanceOf(RegionMutationResult.Success.class, first);
			RegionMutationResult.Failure failure = assertInstanceOf(RegionMutationResult.Failure.class, duplicate);
			assertEquals(RegionMutationResult.Reason.DUPLICATE_KEY, failure.reason());
			assertEquals(1, engine.snapshot().definitions().size());
		}

		try (RegionEngine reloaded = RegionEngine.open(database)) {
			assertEquals(1, reloaded.snapshot().definitions().size());
			RegionDefinition stored = reloaded.find(world, key);
			assertEquals(new CuboidGeometry(new BlockBox(0, 0, 0, 16, 16, 16)), stored.geometry());
		}
	}

	@Test
	void polygonAndPolyhedronGeometrySurviveRestart() throws Exception {
		Path database = temporaryDirectory.resolve("shapes.db");
		WorldId world = new WorldId(UUID.randomUUID());
		RegionGeometry polygon = new PolygonPrismGeometry(
			List.of(new BlockPoint2(0, 0), new BlockPoint2(12, 0), new BlockPoint2(0, 12)),
			-10,
			20
		);
		RegionGeometry polyhedron = new ConvexPolyhedronGeometry(
			new BlockBox(20, 0, 20, 31, 11, 31),
			List.of(
				new PolyhedronPlane(1, 0, 0, 30),
				new PolyhedronPlane(-1, 0, 0, -20),
				new PolyhedronPlane(0, 1, 0, 10),
				new PolyhedronPlane(0, -1, 0, 0),
				new PolyhedronPlane(0, 0, 1, 30),
				new PolyhedronPlane(0, 0, -1, -20)
			)
		);

		try (RegionEngine engine = RegionEngine.open(database)) {
			assertTrue(engine.create(RegionKey.of("custom", "polygon"), world, 0, polygon).join().successful());
			assertTrue(engine.create(RegionKey.of("custom", "polyhedron"), world, 0, polyhedron).join().successful());
		}

		try (RegionEngine reloaded = RegionEngine.open(database)) {
			assertEquals(polygon, reloaded.find(world, RegionKey.of("custom", "polygon")).geometry());
			assertEquals(polyhedron, reloaded.find(world, RegionKey.of("custom", "polyhedron")).geometry());
		}
	}

	@Test
	void regionFlagsSurviveUpdatesAndRestart() throws Exception {
		Path database = temporaryDirectory.resolve("flags.db");
		WorldId world = new WorldId(UUID.randomUUID());
		RegionKey key = RegionKey.of("custom", "yard");
		RegionFlagSet flags = RegionFlagSet.empty()
			.with(ProtectionAction.PLAYER_PVP, ProtectionDecision.ALLOW)
			.with(ProtectionAction.BLOCK_BREAK, ProtectionDecision.DENY);

		try (RegionEngine engine = RegionEngine.open(database)) {
			RegionDefinition created = assertInstanceOf(
				RegionMutationResult.Success.class,
				engine.create(key, world, 0, new CuboidGeometry(new BlockBox(0, 0, 0, 16, 16, 16))).join()
			).region();
			assertTrue(engine.setFlags(created.id(), created.revision(), flags).join().successful());
		}

		try (RegionEngine reloaded = RegionEngine.open(database)) {
			assertEquals(flags, reloaded.find(world, key).flags());
		}
	}
}
