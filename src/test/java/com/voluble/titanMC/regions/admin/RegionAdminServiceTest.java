package com.voluble.titanMC.regions.admin;

import com.voluble.titanMC.regions.model.BlockBox;
import com.voluble.titanMC.regions.model.CuboidGeometry;
import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.RegionKey;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.service.RegionEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionAdminServiceTest {

	@TempDir
	Path temporaryDirectory;

	@Test
	void managesCustomRegionsWithoutLosingFlagsOnRedefine() throws Exception {
		WorldId world = new WorldId(UUID.randomUUID());
		try (RegionEngine engine = RegionEngine.open(temporaryDirectory.resolve("admin.db"))) {
			RegionAdminService admin = new RegionAdminService(engine);
			assertTrue(admin.create(
				"yard", world, 250, new CuboidGeometry(new BlockBox(0, 0, 0, 10, 10, 10))
			).successful());
			assertTrue(admin.setFlag(
				"yard", world, ProtectionAction.PLAYER_PVP, ProtectionDecision.ALLOW
			).successful());
			assertTrue(admin.redefine(
				"yard", world, new CuboidGeometry(new BlockBox(20, 0, 20, 30, 10, 30))
			).successful());

			RegionDefinition region = admin.find(world, "yard");
			assertEquals(RegionKey.of("custom", "yard"), region.key());
			assertEquals(250, region.priority());
			assertEquals(ProtectionDecision.ALLOW, region.flags().decision(ProtectionAction.PLAYER_PVP));
			assertEquals(1, admin.list(world).size());
		}
	}
}
