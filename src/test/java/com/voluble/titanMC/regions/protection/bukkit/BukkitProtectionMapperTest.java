package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitProtectionMapperTest extends MockBukkitProtectionTestSupport {

	@Test
	void snapshotsPlayerIdentityAndGrantedPermissions() {
		Player player = server.addPlayer();
		Plugin plugin = MockBukkit.createMockPlugin();
		player.addAttachment(plugin, "titanmc.protection.bypass", true);
		player.addAttachment(plugin, "titanmc.protection.denied", false);

		var actor = BukkitProtectionMapper.actor(player);

		assertEquals(player.getUniqueId(), actor.playerId());
		assertTrue(actor.hasPermission("TITANMC.PROTECTION.BYPASS"));
		assertFalse(actor.hasPermission("titanmc.protection.denied"));
	}

	@Test
	void mapsBlockAndNegativeFractionalLocationByWorldUuid() {
		World world = server.addSimpleWorld("protection_mapper");
		Block block = world.getBlockAt(4, 70, -8);

		assertEquals(
			new BlockPosition(new WorldId(world.getUID()), 4, 70, -8),
			BukkitProtectionMapper.position(block)
		);
		assertEquals(
			new BlockPosition(new WorldId(world.getUID()), -2, 64, -1),
			BukkitProtectionMapper.position(new Location(world, -1.2, 64.9, -0.1))
		);
	}

	@Test
	void createsTypedProtectionRequest() {
		World world = server.addSimpleWorld("protection_request");
		Player player = server.addPlayer();
		Block block = world.getBlockAt(1, 2, 3);

		ProtectionRequest request = BukkitProtectionMapper.request(player, ProtectionAction.BLOCK_BREAK, block);

		assertEquals(ProtectionAction.BLOCK_BREAK, request.action());
		assertEquals(player.getUniqueId(), request.actor().playerId());
		assertEquals(BukkitProtectionMapper.position(block), request.target());
	}
}
