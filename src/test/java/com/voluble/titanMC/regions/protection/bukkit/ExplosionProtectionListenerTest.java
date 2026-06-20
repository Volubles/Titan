package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import com.voluble.titanMC.regions.protection.policy.ProtectionBypass;
import com.voluble.titanMC.regions.protection.policy.RegionPolicyRegistry;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExplosionProtectionListenerTest extends MockBukkitProtectionTestSupport {

	private World world;
	private Player player;
	private ExplosionProtectionListener listener;
	private List<ProtectionRequest> requests;

	@BeforeEach
	void createFixtures() {
		world = server.addSimpleWorld("explosion_protection");
		player = server.addPlayer();
		requests = new ArrayList<>();
		ProtectionService service = new ProtectionService(
			(worldId, x, y, z) -> List.of(),
			RegionPolicyRegistry.builder().build(),
			request -> {
				requests.add(request);
				return request.target().x() == 2 ? ProtectionDecision.ALLOW : ProtectionDecision.DENY;
			},
			ProtectionBypass.none()
		);
		listener = new ExplosionProtectionListener(service);
	}

	@Test
	void filtersProtectedBlocksFromEntityExplosions() {
		List<Block> affected = affectedBlocks();
		EntityExplodeEvent event = new EntityExplodeEvent(
			player, new Location(world, 0, 64, 0), affected, 1.0F, ExplosionResult.DESTROY
		);

		listener.onEntityExplode(event);

		assertEquals(List.of(world.getBlockAt(2, 64, 0)), event.blockList());
		assertRequests();
	}

	@Test
	void filtersProtectedBlocksFromBlockExplosions() {
		Block source = world.getBlockAt(0, 64, 0);
		source.setType(Material.TNT);
		BlockExplodeEvent event = new BlockExplodeEvent(
			source, source.getState(), affectedBlocks(), 1.0F, ExplosionResult.DESTROY
		);

		listener.onBlockExplode(event);

		assertEquals(List.of(world.getBlockAt(2, 64, 0)), event.blockList());
		assertRequests();
	}

	private List<Block> affectedBlocks() {
		return new ArrayList<>(List.of(
			world.getBlockAt(1, 64, 0),
			world.getBlockAt(2, 64, 0),
			world.getBlockAt(3, 64, 0)
		));
	}

	private void assertRequests() {
		assertEquals(3, requests.size());
		for (ProtectionRequest request : requests) {
			assertEquals(ProtectionAction.EXPLOSION_BLOCK_DAMAGE, request.action());
		}
	}
}
