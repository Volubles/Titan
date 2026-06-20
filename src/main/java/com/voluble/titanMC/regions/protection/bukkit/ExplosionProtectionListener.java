package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.service.ProtectionEvaluation;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class ExplosionProtectionListener implements Listener {

	private static final ProtectionActor ACTOR = ProtectionActor.environment("explosion");

	private final ProtectionService protection;

	public ExplosionProtectionListener(ProtectionService protection) {
		this.protection = Objects.requireNonNull(protection, "protection");
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		filterProtectedBlocks(event.blockList());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event) {
		filterProtectedBlocks(event.blockList());
	}

	private void filterProtectedBlocks(List<Block> blocks) {
		ProtectionEvaluation evaluation = protection.beginEvaluation(ACTOR, Instant.now());
		blocks.removeIf(block -> !evaluation.resolve(BukkitProtectionMapper.request(
			ACTOR, ProtectionAction.EXPLOSION_BLOCK_DAMAGE, block
		)).allowed());
	}
}
