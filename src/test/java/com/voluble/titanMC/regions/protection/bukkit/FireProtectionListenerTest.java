package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.policy.ProtectionBypass;
import com.voluble.titanMC.regions.protection.policy.RegionPolicyRegistry;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireProtectionListenerTest extends MockBukkitProtectionTestSupport {

	private World world;
	private Player player;
	private FireProtectionListener listener;

	@BeforeEach
	void createFixtures() {
		world = server.addSimpleWorld("fire_protection");
		player = server.addPlayer();
		Plugin plugin = MockBukkit.createMockPlugin();
		player.addAttachment(plugin, "titanmc.protection.bypass", true);
		ProtectionService service = new ProtectionService(
			(worldId, x, y, z) -> List.of(),
			RegionPolicyRegistry.builder().build(),
			request -> ProtectionDecision.DENY,
			ProtectionBypass.permission("titanmc.protection.bypass")
		);
		listener = new FireProtectionListener(service);
	}

	@Test
	void allowsBypassedPlayersToIgniteBlocks() {
		BlockIgniteEvent event = new BlockIgniteEvent(
			world.getBlockAt(1, 64, 1), BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, player
		);

		listener.onBlockIgnite(event);

		assertFalse(event.isCancelled());
	}

	@Test
	void deniesEnvironmentalFireSpread() {
		Block source = world.getBlockAt(3, 64, 3);
		source.setType(Material.FIRE);
		Block target = world.getBlockAt(4, 64, 3);
		target.setType(Material.AIR);
		Block stateBlock = world.getBlockAt(5, 64, 3);
		stateBlock.setType(Material.FIRE);
		BlockSpreadEvent event = new BlockSpreadEvent(target, source, stateBlock.getState());

		listener.onBlockSpread(event);

		assertTrue(event.isCancelled());
	}

	@Test
	void deniesBlocksBurning() {
		Block fire = world.getBlockAt(7, 64, 7);
		fire.setType(Material.FIRE);
		BlockBurnEvent event = new BlockBurnEvent(world.getBlockAt(8, 64, 7), fire);

		listener.onBlockBurn(event);

		assertTrue(event.isCancelled());
	}

	@Test
	void ignoresNonFireBlockSpread() {
		Block source = world.getBlockAt(10, 64, 10);
		source.setType(Material.BROWN_MUSHROOM);
		Block target = world.getBlockAt(11, 64, 10);
		BlockSpreadEvent event = new BlockSpreadEvent(target, source, source.getState());

		listener.onBlockSpread(event);

		assertFalse(event.isCancelled());
	}
}
