package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import com.voluble.titanMC.regions.protection.policy.RegionPolicyRegistry;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitProtectionBypassTest extends MockBukkitProtectionTestSupport {

	private Player player;
	private Plugin plugin;

	@BeforeEach
	void createFixtures() {
		player = server.addPlayer();
		plugin = MockBukkit.createMockPlugin();
	}

	@Test
	void usesBukkitPermissionResolutionInsteadOfCopiedPermissionSet() {
		player.addAttachment(plugin, "titanmc.protection.bypass", true);
		ProtectionService protection = new ProtectionService(
			(worldId, x, y, z) -> List.of(),
			RegionPolicyRegistry.builder().build(),
			request -> ProtectionDecision.DENY,
			BukkitProtectionBypass.permission(server, "titanmc.protection.bypass")
		);
		ProtectionRequest request = ProtectionRequest.at(
			com.voluble.titanMC.regions.protection.model.ProtectionActor.player(
				player.getUniqueId(), java.util.Set.of()
			),
			ProtectionAction.BLOCK_INTERACT,
			new BlockPosition(new WorldId(player.getWorld().getUID()), 0, 64, 0)
		);

		assertTrue(protection.allowed(request));
	}

	@Test
	void bypassAppliesToEveryProtectionAction() {
		player.addAttachment(plugin, "titanmc.protection.bypass", true);
		ProtectionService protection = new ProtectionService(
			(worldId, x, y, z) -> List.of(),
			RegionPolicyRegistry.builder().build(),
			request -> ProtectionDecision.DENY,
			BukkitProtectionBypass.permission(server, "titanmc.protection.bypass")
		);
		var actor = com.voluble.titanMC.regions.protection.model.ProtectionActor.player(
			player.getUniqueId(), java.util.Set.of()
		);
		var position = new BlockPosition(new WorldId(player.getWorld().getUID()), 0, 64, 0);

		for (ProtectionAction action : ProtectionAction.values()) {
			assertTrue(
				protection.allowed(ProtectionRequest.at(actor, action, position)),
				() -> "bypass did not apply to " + action
			);
		}
	}
}
