package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Objects;

public final class EntityInteractionProtectionListener implements Listener {

	private final ProtectionService protection;

	public EntityInteractionProtectionListener(ProtectionService protection) {
		this.protection = Objects.requireNonNull(protection, "protection");
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityPlace(EntityPlaceEvent event) {
		Player player = event.getPlayer();
		if (player == null || specialized(event.getEntity())) return;
		if (!allowed(player, ProtectionAction.ENTITY_PLACE, event.getEntity())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityInteract(PlayerInteractEntityEvent event) {
		if (event instanceof PlayerInteractAtEntityEvent || specialized(event.getRightClicked())) return;
		if (!allowed(event.getPlayer(), ProtectionAction.ENTITY_INTERACT, event.getRightClicked())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityInteractAt(PlayerInteractAtEntityEvent event) {
		if (specialized(event.getRightClicked())) return;
		if (!allowed(event.getPlayer(), ProtectionAction.ENTITY_INTERACT, event.getRightClicked())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageByEntityEvent event) {
		if (specialized(event.getEntity())) return;
		Player player = responsiblePlayer(event.getDamager());
		ProtectionAction action = event.getEntity() instanceof Player
			? ProtectionAction.PLAYER_PVP
			: ProtectionAction.ENTITY_DAMAGE;
		if (player != null && !allowed(player, action, event.getEntity())) {
			event.setCancelled(true);
		}
	}

	private boolean allowed(Player player, ProtectionAction action, Entity target) {
		return protection.allowed(BukkitProtectionMapper.request(player, action, target.getLocation()));
	}

	private static boolean specialized(Entity entity) {
		return entity instanceof Vehicle || entity instanceof Hanging;
	}

	static Player responsiblePlayer(Entity actor) {
		if (actor instanceof Player player) return player;
		if (actor instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
		return null;
	}
}
