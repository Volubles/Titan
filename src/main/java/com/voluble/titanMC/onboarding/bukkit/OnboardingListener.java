package com.voluble.titanMC.onboarding.bukkit;

import com.voluble.titanMC.onboarding.OnboardingService;
import com.voluble.titanMC.onboarding.config.OnboardingConfigurationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Objects;

public final class OnboardingListener implements Listener {
	private final Plugin plugin;
	private final OnboardingConfigurationManager configuration;
	private final OnboardingService onboarding;

	public OnboardingListener(
		Plugin plugin,
		OnboardingConfigurationManager configuration,
		OnboardingService onboarding
	) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.onboarding = Objects.requireNonNull(onboarding, "onboarding");
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		long delay = configuration.current().firstJoinDelayTicks();
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> onboarding.startFirstJoin(event.getPlayer()), delay);
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
