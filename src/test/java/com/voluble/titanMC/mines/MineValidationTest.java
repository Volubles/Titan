package com.voluble.titanMC.mines;

import com.voluble.titanMC.util.RegionUtils;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MineValidationTest {

	private static final UUID WORLD_ID = UUID.randomUUID();

	@Test
	void validatesNamesUsedAsStorageKeys() {
		assertNull(MineValidation.validateName("mine-01_alpha"));
		assertTrue(MineValidation.validateName("mine.with.path") != null);
		assertTrue(MineValidation.validateName("") != null);
	}

	@Test
	void limitsSelectionVolume() {
		RegionUtils.Cuboid allowed = new RegionUtils.Cuboid(WORLD_ID, 0, 0, 0, 199, 99, 999);
		RegionUtils.Cuboid tooLarge = new RegionUtils.Cuboid(WORLD_ID, 0, 0, 0, 200, 99, 999);

		assertNull(MineValidation.validateCuboid(allowed));
		assertTrue(MineValidation.validateCuboid(tooLarge) != null);
	}

	@Test
	void cuboidIntersectionIsInclusiveAndWorldAware() {
		RegionUtils.Cuboid first = new RegionUtils.Cuboid(WORLD_ID, 0, 0, 0, 10, 10, 10);
		RegionUtils.Cuboid touching = new RegionUtils.Cuboid(WORLD_ID, 10, 10, 10, 20, 20, 20);
		RegionUtils.Cuboid separate = new RegionUtils.Cuboid(WORLD_ID, 11, 0, 0, 20, 10, 10);
		RegionUtils.Cuboid otherWorld = new RegionUtils.Cuboid(UUID.randomUUID(), 0, 0, 0, 10, 10, 10);

		assertTrue(first.intersects(touching));
		assertFalse(first.intersects(separate));
		assertFalse(first.intersects(otherWorld));
	}
}
