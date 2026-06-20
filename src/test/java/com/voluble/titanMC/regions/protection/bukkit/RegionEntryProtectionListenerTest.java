package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.admin.RegionAdminService;
import com.voluble.titanMC.regions.model.BlockBox;
import com.voluble.titanMC.regions.model.CuboidGeometry;
import com.voluble.titanMC.regions.model.RegionTextFlag;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.policy.ProtectionBypass;
import com.voluble.titanMC.regions.protection.service.RegionEntryService;
import com.voluble.titanMC.regions.service.RegionEngine;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionEntryProtectionListenerTest extends MockBukkitProtectionTestSupport {

	@TempDir
	Path temporaryDirectory;

	private World world;
	private PlayerMock player;
	private Plugin plugin;
	private RegionEngine engine;
	private RegionAdminService admin;

	@BeforeEach
	void createFixtures() throws Exception {
		world = server.addSimpleWorld("entry_protection");
		player = server.addPlayer();
		plugin = MockBukkit.createMockPlugin();
		engine = RegionEngine.open(temporaryDirectory.resolve("entry-listener.db"));
		admin = new RegionAdminService(engine);
		assertTrue(admin.create(
			"spawn",
			new WorldId(world.getUID()),
			100,
			new CuboidGeometry(new BlockBox(0, 0, 0, 10, 10, 10))
		).successful());
	}

	@AfterEach
	void closeEngine() throws Exception {
		if (engine != null) engine.close();
	}

	@Test
	void cancelsDeniedEntryAndSendsCustomDenyMessage() {
		WorldId worldId = new WorldId(world.getUID());
		assertTrue(admin.setFlag(
			"spawn", worldId, ProtectionAction.ENTRY, ProtectionDecision.DENY
		).successful());
		assertTrue(admin.setText(
			"spawn", worldId, RegionTextFlag.ENTRY_DENY_MESSAGE, "<red>Members only</red>"
		).successful());
		register(ProtectionBypass.none());
		PlayerMoveEvent event = movement(-1, 1);

		server.getPluginManager().callEvent(event);

		assertTrue(event.isCancelled());
		assertEquals("Members only", nextMessage());
	}

	@Test
	void sendsEntryAndExitMessagesOnlyForSuccessfulTransitions() {
		WorldId worldId = new WorldId(world.getUID());
		assertTrue(admin.setText(
			"spawn", worldId, RegionTextFlag.ENTRY_MESSAGE, "<green>Welcome</green>"
		).successful());
		assertTrue(admin.setText(
			"spawn", worldId, RegionTextFlag.EXIT_MESSAGE, "<yellow>Goodbye</yellow>"
		).successful());
		register(ProtectionBypass.none());

		PlayerMoveEvent entering = movement(-1, 1);
		server.getPluginManager().callEvent(entering);
		PlayerMoveEvent inside = movement(1, 2);
		server.getPluginManager().callEvent(inside);
		PlayerMoveEvent exiting = movement(2, 11);
		server.getPluginManager().callEvent(exiting);

		assertFalse(entering.isCancelled());
		assertFalse(inside.isCancelled());
		assertFalse(exiting.isCancelled());
		assertEquals("Welcome", nextMessage());
		assertEquals("Goodbye", nextMessage());
	}

	@Test
	void bukkitPermissionBypassesDeniedEntry() {
		assertTrue(admin.setFlag(
			"spawn", new WorldId(world.getUID()), ProtectionAction.ENTRY, ProtectionDecision.DENY
		).successful());
		player.addAttachment(plugin, "titanmc.protection.bypass", true);
		register(BukkitProtectionBypass.permission(server, "titanmc.protection.bypass"));
		PlayerMoveEvent event = movement(-1, 1);

		server.getPluginManager().callEvent(event);

		assertFalse(event.isCancelled());
	}

	@Test
	void deniesTeleportsIntoProtectedRegions() {
		assertTrue(admin.setFlag(
			"spawn", new WorldId(world.getUID()), ProtectionAction.ENTRY, ProtectionDecision.DENY
		).successful());
		register(ProtectionBypass.none());
		PlayerTeleportEvent event = new PlayerTeleportEvent(
			player,
			new Location(world, -1, 1, 1),
			new Location(world, 1, 1, 1),
			PlayerTeleportEvent.TeleportCause.COMMAND
		);

		server.getPluginManager().callEvent(event);

		assertTrue(event.isCancelled());
	}

	private void register(ProtectionBypass bypass) {
		server.getPluginManager().registerEvents(
			new RegionEntryProtectionListener(new RegionEntryService(engine, bypass)),
			plugin
		);
	}

	private PlayerMoveEvent movement(int fromX, int toX) {
		return new PlayerMoveEvent(
			player,
			new Location(world, fromX, 1, 1),
			new Location(world, toX, 1, 1)
		);
	}

	private String nextMessage() {
		return PlainTextComponentSerializer.plainText().serialize(player.nextComponentMessage());
	}
}
