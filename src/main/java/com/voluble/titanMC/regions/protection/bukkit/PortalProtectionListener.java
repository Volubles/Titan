package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.Objects;

public final class PortalProtectionListener implements Listener {

	private static final ProtectionActor ACTOR = ProtectionActor.environment("portal-create");

	private final ProtectionService protection;

	public PortalProtectionListener(ProtectionService protection) {
		this.protection = Objects.requireNonNull(protection, "protection");
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPortalCreate(PortalCreateEvent event) {
		Player player = event.getEntity() instanceof Player found ? found : null;
		for (BlockState state : event.getBlocks()) {
			boolean allowed = player == null
				? protection.allowed(BukkitProtectionMapper.request(
					ACTOR, ProtectionAction.PORTAL_CREATE, state.getBlock()
				))
				: protection.allowed(BukkitProtectionMapper.request(
					player, ProtectionAction.PORTAL_CREATE, state.getBlock()
				));
			if (!allowed) {
				event.setCancelled(true);
				return;
			}
		}
	}
}
