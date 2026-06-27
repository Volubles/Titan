package com.voluble.titanMC.cinematics.runtime;

import com.voluble.titanMC.cinematics.model.CameraPathDefinition;
import com.voluble.titanMC.cinematics.model.CameraPoint;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CameraPathInterpolatorTest {
	private ServerMock server;
	private World world;

	@BeforeEach
	void setUp() {
		server = MockBukkit.mock();
		world = server.addSimpleWorld("cinematic_path");
	}

	@AfterEach
	void tearDown() {
		MockBukkit.unmock();
	}

	@Test
	void zeroTickSegmentTeleportsToNextCanvasPoint() {
		CameraPathDefinition path = new CameraPathDefinition(true, List.of(
			point(0, 4, 10.0),
			point(40, 8, 20.0),
			point(0, 0, 0.0)
		));

		Location instant = CameraPathInterpolator.locationAt(path.points(), 0);
		Location halfway = CameraPathInterpolator.locationAt(path.points(), 20);

		assertEquals(10.0, instant.getX(), 0.001);
		assertEquals(15.0, halfway.getX(), 0.001);
	}

	private CameraPoint point(int tick, int slot, double x) {
		return new CameraPoint(tick, slot, world.getName(), x, 64.0, 0.0, 0.0f, 0.0f);
	}
}
