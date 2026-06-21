package com.voluble.titanMC.ranks.bukkit;

import com.voluble.titanMC.ranks.event.PlayerRankChangedEvent;
import com.voluble.titanMC.ranks.model.PrisonRank;
import com.voluble.titanMC.ranks.model.RankId;
import com.voluble.titanMC.ranks.model.RankupRequirement;
import com.voluble.titanMC.ranks.model.WardDefinition;
import com.voluble.titanMC.ranks.model.WardId;
import com.voluble.titanMC.ranks.persistence.PlayerRankStorage;
import com.voluble.titanMC.ranks.service.PlayerRankService;
import com.voluble.titanMC.ranks.service.RankCatalog;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerRankListenerTest {
	private static final WardId E = WardId.of("e");
	private static final RankId E4 = RankId.of("e4");
	private static final RankId E3 = RankId.of("e3");

	@TempDir Path directory;
	private ServerMock server;
	private Plugin plugin;
	private PlayerRankStorage storage;
	private PlayerRankService service;

	@BeforeEach
	void setUp() throws Exception {
		server = MockBukkit.mock();
		plugin = MockBukkit.createMockPlugin();
		storage = new PlayerRankStorage(directory.resolve("ranks.db"));
		RankCatalog catalog = new RankCatalog(
			List.of(new WardDefinition(E, "E Ward", List.of(E4, E3))),
			List.of(
				new PrisonRank(E4, E, "E4"),
				new PrisonRank(E3, E, "E3").withRankup(RankupRequirement.of(100L))
			)
		);
		Logger logger = Logger.getAnonymousLogger();
		logger.setLevel(Level.OFF);
		service = new PlayerRankService(catalog, storage,
			event -> server.getPluginManager().callEvent(event), logger);
		server.getPluginManager().registerEvents(new PlayerRankListener(service), plugin);
	}

	@AfterEach
	void tearDown() throws Exception {
		storage.close();
		MockBukkit.unmock();
	}

	@Test
	void joiningPlayerReceivesStarterRank() {
		Player player = server.addPlayer();

		assertEquals(E4, service.current(player.getUniqueId()).orElseThrow().rankId());
	}

	@Test
	void joiningPlayerKeepsExistingRank() throws Exception {
		Player player = server.addPlayer();
		service.apply(service.current(player.getUniqueId()).orElseThrow().withRank(E3, 5_000L));

		Player rejoiner = server.addPlayer("rejoiner");
		assertEquals(E4, service.current(rejoiner.getUniqueId()).orElseThrow().rankId());
		assertEquals(E3, service.current(player.getUniqueId()).orElseThrow().rankId());
	}

	@Test
	void firstJoinPublishesChangeEvent() {
		java.util.List<PlayerRankChangedEvent> captured = new java.util.ArrayList<>();
		server.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
			@org.bukkit.event.EventHandler
			public void onChange(PlayerRankChangedEvent event) { captured.add(event); }
		}, plugin);

		Player player = server.addPlayer();

		assertEquals(1, captured.size());
		assertEquals(player.getUniqueId(), captured.getFirst().playerId());
		assertTrue(captured.getFirst().previous().isEmpty());
	}
}
