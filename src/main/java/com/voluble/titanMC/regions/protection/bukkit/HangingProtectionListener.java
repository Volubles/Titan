package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Objects;

public final class HangingProtectionListener implements Listener {

	private final ProtectionService protection;

	public HangingProtectionListener(ProtectionService protection) {
		this.protection = Objects.requireNonNull(protection, "protection");
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onHangingPlace(HangingPlaceEvent event) {
		Player player = event.getPlayer();
		if (player != null && !allowed(player, event.getEntity())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onHangingBreak(HangingBreakByEntityEvent event) {
		Player player = EntityInteractionProtectionListener.responsiblePlayer(event.getRemover());
		if (player != null && !allowed(player, event.getEntity())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onHangingInteract(PlayerInteractEntityEvent event) {
		if (event instanceof PlayerInteractAtEntityEvent || !(event.getRightClicked() instanceof Hanging hanging)) return;
		if (!allowed(event.getPlayer(), hanging)) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onHangingInteractAt(PlayerInteractAtEntityEvent event) {
		if (event.getRightClicked() instanceof Hanging hanging && !allowed(event.getPlayer(), hanging)) {
			event.setCancelled(true);
		}
	}

	private boolean allowed(Player player, Hanging hanging) {
		return protection.allowed(BukkitProtectionMapper.request(
			player, ProtectionAction.HANGING_MODIFY, hanging.getLocation()
		));
	}
}
