package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import com.voluble.titanMC.regions.protection.model.TransitionRule;
import com.voluble.titanMC.regions.protection.service.ProtectionEvaluation;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;

import java.time.Instant;
import java.util.Objects;

public final class FireProtectionListener implements Listener {

	private static final ProtectionActor ENVIRONMENT = ProtectionActor.environment("fire");

	private final ProtectionService protection;

	public FireProtectionListener(ProtectionService protection) {
		this.protection = Objects.requireNonNull(protection, "protection");
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockIgnite(BlockIgniteEvent event) {
		Player player = event.getPlayer();
		ProtectionRequest request = player == null
			? BukkitProtectionMapper.request(ENVIRONMENT, ProtectionAction.FIRE_SPREAD, event.getBlock())
			: BukkitProtectionMapper.request(player, ProtectionAction.FIRE_SPREAD, event.getBlock());
		if (!protection.allowed(request)) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockSpread(BlockSpreadEvent event) {
		if (event.getSource().getType() != Material.FIRE
			&& event.getNewState().getType() != Material.FIRE) return;
		if (!allowsTransition(event.getSource(), event.getBlock())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		Block source = event.getIgnitingBlock();
		if (source == null) {
			if (!protection.allowed(BukkitProtectionMapper.request(
				ENVIRONMENT, ProtectionAction.FIRE_SPREAD, event.getBlock()
			))) event.setCancelled(true);
			return;
		}
		if (!allowsTransition(source, event.getBlock())) event.setCancelled(true);
	}

	private boolean allowsTransition(Block source, Block target) {
		ProtectionEvaluation evaluation = protection.beginEvaluation(ENVIRONMENT, Instant.now());
		var movement = BukkitProtectionMapper.movement(
			ENVIRONMENT, ProtectionAction.FIRE_SPREAD, source, target
		);
		return evaluation.resolveTransition(movement, TransitionRule.BOTH).allowed();
	}
}
