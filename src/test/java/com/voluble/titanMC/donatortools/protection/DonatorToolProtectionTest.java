package com.voluble.titanMC.donatortools.protection;

import com.voluble.titanMC.donatortools.MockBukkitDonatorToolsTestSupport;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.policy.ProtectionBypass;
import com.voluble.titanMC.regions.protection.policy.RegionPolicyRegistry;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonatorToolProtectionTest extends MockBukkitDonatorToolsTestSupport {

	@Test
	void usesTheSameRegionDecisionAndAdminBypassAsOrdinaryBlockBreaking() {
		var world = server.addSimpleWorld("tool_protection");
		var player = server.addPlayer();
		var plugin = MockBukkit.createMockPlugin();
		ProtectionService protection = new ProtectionService(
			(worldId, x, y, z) -> List.of(),
			RegionPolicyRegistry.builder().build(),
			request -> request.target().x() <= 0
				? ProtectionDecision.ALLOW
				: ProtectionDecision.DENY,
			ProtectionBypass.permission("titanmc.protection.bypass")
		);
		DonatorToolProtection tools = new DonatorToolProtection(protection, (actor, block) -> true);
		var allowed = world.getBlockAt(0, 64, 0);
		var denied = world.getBlockAt(1, 64, 0);
		allowed.setType(Material.STONE);
		denied.setType(Material.STONE);

		assertTrue(tools.canBreak(player, allowed));
		assertFalse(tools.canBreak(player, denied));

		player.addAttachment(plugin, "titanmc.protection.bypass", true);
		assertTrue(tools.canBreak(player, denied));
	}
}
