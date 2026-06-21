package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.protection.service.RegionEntryService;
import com.voluble.titanMC.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public final class RegionEntryProtectionListener implements Listener {

	private static final long DENY_MESSAGE_COOLDOWN_MILLIS = 1_000L;

	private final RegionEntryService entries;
	private final Map<UUID, Long> lastDenyMessage = new HashMap<>();
	private final Map<UUID, RegionEntryService.Membership> memberships = new HashMap<>();
	private final Map<PlayerMoveEvent, RegionEntryService.Transition> pendingMessages = new HashMap<>();

	public RegionEntryProtectionListener(RegionEntryService entries) {
		this.entries = Objects.requireNonNull(entries, "entries");
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void protectEntry(PlayerMoveEvent event) {
		if (event instanceof PlayerTeleportEvent) return;
		protect(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void protectTeleportEntry(PlayerTeleportEvent event) {
		protect(event);
	}

	private void protect(PlayerMoveEvent event) {
		if (!changedBlock(event.getFrom(), event.getTo())) return;
		Player player = event.getPlayer();
		BlockPosition from = BukkitProtectionMapper.position(event.getFrom());
		BlockPosition to = BukkitProtectionMapper.position(event.getTo());
		RegionEntryService.MovementMembership movement = entries.movementMembership(
			from,
			to,
			memberships.get(player.getUniqueId())
		);
		RegionEntryService.Membership source = movement.source();
		RegionEntryService.Membership target = movement.target();
		RegionEntryService.Transition transition = entries.evaluate(
			() -> BukkitProtectionMapper.actor(player),
			source,
			target
		);
		if (transition.allowed()) {
			memberships.put(player.getUniqueId(), target);
			if (transition.entryMessage().isPresent() || transition.exitMessage().isPresent()) {
				pendingMessages.put(event, transition);
			}
		} else {
			event.setCancelled(true);
			memberships.put(player.getUniqueId(), source);
			sendDenied(player, transition);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void sendTransitionMessages(PlayerMoveEvent event) {
		if (event instanceof PlayerTeleportEvent) return;
		sendTransitionMessagesAfter(event);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void sendTeleportTransitionMessages(PlayerTeleportEvent event) {
		sendTransitionMessagesAfter(event);
	}

	private void sendTransitionMessagesAfter(PlayerMoveEvent event) {
		RegionEntryService.Transition transition = pendingMessages.remove(event);
		if (transition == null || event.isCancelled()) return;
		transition.exitMessage().ifPresent(message ->
			send(event.getPlayer(), message)
		);
		transition.entryMessage().ifPresent(message ->
			send(event.getPlayer(), message)
		);
	}

	@EventHandler
	public void forgetPlayer(PlayerQuitEvent event) {
		UUID playerId = event.getPlayer().getUniqueId();
		lastDenyMessage.remove(playerId);
		memberships.remove(playerId);
	}

	private void sendDenied(Player player, RegionEntryService.Transition transition) {
		long now = System.currentTimeMillis();
		Long previous = lastDenyMessage.put(player.getUniqueId(), now);
		if (previous != null && now - previous < DENY_MESSAGE_COOLDOWN_MILLIS) return;
		String message = transition.denyMessage().orElse("<red>You may not enter this region.</red>");
		send(player, message);
	}

	private static void send(Player player, String message) {
		Component rendered;
		try {
			rendered = ChatUtils.format(player, message);
		} catch (RuntimeException exception) {
			rendered = Component.text(message);
		}
		player.sendMessage(rendered);
	}

	private static boolean changedBlock(Location from, Location to) {
		return to != null && (
			from.getWorld() != to.getWorld()
				|| from.getBlockX() != to.getBlockX()
				|| from.getBlockY() != to.getBlockY()
				|| from.getBlockZ() != to.getBlockZ()
		);
	}
}
