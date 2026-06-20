package com.voluble.titanMC.regions.protection.bukkit;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class MockBukkitProtectionEnvironmentTest extends MockBukkitProtectionTestSupport {

	@Test
	void createsWorldPlayerAndDispatchesBlockEvent() {
		World world = server.addSimpleWorld("protection_test");
		Player player = server.addPlayer();
		Block block = world.getBlockAt(4, 64, 8);
		block.setType(Material.STONE);
		BlockBreakEvent event = new BlockBreakEvent(block, player);

		server.getPluginManager().callEvent(event);

		assertSame(block, event.getBlock());
		assertSame(player, event.getPlayer());
		assertFalse(event.isCancelled());
	}
}
