package com.voluble.titanMC.ranks.bukkit;

import com.voluble.titanMC.ranks.service.PlayerRankService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;

public final class PlayerRankListener implements Listener {
	private final PlayerRankService service;

	public PlayerRankListener(PlayerRankService service) {
		this.service = Objects.requireNonNull(service, "service");
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		service.assignStarting(event.getPlayer().getUniqueId());
	}
}
