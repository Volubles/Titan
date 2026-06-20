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
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PistonProtectionListenerTest extends MockBukkitProtectionTestSupport {

	private World world;
	private Block piston;
	private PistonProtectionListener listener;
	private List<ProtectionRequest> requests;
	private int deniedX;

	@BeforeEach
	void createFixtures() {
		world = server.addSimpleWorld("piston_protection");
		piston = world.getBlockAt(0, 64, 0);
		piston.setType(Material.STICKY_PISTON);
		requests = new ArrayList<>();
		deniedX = Integer.MIN_VALUE;
		ProtectionService service = new ProtectionService(
			(worldId, x, y, z) -> List.of(),
			RegionPolicyRegistry.builder().build(),
			request -> {
				requests.add(request);
				return request.target().x() == deniedX ? ProtectionDecision.DENY : ProtectionDecision.ALLOW;
			},
			ProtectionBypass.none()
		);
		listener = new PistonProtectionListener(service);
	}

	@Test
	void allowsAnExtensionWhenThePistonAndMovedBlocksAreAllowed() {
		Block moved = world.getBlockAt(1, 64, 0);
		BlockPistonExtendEvent event = new BlockPistonExtendEvent(piston, List.of(moved), BlockFace.EAST);

		listener.onPistonExtend(event);

		assertFalse(event.isCancelled());
		assertPositions(0, 1, 1, 2);
	}

	@Test
	void deniesAnExtensionWhenAMovedBlockEntersProtectedSpace() {
		deniedX = 2;
		Block moved = world.getBlockAt(1, 64, 0);
		BlockPistonExtendEvent event = new BlockPistonExtendEvent(piston, List.of(moved), BlockFace.EAST);

		listener.onPistonExtend(event);

		assertTrue(event.isCancelled());
	}

	@Test
	void deniesAnEmptyExtensionWhenThePistonHeadEntersProtectedSpace() {
		deniedX = 1;
		BlockPistonExtendEvent event = new BlockPistonExtendEvent(piston, List.of(), BlockFace.EAST);

		listener.onPistonExtend(event);

		assertTrue(event.isCancelled());
		assertPositions(0, 1);
	}

	@Test
	void retractsMovedBlocksTowardThePiston() {
		Block pulled = world.getBlockAt(2, 64, 0);
		BlockPistonRetractEvent event = new BlockPistonRetractEvent(piston, List.of(pulled), BlockFace.EAST);

		listener.onPistonRetract(event);

		assertFalse(event.isCancelled());
		assertPositions(1, 0, 2, 1);
	}

	private void assertPositions(int... xCoordinates) {
		assertEquals(xCoordinates.length, requests.size());
		for (int index = 0; index < xCoordinates.length; index++) {
			ProtectionRequest request = requests.get(index);
			assertEquals(ProtectionAction.PISTON_MOVE, request.action());
			assertEquals(xCoordinates[index], request.target().x());
		}
	}
}
