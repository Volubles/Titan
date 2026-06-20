package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import com.voluble.titanMC.regions.protection.policy.ProtectionBypass;
import com.voluble.titanMC.regions.protection.policy.RegionPolicyRegistry;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BucketProtectionListenerTest extends MockBukkitProtectionTestSupport {

	private World world;
	private Player player;
	private BucketProtectionListener listener;
	private List<ProtectionRequest> requests;

	@BeforeEach
	void createFixtures() {
		world = server.addSimpleWorld("bucket_protection");
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
		listener = new BucketProtectionListener(service);
	}

	@Test
	void protectsTheAffectedBlockWhenFillingBuckets() {
		Block affected = world.getBlockAt(4, 64, 4);
		Block clicked = world.getBlockAt(4, 64, 5);
		PlayerBucketFillEvent event = new PlayerBucketFillEvent(
			player,
			affected,
			clicked,
			BlockFace.NORTH,
			Material.BUCKET,
			new ItemStack(Material.WATER_BUCKET),
			EquipmentSlot.HAND
		);

		listener.onBucketFill(event);

		assertTrue(event.isCancelled());
		assertRequest(ProtectionAction.BUCKET_FILL, affected);
	}

	@Test
	void protectsTheAffectedBlockWhenEmptyingBuckets() {
		Block affected = world.getBlockAt(8, 64, 8);
		Block clicked = world.getBlockAt(8, 63, 8);
		PlayerBucketEmptyEvent event = new PlayerBucketEmptyEvent(
			player,
			affected,
			clicked,
			BlockFace.UP,
			Material.WATER_BUCKET,
			new ItemStack(Material.BUCKET),
			EquipmentSlot.HAND
		);

		listener.onBucketEmpty(event);

		assertTrue(event.isCancelled());
		assertRequest(ProtectionAction.BUCKET_EMPTY, affected);
	}

	@Test
	void remembersFluidSourcesPlacedThroughBypass() {
		player.addAttachment(MockBukkit.createMockPlugin(), "titanmc.protection.bypass", true);
		TrustedFluidFlow trusted = new TrustedFluidFlow();
		ProtectionService bypassingService = new ProtectionService(
			(worldId, x, y, z) -> List.of(),
			RegionPolicyRegistry.builder().build(),
			request -> ProtectionDecision.DENY,
			ProtectionBypass.permission("titanmc.protection.bypass")
		);
		BucketProtectionListener bypassingListener = new BucketProtectionListener(bypassingService, trusted);
		Block affected = world.getBlockAt(12, 64, 12);
		PlayerBucketEmptyEvent event = new PlayerBucketEmptyEvent(
			player,
			affected,
			world.getBlockAt(12, 63, 12),
			BlockFace.UP,
			Material.WATER_BUCKET,
			new ItemStack(Material.BUCKET),
			EquipmentSlot.HAND
		);

		bypassingListener.onBucketEmpty(event);
		bypassingListener.rememberBypassedFluidSource(event);

		assertTrue(trusted.contains(affected));
	}

	private void assertRequest(ProtectionAction action, Block target) {
		assertEquals(1, requests.size());
		ProtectionRequest request = requests.getFirst();
		assertEquals(action, request.action());
		assertEquals(target.getX(), request.target().x());
		assertEquals(target.getY(), request.target().y());
		assertEquals(target.getZ(), request.target().z());
	}
}
