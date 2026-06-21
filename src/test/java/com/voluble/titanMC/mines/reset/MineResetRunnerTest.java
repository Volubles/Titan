package com.voluble.titanMC.mines.reset;

import com.voluble.titanMC.mines.Mine;
import com.voluble.titanMC.mines.WeightedPalette;
import com.voluble.titanMC.util.RegionUtils;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MineResetRunnerTest {

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
	void skipsBlockWritesWhenThePaletteAlreadySelectedTheExistingMaterial() {
		var world = server.addSimpleWorld("reset");
		world.getBlockAt(0, 64, 0).setType(Material.STONE);
		world.getBlockAt(1, 64, 0).setType(Material.DIRT);
		world.getBlockAt(2, 64, 0).setType(Material.AIR);
		WeightedPalette palette = new WeightedPalette();
		palette.addOrUpdate(Material.STONE, 1);
		Mine mine = new Mine(
			"test",
			new RegionUtils.Cuboid(world.getUID(), 0, 64, 0, 2, 64, 0),
			900,
			true,
			1500,
			palette
		);
		MineResetRunner runner = new MineResetRunner(MockBukkit.createMockPlugin(), mine);

		MineResetWork work = runner.process(3, Long.MAX_VALUE);

		assertEquals(3, work.scannedBlocks());
		assertEquals(2, work.changedBlocks());
		assertTrue(work.finished());
		assertEquals(Material.STONE, world.getBlockAt(0, 64, 0).getType());
		assertEquals(Material.STONE, world.getBlockAt(1, 64, 0).getType());
		assertEquals(Material.STONE, world.getBlockAt(2, 64, 0).getType());
	}
}
