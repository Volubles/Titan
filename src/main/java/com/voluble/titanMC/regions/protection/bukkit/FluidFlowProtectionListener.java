package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.model.TransitionRule;
import com.voluble.titanMC.regions.protection.service.ProtectionEvaluation;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

import java.time.Instant;
import java.util.Objects;

public final class FluidFlowProtectionListener implements Listener {

	private static final ProtectionActor ACTOR = ProtectionActor.environment("fluid-flow");

	private final ProtectionService protection;
	private final TrustedFluidFlow trusted;

	public FluidFlowProtectionListener(ProtectionService protection, TrustedFluidFlow trusted) {
		this.protection = Objects.requireNonNull(protection, "protection");
		this.trusted = Objects.requireNonNull(trusted, "trusted");
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onFluidFlow(BlockFromToEvent event) {
		if (!isFluid(event.getBlock().getType()) || trusted.contains(event.getBlock())) return;
		ProtectionEvaluation evaluation = protection.beginEvaluation(ACTOR, Instant.now());
		var movement = BukkitProtectionMapper.movement(
			ACTOR, ProtectionAction.FLUID_FLOW, event.getBlock(), event.getToBlock()
		);
		if (!evaluation.resolveTransition(movement, TransitionRule.BOTH).allowed()) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void rememberTrustedFlow(BlockFromToEvent event) {
		if (isFluid(event.getBlock().getType()) && trusted.contains(event.getBlock())) {
			trusted.add(event.getToBlock());
		}
	}

	private static boolean isFluid(Material material) {
		return material == Material.WATER || material == Material.LAVA;
	}
}
