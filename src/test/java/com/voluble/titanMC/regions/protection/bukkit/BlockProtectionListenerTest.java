package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.policy.ProtectionBypass;
import com.voluble.titanMC.regions.protection.policy.RegionPolicyRegistry;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockProtectionListenerTest extends MockBukkitProtectionTestSupport {

	private World world;
	private Player player;
	private Plugin plugin;

	@BeforeEach
	void createFixtures() {
		world = server.addSimpleWorld("block_protection");
		player = server.addPlayer();
		plugin = MockBukkit.createMockPlugin();
	}

	@Test
	void cancelsBreakAndPlaceWhenProtectionDenies() {
		register(request -> ProtectionDecision.DENY, ProtectionBypass.none());

		BlockBreakEvent breakEvent = breakEvent(world.getBlockAt(1, 64, 1));
		BlockPlaceEvent placeEvent = placeEvent(world.getBlockAt(2, 64, 2));
		server.getPluginManager().callEvent(breakEvent);
		server.getPluginManager().callEvent(placeEvent);

		assertTrue(breakEvent.isCancelled());
		assertTrue(placeEvent.isCancelled());
	}

	@Test
	void mapsBreakAndPlaceToIndependentActions() {
		register(
			request -> request.action() == ProtectionAction.BLOCK_BREAK
				? ProtectionDecision.ALLOW
				: ProtectionDecision.DENY,
			ProtectionBypass.none()
		);

		BlockBreakEvent breakEvent = breakEvent(world.getBlockAt(1, 64, 1));
		BlockPlaceEvent placeEvent = placeEvent(world.getBlockAt(2, 64, 2));
		server.getPluginManager().callEvent(breakEvent);
		server.getPluginManager().callEvent(placeEvent);

		assertFalse(breakEvent.isCancelled());
		assertTrue(placeEvent.isCancelled());
	}

	@Test
	void bypassPermissionAllowsDeniedAction() {
		player.addAttachment(plugin, "titanmc.protection.bypass", true);
		register(
			request -> ProtectionDecision.DENY,
			ProtectionBypass.permission("titanmc.protection.bypass")
		);

		BlockBreakEvent event = breakEvent(world.getBlockAt(1, 64, 1));
		server.getPluginManager().callEvent(event);

		assertFalse(event.isCancelled());
	}

	private void register(
		com.voluble.titanMC.regions.protection.policy.ProtectionDefaults defaults,
		ProtectionBypass bypass
	) {
		ProtectionService service = new ProtectionService(
			(worldId, x, y, z) -> List.of(),
			RegionPolicyRegistry.builder().build(),
			defaults,
			bypass
		);
		server.getPluginManager().registerEvents(new BlockProtectionListener(service), plugin);
	}

	private BlockBreakEvent breakEvent(Block block) {
		block.setType(Material.STONE);
		return new BlockBreakEvent(block, player);
	}

	private BlockPlaceEvent placeEvent(Block block) {
		block.setType(Material.STONE);
		return new BlockPlaceEvent(
			block,
			block.getState(),
			world.getBlockAt(block.getX(), block.getY() - 1, block.getZ()),
			new ItemStack(Material.STONE),
			player,
			true,
			EquipmentSlot.HAND
		);
	}
}
