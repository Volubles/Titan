package com.voluble.titanMC.onboarding.bukkit;

import com.voluble.titanMC.onboarding.OnboardingService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;
import java.util.Objects;

public final class OnboardingListener implements Listener {
	private final OnboardingService onboarding;

	public OnboardingListener(
		OnboardingService onboarding
	) {
		this.onboarding = Objects.requireNonNull(onboarding, "onboarding");
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		onboarding.startFirstJoin(event.getPlayer());
	}

	@EventHandler
	public void onInput(PlayerInputEvent event) {
		onboarding.handleInput(event.getPlayer(), event.getInput());
	}

	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof org.bukkit.entity.Player player && onboarding.active(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent event) {
		if (!onboarding.active(event.getPlayer().getUniqueId())) return;
		String lower = event.getMessage().toLowerCase(Locale.ROOT);
		if (lower.startsWith("/onboarding") || lower.startsWith("/ob")) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		onboarding.stop(event.getPlayer().getUniqueId(), false);
	}
}
