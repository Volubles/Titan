package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedBlockAccessRegistryTest {
	private ServerMock server;
	private Logger logger;

	@BeforeEach
	void setUp() {
		server = MockBukkit.mock();
		logger = Logger.getAnonymousLogger();
		logger.setLevel(Level.OFF);
	}

	@AfterEach
	void tearDown() {
		MockBukkit.unmock();
	}

	@Test
	void anyRegisteredOwnerMayAllowInteraction() {
		var player = server.addPlayer();
		var block = server.addSimpleWorld("managed").getBlockAt(1, 64, 1);
		block.setType(Material.CHEST);
		ManagedBlockAccessRegistry registry = new ManagedBlockAccessRegistry(logger);
		registry.register((actor, action, target) -> false);
		registry.register((actor, action, target) -> action == ProtectionAction.CONTAINER_OPEN);

		assertTrue(registry.allows(player, ProtectionAction.CONTAINER_OPEN, block));
	}

	@Test
	void failingOwnerFallsBackToOtherOwnersAndDefaults() {
		var player = server.addPlayer();
		var block = server.addSimpleWorld("managed_failure").getBlockAt(1, 64, 1);
		ManagedBlockAccessRegistry registry = new ManagedBlockAccessRegistry(logger);
		AtomicInteger evaluated = new AtomicInteger();
		registry.register((actor, action, target) -> { throw new IllegalStateException("failed"); });
		registry.register((actor, action, target) -> {
			evaluated.incrementAndGet();
			return false;
		});

		assertFalse(registry.allows(player, ProtectionAction.BLOCK_INTERACT, block));
		assertEquals(1, evaluated.get());
	}
}
