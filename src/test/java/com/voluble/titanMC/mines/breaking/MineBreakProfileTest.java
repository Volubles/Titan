package com.voluble.titanMC.mines.breaking;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MineBreakProfileTest {
	@Test
	void allowListOnlyAllowsConfiguredBlockMaterials() {
		MineBreakProfile profile = new MineBreakProfile.AllowList(Set.of(
			Material.OAK_LOG, Material.OAK_STAIRS, Material.OAK_SLAB
		));

		assertTrue(profile.allows(Material.OAK_LOG));
		assertTrue(profile.allows(Material.OAK_STAIRS));
		assertFalse(profile.allows(Material.STONE));
		assertFalse(profile.allows(Material.AIR));
	}
}
