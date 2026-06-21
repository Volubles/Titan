package com.voluble.titanMC.mines;

import com.voluble.titanMC.util.RegionUtils;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MineDepletionTest {

	@Test
	void cachedThresholdMatchesDisplayedRemainingPercentage() {
		Mine mine = mine(100);
		mine.setAutoResetBelowPercent(25);

		for (int broken = 0; broken <= 100; broken++) {
			mine.setBrokenBlocks(broken);
			assertEquals(
				mine.getRemainingPercent() <= 25,
				mine.shouldAutoResetByDepletion(),
				"broken=" + broken
			);
		}
	}

	@Test
	void cuboidChangesRecalculateVolumeAndThreshold() {
		Mine mine = mine(10);
		mine.setAutoResetBelowPercent(50);
		mine.setBrokenBlocks(5);
		assertTrue(mine.shouldAutoResetByDepletion());

		mine.setCuboid(cuboid(20));

		assertEquals(20, mine.getTotalBlockCountSafe());
		assertFalse(mine.shouldAutoResetByDepletion());
	}

	private static Mine mine(int blocks) {
		return new Mine("test", cuboid(blocks), 900, true, 1500, new WeightedPalette());
	}

	private static RegionUtils.Cuboid cuboid(int blocks) {
		return new RegionUtils.Cuboid(UUID.randomUUID(), 0, 0, 0, blocks - 1, 0, 0);
	}
}
