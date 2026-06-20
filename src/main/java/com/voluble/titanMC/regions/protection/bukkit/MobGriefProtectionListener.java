package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityInteractEvent;

import java.util.Objects;

public final class MobGriefProtectionListener implements Listener {

	private static final ProtectionActor ACTOR = ProtectionActor.environment("mob-grief");

	private final ProtectionService protection;

	public MobGriefProtectionListener(ProtectionService protection) {
		this.protection = Objects.requireNonNull(protection, "protection");
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		if (event.getEntity() instanceof Mob && !allowed(event.getBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityInteract(EntityInteractEvent event) {
		if (event.getEntity() instanceof Mob && !allowed(event.getBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityBlockForm(EntityBlockFormEvent event) {
		if (event.getEntity() instanceof Mob && !allowed(event.getBlock())) {
			event.setCancelled(true);
		}
	}

	private boolean allowed(org.bukkit.block.Block block) {
		return protection.allowed(BukkitProtectionMapper.request(
			ACTOR, ProtectionAction.MOB_GRIEF, block
		));
	}
}
