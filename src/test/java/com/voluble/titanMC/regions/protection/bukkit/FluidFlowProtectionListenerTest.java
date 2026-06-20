package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.policy.ProtectionBypass;
import com.voluble.titanMC.regions.protection.policy.RegionPolicyRegistry;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockFromToEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidFlowProtectionListenerTest extends MockBukkitProtectionTestSupport {

	private World world;
	private TrustedFluidFlow trusted;
	private FluidFlowProtectionListener listener;

	@BeforeEach
	void createFixtures() {
		world = server.addSimpleWorld("fluid_flow_protection");
		trusted = new TrustedFluidFlow();
		ProtectionService service = new ProtectionService(
			(worldId, x, y, z) -> List.of(),
			RegionPolicyRegistry.builder().build(),
			request -> request.action() == ProtectionAction.FLUID_FLOW
				? ProtectionDecision.DENY
				: ProtectionDecision.ALLOW,
			ProtectionBypass.none()
		);
		listener = new FluidFlowProtectionListener(service, trusted);
	}

	@Test
	void deniesUntrustedFluidFlow() {
		Block source = waterAt(1, 64, 1);
		BlockFromToEvent event = new BlockFromToEvent(source, world.getBlockAt(2, 64, 1));

		listener.onFluidFlow(event);

		assertTrue(event.isCancelled());
	}

	@Test
	void propagatesTrustedAdminFluid() {
		Block source = waterAt(4, 64, 4);
		Block target = world.getBlockAt(5, 64, 4);
		trusted.add(source);
		BlockFromToEvent event = new BlockFromToEvent(source, target);

		listener.onFluidFlow(event);
		listener.rememberTrustedFlow(event);

		assertFalse(event.isCancelled());
		assertTrue(trusted.contains(target));
	}

	@Test
	void ignoresNonFluidFromToEvents() {
		Block source = world.getBlockAt(7, 64, 7);
		source.setType(Material.DRAGON_EGG);
		BlockFromToEvent event = new BlockFromToEvent(source, world.getBlockAt(8, 64, 7));

		listener.onFluidFlow(event);

		assertFalse(event.isCancelled());
	}

	private Block waterAt(int x, int y, int z) {
		Block block = world.getBlockAt(x, y, z);
		block.setType(Material.WATER);
		return block;
	}
}
