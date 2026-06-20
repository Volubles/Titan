package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import com.voluble.titanMC.regions.protection.policy.ProtectionBypass;
import com.voluble.titanMC.regions.protection.policy.RegionPolicyRegistry;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityInteractionProtectionListenerTest extends MockBukkitProtectionTestSupport {

	private Player player;
	private Entity target;
	private EntityInteractionProtectionListener listener;
	private List<ProtectionRequest> requests;

	@BeforeEach
	void createFixtures() {
		World world = server.addSimpleWorld("entity_interaction_protection");
		player = server.addPlayer();
		target = world.spawnEntity(new Location(world, 3.5, 64, 5.5), org.bukkit.entity.EntityType.ARMOR_STAND);
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
		listener = new EntityInteractionProtectionListener(service);
	}

	@Test
	void deniesEntityPlacement() {
		EntityPlaceEvent event = new EntityPlaceEvent(target, player, target.getLocation().getBlock(), BlockFace.UP);

		listener.onEntityPlace(event);

		assertTrue(event.isCancelled());
		assertRequest(ProtectionAction.ENTITY_PLACE);
	}

	@Test
	void deniesEntityInteraction() {
		PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, target);

		listener.onEntityInteract(event);

		assertTrue(event.isCancelled());
		assertRequest(ProtectionAction.ENTITY_INTERACT);
	}

	@Test
	void deniesPlayerDamageToEntities() {
		EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
			player, target, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1.0
		);

		listener.onEntityDamage(event);

		assertTrue(event.isCancelled());
		assertRequest(ProtectionAction.ENTITY_DAMAGE);
	}

	@Test
	void mapsPlayerDamageToPvpFlag() {
		Player victim = server.addPlayer();
		victim.teleport(target.getLocation());
		EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
			player, victim, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1.0
		);

		listener.onEntityDamage(event);

		assertTrue(event.isCancelled());
		assertRequest(ProtectionAction.PLAYER_PVP);
	}

	private void assertRequest(ProtectionAction action) {
		assertEquals(1, requests.size());
		assertEquals(action, requests.getFirst().action());
		assertEquals(3, requests.getFirst().target().x());
		assertEquals(64, requests.getFirst().target().y());
		assertEquals(5, requests.getFirst().target().z());
	}
}
