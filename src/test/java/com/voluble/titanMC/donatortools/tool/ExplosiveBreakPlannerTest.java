package com.voluble.titanMC.donatortools.tool;

import com.voluble.titanMC.donatortools.MockBukkitDonatorToolsTestSupport;
import com.voluble.titanMC.donatortools.config.DonatorToolsConfiguration;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExplosiveBreakPlannerTest extends MockBukkitDonatorToolsTestSupport {

	@Test
	void includesOnlyConfiguredAndProtectionAllowedAdditionalBlocks() {
		var world = server.addSimpleWorld("explosive_plan");
		var player = server.addPlayer();
		var center = world.getBlockAt(10, 64, 10);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					center.getRelative(dx, dy, dz).setType(Material.STONE);
				}
			}
		}
		center.getRelative(1, 0, 0).setType(Material.DIAMOND_ORE);
		ExplosiveBreakPlanner planner = new ExplosiveBreakPlanner(
			() -> new DonatorToolsConfiguration(true, Set.of(Material.STONE, Material.DIAMOND_ORE)),
			(actor, block) -> block.getX() <= center.getX()
		);

		var blocks = planner.additionalBlocks(player, center);

		assertEquals(17, blocks.size());
		assertFalse(blocks.contains(center));
		assertFalse(blocks.contains(center.getRelative(1, 0, 0)));
	}
}
