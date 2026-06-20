package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionResolution;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import java.util.Objects;

public final class BucketProtectionListener implements Listener {

	private final ProtectionService protection;
	private final TrustedFluidFlow trusted;

	public BucketProtectionListener(ProtectionService protection) {
		this(protection, new TrustedFluidFlow());
	}

	public BucketProtectionListener(ProtectionService protection, TrustedFluidFlow trusted) {
		this.protection = Objects.requireNonNull(protection, "protection");
		this.trusted = Objects.requireNonNull(trusted, "trusted");
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

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void rememberBypassedFluidSource(PlayerBucketEmptyEvent event) {
		ProtectionResolution resolution = protection.resolve(BukkitProtectionMapper.request(
			event.getPlayer(), ProtectionAction.BUCKET_EMPTY, event.getBlock()
		));
		if (resolution.reason() == ProtectionResolution.Reason.BYPASS) trusted.add(event.getBlock());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void forgetCollectedFluid(PlayerBucketFillEvent event) {
		trusted.remove(event.getBlock());
	}
}
