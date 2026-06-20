package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import java.util.Objects;

public final class BucketProtectionListener implements Listener {

	private final ProtectionService protection;

	public BucketProtectionListener(ProtectionService protection) {
		this.protection = Objects.requireNonNull(protection, "protection");
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBucketFill(PlayerBucketFillEvent event) {
		if (!protection.allowed(BukkitProtectionMapper.request(
			event.getPlayer(), ProtectionAction.BUCKET_FILL, event.getBlock()
		))) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBucketEmpty(PlayerBucketEmptyEvent event) {
		if (!protection.allowed(BukkitProtectionMapper.request(
			event.getPlayer(), ProtectionAction.BUCKET_EMPTY, event.getBlock()
		))) {
			event.setCancelled(true);
		}
	}
}
