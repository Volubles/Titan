package com.voluble.titanMC.mines;

import com.voluble.titanMC.util.RegionUtils;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MineLookupTest {

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
	void regionIndexReturnsTheFirstContainingCuboidWithoutBuildingAResultList() {
		var world = server.addSimpleWorld("lookup");
		RegionUtils.Cuboid cuboid = new RegionUtils.Cuboid(
			world.getUID(), 0, 0, 0, 31, 10, 31
		);
		RegionUtils.RegionIndex index = new RegionUtils.RegionIndex();
		index.add(cuboid);

		assertEquals(cuboid, index.getFirstAt(new Location(world, 5, 5, 5)));
		assertNull(index.getFirstAt(new Location(world, 40, 5, 40)));
	}
}
