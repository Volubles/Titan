package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BukkitProtectionConfigurationTest extends MockBukkitProtectionTestSupport {

	@Test
	void protectsOnlyExplicitlyConfiguredWorlds() {
		World protectedWorld = server.addSimpleWorld("cells");
		World openWorld = server.addSimpleWorld("lobby");
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("protection.protected-worlds", List.of("cells"));

		BukkitProtectionConfiguration configuration = BukkitProtectionConfiguration.load(yaml, server);

		assertEquals(ProtectionDecision.DENY, configuration.defaults().decide(request(protectedWorld)));
		assertEquals(ProtectionDecision.ALLOW, configuration.defaults().decide(request(openWorld)));
	}

	@Test
	void emptyWorldListLeavesEveryWorldOpen() {
		World world = server.addSimpleWorld("open");
		BukkitProtectionConfiguration configuration = BukkitProtectionConfiguration.load(new YamlConfiguration(), server);

		assertEquals(ProtectionDecision.ALLOW, configuration.defaults().decide(request(world)));
	}

	@Test
	void rejectsConfiguredWorldThatIsNotLoaded() {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("protection.protected-worlds", List.of("typo_world"));

		assertThrows(IllegalArgumentException.class, () -> BukkitProtectionConfiguration.load(yaml, server));
	}

	private static ProtectionRequest request(World world) {
		return ProtectionRequest.at(
			ProtectionActor.system("configuration-test", Set.of()),
			ProtectionAction.BLOCK_BREAK,
			new BlockPosition(new WorldId(world.getUID()), 0, 64, 0)
		);
	}
}
