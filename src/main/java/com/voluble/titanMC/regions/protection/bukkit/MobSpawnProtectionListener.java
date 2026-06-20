package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class MobSpawnProtectionListener implements Listener {

	private static final ProtectionActor ACTOR = ProtectionActor.environment("mob-spawn");
	private static final Set<CreatureSpawnEvent.SpawnReason> DIRECT_SPAWNS = EnumSet.of(
		CreatureSpawnEvent.SpawnReason.COMMAND,
		CreatureSpawnEvent.SpawnReason.CUSTOM,
		CreatureSpawnEvent.SpawnReason.DEFAULT,
		CreatureSpawnEvent.SpawnReason.EGG,
		CreatureSpawnEvent.SpawnReason.SPAWNER_EGG,
		CreatureSpawnEvent.SpawnReason.DISPENSE_EGG,
		CreatureSpawnEvent.SpawnReason.BUCKET,
		CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN,
		CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM,
		CreatureSpawnEvent.SpawnReason.BUILD_COPPERGOLEM,
		CreatureSpawnEvent.SpawnReason.BUILD_WITHER
	);

	private final ProtectionService protection;

	public MobSpawnProtectionListener(ProtectionService protection) {
		this.protection = Objects.requireNonNull(protection, "protection");
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (DIRECT_SPAWNS.contains(event.getSpawnReason())) return;
		if (!protection.allowed(BukkitProtectionMapper.request(
			ACTOR, ProtectionAction.MOB_SPAWN, event.getLocation().getBlock()
		))) {
			event.setCancelled(true);
		}
	}
}
