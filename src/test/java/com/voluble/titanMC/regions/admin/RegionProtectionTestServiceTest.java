package com.voluble.titanMC.regions.admin;

import com.voluble.titanMC.regions.model.BlockBox;
import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.model.CuboidGeometry;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionResolution;
import com.voluble.titanMC.regions.protection.policy.ProtectionBypass;
import com.voluble.titanMC.regions.protection.policy.RegionPolicyRegistry;
import com.voluble.titanMC.regions.protection.policy.WorldProtectionDefaults;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import com.voluble.titanMC.regions.service.RegionEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionProtectionTestServiceTest {

	@TempDir
	Path temporaryDirectory;

	@Test
	void reportsMatchingRegionsInPriorityOrderAndTheWinningDecision() throws Exception {
		WorldId world = new WorldId(UUID.randomUUID());
		ProtectionActor player = ProtectionActor.player(UUID.randomUUID(), Set.of());
		try (RegionEngine engine = RegionEngine.open(temporaryDirectory.resolve("test-command.db"))) {
			RegionAdminService admin = new RegionAdminService(engine);
			CuboidGeometry geometry = new CuboidGeometry(new BlockBox(0, 0, 0, 16, 16, 16));
			assertTrue(admin.create("low", world, 100, geometry).successful());
			assertTrue(admin.create("high", world, 200, geometry).successful());
			assertTrue(admin.setFlag(
				"low", world, ProtectionAction.BLOCK_PLACE, ProtectionDecision.DENY
			).successful());
			assertTrue(admin.setFlag(
				"high", world, ProtectionAction.BLOCK_PLACE, ProtectionDecision.ALLOW
			).successful());
			ProtectionService protection = protection(engine, world, ProtectionBypass.none());
			RegionProtectionTestService tests = new RegionProtectionTestService(engine, protection);

			var result = tests.test(
				player,
				ProtectionAction.BLOCK_PLACE,
				new BlockPosition(world, 5, 5, 5)
			);

			assertEquals(ProtectionDecision.ALLOW, result.resolution().decision());
			assertEquals(200, result.resolution().decidingPriority().orElseThrow());
			assertEquals(
				java.util.List.of("high", "low"),
				result.matchingRegions().stream().map(region -> region.key().name()).toList()
			);
		}
	}

	@Test
	void stillReportsMatchingRegionsWhenAdminBypassShortCircuitsEvaluation() throws Exception {
		WorldId world = new WorldId(UUID.randomUUID());
		ProtectionActor adminActor = ProtectionActor.player(
			UUID.randomUUID(),
			Set.of("titanmc.protection.bypass")
		);
		try (RegionEngine engine = RegionEngine.open(temporaryDirectory.resolve("test-bypass.db"))) {
			RegionAdminService admin = new RegionAdminService(engine);
			assertTrue(admin.create(
				"spawn",
				world,
				100,
				new CuboidGeometry(new BlockBox(0, 0, 0, 16, 16, 16))
			).successful());
			ProtectionService protection = protection(
				engine,
				world,
				ProtectionBypass.permission("titanmc.protection.bypass")
			);
			RegionProtectionTestService tests = new RegionProtectionTestService(engine, protection);

			var result = tests.test(
				adminActor,
				ProtectionAction.BLOCK_BREAK,
				new BlockPosition(world, 5, 5, 5)
			);

			assertEquals(ProtectionResolution.Reason.BYPASS, result.resolution().reason());
			assertEquals(1, result.matchingRegions().size());
			assertTrue(result.resolution().evaluations().isEmpty());
		}
	}

	@Test
	void reportsDenyWhenEqualPriorityRegionsConflict() throws Exception {
		WorldId world = new WorldId(UUID.randomUUID());
		ProtectionActor player = ProtectionActor.player(UUID.randomUUID(), Set.of());
		try (RegionEngine engine = RegionEngine.open(temporaryDirectory.resolve("test-conflict.db"))) {
			RegionAdminService admin = new RegionAdminService(engine);
			CuboidGeometry geometry = new CuboidGeometry(new BlockBox(0, 0, 0, 16, 16, 16));
			assertTrue(admin.create("allowing", world, 100, geometry).successful());
			assertTrue(admin.create("denying", world, 100, geometry).successful());
			assertTrue(admin.setFlag(
				"allowing", world, ProtectionAction.BLOCK_INTERACT, ProtectionDecision.ALLOW
			).successful());
			assertTrue(admin.setFlag(
				"denying", world, ProtectionAction.BLOCK_INTERACT, ProtectionDecision.DENY
			).successful());
			RegionProtectionTestService tests = new RegionProtectionTestService(
				engine,
				protection(engine, world, ProtectionBypass.none())
			);

			var result = tests.test(
				player,
				ProtectionAction.BLOCK_INTERACT,
				new BlockPosition(world, 5, 5, 5)
			);

			assertEquals(ProtectionDecision.DENY, result.resolution().decision());
			assertEquals(2, result.resolution().evaluations().size());
			assertEquals(100, result.resolution().decidingPriority().orElseThrow());
		}
	}

	private static ProtectionService protection(
		RegionEngine engine,
		WorldId world,
		ProtectionBypass bypass
	) {
		return ProtectionService.forEngine(
			engine,
			RegionPolicyRegistry.builder().build(),
			WorldProtectionDefaults.builder()
				.worldDefault(world, ProtectionDecision.DENY)
				.build(),
			bypass
		);
	}
}
