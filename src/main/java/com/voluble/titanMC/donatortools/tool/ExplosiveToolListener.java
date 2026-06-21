package com.voluble.titanMC.donatortools.tool;

import com.voluble.titanMC.donatortools.config.DonatorToolsSettings;
import com.voluble.titanMC.donatortools.item.DonatorToolRegistry;
import com.voluble.titanMC.donatortools.item.DonatorToolType;
import com.voluble.titanMC.donatortools.protection.BlockBreakPermission;
import com.voluble.titanMC.donatortools.tracking.BlockBreakTracker;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ExplosiveToolListener implements Listener {

	private final DonatorToolRegistry tools;
	private final DonatorToolsSettings configuration;
	private final ExplosiveBreakPlanner planner;
	private final BlockBreakTracker mineBreaks;
	private final Map<UUID, PendingExplosiveBreak> pending = new HashMap<>();

	public ExplosiveToolListener(
		DonatorToolRegistry tools,
		DonatorToolsSettings configuration,
		BlockBreakPermission protection,
		BlockBreakTracker mineBreaks
	) {
		this.tools = Objects.requireNonNull(tools, "tools");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.planner = new ExplosiveBreakPlanner(
			configuration,
			Objects.requireNonNull(protection, "protection")
		);
		this.mineBreaks = Objects.requireNonNull(mineBreaks, "mineBreaks");
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void rememberTool(BlockBreakEvent event) {
		Player player = event.getPlayer();
		pending.remove(player.getUniqueId());
		if (player.getGameMode() == GameMode.CREATIVE) return;
		ItemStack tool = player.getInventory().getItemInMainHand();
		if (tools.identify(tool).orElse(null) != DonatorToolType.EXPLOSIVE) return;
		if (!configuration.current().allows(event.getBlock().getType())) return;
		pending.put(player.getUniqueId(), new PendingExplosiveBreak(
			event.getBlock().getWorld().getUID(),
			event.getBlock().getX(),
			event.getBlock().getY(),
			event.getBlock().getZ(),
			tool.clone()
		));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		PendingExplosiveBreak prepared = pending.remove(player.getUniqueId());
		if (prepared == null || !prepared.matches(event.getBlock())) return;
		ItemStack tool = prepared.tool();
		Block center = event.getBlock();

		center.getWorld().playSound(
			center.getLocation().add(0.5, 0.5, 0.5),
			Sound.ENTITY_GENERIC_EXPLODE,
			0.5f,
			1.2f
		);
		for (Block block : planner.additionalBlocks(player, center)) {
			breakAdditional(block, tool);
		}
	}

	@EventHandler
	public void forgetPlayer(PlayerQuitEvent event) {
		pending.remove(event.getPlayer().getUniqueId());
	}

	private void breakAdditional(Block block, ItemStack tool) {
		var location = block.getLocation();
		if (!block.breakNaturally(tool, false)) return;
		block.getWorld().spawnParticle(
			Particle.EXPLOSION,
			location.add(0.5, 0.5, 0.5),
			1,
			0.1,
			0.1,
			0.1,
			0.01
		);
		mineBreaks.record(location);
	}

	private record PendingExplosiveBreak(UUID worldId, int x, int y, int z, ItemStack tool) {
		private boolean matches(Block block) {
			return block.getWorld().getUID().equals(worldId)
				&& block.getX() == x
				&& block.getY() == y
				&& block.getZ() == z;
		}
	}
}
