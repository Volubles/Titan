package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.model.TransitionRule;
import com.voluble.titanMC.regions.protection.service.ProtectionEvaluation;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.time.Instant;
import java.util.Objects;

public final class BlockLifecycleProtectionListener implements Listener {

	private static final ProtectionActor GROWTH = ProtectionActor.environment("block-growth");
	private static final ProtectionActor DECAY = ProtectionActor.environment("block-decay");

	private final ProtectionService protection;

	public BlockLifecycleProtectionListener(ProtectionService protection) {
		this.protection = Objects.requireNonNull(protection, "protection");
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockGrow(BlockGrowEvent event) {
		if (!protection.allowed(BukkitProtectionMapper.request(
			GROWTH, ProtectionAction.BLOCK_GROWTH, event.getBlock()
		))) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockSpread(BlockSpreadEvent event) {
		if (event.getSource().getType() == Material.FIRE
			|| event.getNewState().getType() == Material.FIRE) return;
		if (!allowsGrowth(event.getSource(), event.getBlock())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onStructureGrow(StructureGrowEvent event) {
		Player player = event.getPlayer();
		for (BlockState state : event.getBlocks()) {
			boolean allowed = player == null
				? protection.allowed(BukkitProtectionMapper.request(
					GROWTH, ProtectionAction.BLOCK_GROWTH, state.getBlock()
				))
				: protection.allowed(BukkitProtectionMapper.request(
					player, ProtectionAction.BLOCK_GROWTH, state.getBlock()
				));
			if (!allowed) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onLeavesDecay(LeavesDecayEvent event) {
		if (!allowsDecay(event.getBlock())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockFade(BlockFadeEvent event) {
		if (!allowsDecay(event.getBlock())) event.setCancelled(true);
	}

	private boolean allowsGrowth(Block source, Block target) {
		ProtectionEvaluation evaluation = protection.beginEvaluation(GROWTH, Instant.now());
		var movement = BukkitProtectionMapper.movement(
			GROWTH, ProtectionAction.BLOCK_GROWTH, source, target
		);
		return evaluation.resolveTransition(movement, TransitionRule.BOTH).allowed();
	}

	private boolean allowsDecay(Block block) {
		return protection.allowed(BukkitProtectionMapper.request(
			DECAY, ProtectionAction.BLOCK_DECAY, block
		));
	}
}
