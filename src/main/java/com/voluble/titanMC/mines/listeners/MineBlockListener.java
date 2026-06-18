package com.voluble.titanMC.mines.listeners;

import com.voluble.titanMC.TitanMC;
import com.voluble.titanMC.mines.Mine;
import com.voluble.titanMC.mines.MineManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class MineBlockListener implements Listener {

	private final TitanMC plugin;
	private final MineManager manager;

	public MineBlockListener(TitanMC plugin) {
		this.plugin = plugin;
		this.manager = plugin.getMineManager();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		Location loc = block.getLocation();
		if (block.getType() == Material.AIR) return;
		Mine mine = manager.getFirstAt(loc);
		if (mine == null) return;
		mine.incrementBroken(1);
		maybeTriggerDepletionReset(mine);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getBlockReplacedState().getType() != Material.AIR) return;
		Location loc = event.getBlock().getLocation();
		Mine mine = manager.getFirstAt(loc);
		if (mine == null) return;
		mine.decrementBroken(1);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		// Count each destroyed non-air block inside a mine
		for (Block block : event.blockList()) {
			if (block.getType() == Material.AIR) continue;
			Location loc = block.getLocation();
			Mine mine = manager.getFirstAt(loc);
			if (mine != null) {
				mine.incrementBroken(1);
				maybeTriggerDepletionReset(mine);
			}
		}
	}

	private void maybeTriggerDepletionReset(Mine mine) {
		if (!mine.isEnabled()) return;
		if (mine.shouldAutoResetByDepletion()) {
			plugin.getMineScheduler().scheduleDepletionReset(mine.getName());
		}
	}
}


