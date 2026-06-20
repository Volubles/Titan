package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.Objects;

public final class EntityContainerProtectionListener implements Listener {

	private final ProtectionService protection;

	public EntityContainerProtectionListener(ProtectionService protection) {
		this.protection = Objects.requireNonNull(protection, "protection");
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (!(event.getPlayer() instanceof Player player)) return;
		if (!(event.getInventory().getHolder(false) instanceof Entity holder)) return;
		if (!protection.allowed(BukkitProtectionMapper.request(
			player, ProtectionAction.CONTAINER_OPEN, holder.getLocation()
		))) {
			event.setCancelled(true);
		}
	}
}
