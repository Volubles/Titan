package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import com.voluble.titanMC.regions.protection.policy.ProtectionBypass;
import com.voluble.titanMC.regions.protection.policy.RegionPolicyRegistry;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityContainerProtectionListenerTest extends MockBukkitProtectionTestSupport {

	private World world;
	private Player player;
	private EntityContainerProtectionListener listener;
	private List<ProtectionRequest> requests;

	@BeforeEach
	void createFixtures() {
		world = server.addSimpleWorld("entity_container_protection");
		player = server.addPlayer();
		requests = new ArrayList<>();
		ProtectionService service = new ProtectionService(
			(worldId, x, y, z) -> List.of(),
			RegionPolicyRegistry.builder().build(),
			request -> {
				requests.add(request);
				return ProtectionDecision.DENY;
			},
			ProtectionBypass.none()
		);
		listener = new EntityContainerProtectionListener(service);
	}

	@Test
	void protectsChestMinecartInventoriesAtTheEntityPosition() {
		assertProtectedInventory(EntityType.CHEST_MINECART, new Location(world, 7.5, 64, 9.5));
	}

	@Test
	void protectsHopperMinecartInventoriesAtTheEntityPosition() {
		assertProtectedInventory(EntityType.HOPPER_MINECART, new Location(world, 11.5, 64, 13.5));
	}

	private void assertProtectedInventory(EntityType type, Location location) {
		Entity entity = world.spawnEntity(location, type);
		InventoryHolder holder = (InventoryHolder) entity;
		InventoryOpenEvent event = new InventoryOpenEvent(player.openInventory(holder.getInventory()));

		listener.onInventoryOpen(event);

		assertTrue(event.isCancelled());
		assertEquals(1, requests.size());
		ProtectionRequest request = requests.getFirst();
		assertEquals(ProtectionAction.CONTAINER_OPEN, request.action());
		assertEquals(location.getBlockX(), request.target().x());
		assertEquals(location.getBlockY(), request.target().y());
		assertEquals(location.getBlockZ(), request.target().z());
	}
}
