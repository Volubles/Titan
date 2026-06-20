package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Objects;

public final class BlockProtectionListener implements Listener {

	private final ProtectionService protection;

	public BlockProtectionListener(ProtectionService protection) {
		this.protection = Objects.requireNonNull(protection, "protection");
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!protection.allowed(BukkitProtectionMapper.request(
			event.getPlayer(), ProtectionAction.BLOCK_BREAK, event.getBlock()
		))) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!protection.allowed(BukkitProtectionMapper.request(
			event.getPlayer(), ProtectionAction.BLOCK_PLACE, event.getBlockPlaced()
		))) {
			event.setCancelled(true);
		}
	}
}
