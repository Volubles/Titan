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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HangingProtectionListenerTest extends MockBukkitProtectionTestSupport {

	private Player player;
	private Hanging hanging;
	private HangingProtectionListener listener;
	private List<ProtectionRequest> requests;

	@BeforeEach
	void createFixtures() {
		World world = server.addSimpleWorld("hanging_protection");
		player = server.addPlayer();
		hanging = (Hanging) world.spawnEntity(new Location(world, 6, 65, 8), EntityType.ITEM_FRAME);
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
		listener = new HangingProtectionListener(service);
	}

	@Test
	void deniesHangingPlacement() {
		HangingPlaceEvent event = new HangingPlaceEvent(
			hanging, player, hanging.getLocation().getBlock(), BlockFace.NORTH, EquipmentSlot.HAND
		);

		listener.onHangingPlace(event);

		assertTrue(event.isCancelled());
		assertRequest();
	}

	@Test
	void deniesHangingInteraction() {
		PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, hanging);

		listener.onHangingInteract(event);

		assertTrue(event.isCancelled());
		assertRequest();
	}

	@Test
	void deniesHangingBreaks() {
		HangingBreakByEntityEvent event = new HangingBreakByEntityEvent(hanging, player);

		listener.onHangingBreak(event);

		assertTrue(event.isCancelled());
		assertRequest();
	}

	private void assertRequest() {
		assertEquals(1, requests.size());
		assertEquals(ProtectionAction.HANGING_MODIFY, requests.getFirst().action());
	}
}
