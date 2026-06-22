package com.voluble.titanMC.mines.storage;

import com.voluble.titanMC.mines.Mine;
import com.voluble.titanMC.mines.MineResetDefinition;
import com.voluble.titanMC.mines.WeightedPalette;
import com.voluble.titanMC.util.RegionUtils;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MineStorageTest {

	private ServerMock server;
	private Plugin plugin;

	@BeforeEach
	void setUp() {
		server = MockBukkit.mock();
		plugin = MockBukkit.createMockPlugin();
	}

	@AfterEach
	void tearDown() {
		MockBukkit.unmock();
	}

	@Test
	void persistsTheLatestSnapshotAndDeletionInOrder() {
		var world = server.addSimpleWorld("mines");
		var palette = new WeightedPalette();
		palette.addOrUpdate(Material.STONE, 1);
		var mine = new Mine(
			"alpha",
			new RegionUtils.Cuboid(world.getUID(), 0, 0, 0, 1, 1, 1),
			60,
			true,
			100,
			palette
		);
		mine.setResetDefinition(new MineResetDefinition.Template("woodfarm_v1"));

		try (MineStorage storage = new MineStorage(plugin)) {
			storage.saveMine(mine);
			mine.setResetIntervalSeconds(120);
			storage.saveMine(mine);
			storage.flush();

			try (MineStorage reader = new MineStorage(plugin)) {
				Map<String, Mine> loaded = reader.loadAll();
				assertEquals(120, loaded.get("alpha").getResetIntervalSeconds());
				assertEquals(
					"woodfarm_v1",
					((MineResetDefinition.Template) loaded.get("alpha").getResetDefinition()).templateId()
				);
			}

			storage.deleteMine("alpha");
		}
		try (MineStorage reader = new MineStorage(plugin)) {
			assertFalse(reader.loadAll().containsKey("alpha"));
		}
	}
}
