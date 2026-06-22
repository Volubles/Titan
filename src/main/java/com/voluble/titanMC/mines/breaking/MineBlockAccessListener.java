package com.voluble.titanMC.mines.breaking;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Objects;

public final class MineBlockAccessListener implements Listener {
	private final MineBlockAccess access;

	public MineBlockAccessListener(MineBlockAccess access) {
		this.access = Objects.requireNonNull(access, "access");
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (access.evaluate(event.getPlayer(), event.getBlock()) != MineBreakDecision.MATERIAL_DENIED) return;
		event.setCancelled(true);
	}
}
