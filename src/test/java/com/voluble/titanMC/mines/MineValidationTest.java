package com.voluble.titanMC.mines;

import com.voluble.titanMC.util.RegionUtils;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MineValidationTest {

	private static final UUID WORLD_ID = UUID.randomUUID();
	private ServerMock server;

	@BeforeEach
	void startServer() {
		server = MockBukkit.mock();
	}

	@AfterEach
	void stopServer() {
		MockBukkit.unmock();
	}

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

	@Test
	void squaredDistanceMatchesDistanceWithoutRequiringSquareRootAtCallSites() {
		var world = server.addSimpleWorld("distance");
		RegionUtils.Cuboid cuboid = new RegionUtils.Cuboid(
			world.getUID(), 0, 0, 0, 10, 10, 10
		);
		Location location = new Location(world, 13, 14, 10);

		assertTrue(cuboid.distanceSquaredTo(location) == 25.0);
		assertTrue(cuboid.distanceTo(location) == 5.0);
	}
}
