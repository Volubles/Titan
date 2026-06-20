package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import com.voluble.titanMC.regions.protection.model.TransitionRule;
import com.voluble.titanMC.regions.protection.service.ProtectionEvaluation;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class PistonProtectionListener implements Listener {

	private static final ProtectionActor ACTOR = ProtectionActor.environment("piston");

	private final ProtectionService protection;

	public PistonProtectionListener(ProtectionService protection) {
		this.protection = Objects.requireNonNull(protection, "protection");
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		Block piston = event.getBlock();
		event.setCancelled(!allowsMovement(
			piston,
			piston.getRelative(event.getDirection()),
			event.getBlocks(),
			event.getDirection()
		));
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		Block piston = event.getBlock();
		event.setCancelled(!allowsMovement(
			piston.getRelative(event.getDirection()),
			piston,
			event.getBlocks(),
			event.getDirection().getOppositeFace()
		));
	}

	private boolean allowsMovement(
		Block operationSource,
		Block operationTarget,
		List<Block> movedBlocks,
		BlockFace movementDirection
	) {
		ProtectionEvaluation evaluation = protection.beginEvaluation(ACTOR, Instant.now());
		if (!allows(evaluation, operationSource, operationTarget)) return false;
		for (Block source : movedBlocks) {
			if (!allows(evaluation, source, source.getRelative(movementDirection))) return false;
		}
		return true;
	}

	private boolean allows(ProtectionEvaluation evaluation, Block source, Block target) {
		ProtectionRequest movement = BukkitProtectionMapper.movement(
			ACTOR, ProtectionAction.PISTON_MOVE, source, target
		);
		return evaluation.resolveTransition(movement, TransitionRule.BOTH).allowed();
	}
}
