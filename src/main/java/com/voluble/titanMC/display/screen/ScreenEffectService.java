package com.voluble.titanMC.display.screen;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class ScreenEffectService implements Listener, AutoCloseable {
	private final Plugin plugin;
	private final ScreenEffectConfigurationManager configuration;
	private final ScreenEffectOverlayResolver overlays;
	private final ScreenEffectHudController hud;
	private final MiniMessage miniMessage = MiniMessage.miniMessage();
	private final Map<UUID, ActiveScreenEffect> active = new ConcurrentHashMap<>();
	private Predicate<UUID> hudRestoreBlocked = ignored -> false;

	public ScreenEffectService(Plugin plugin, ScreenEffectConfigurationManager configuration) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.overlays = new ScreenEffectOverlayResolver(plugin);
		this.hud = new ScreenEffectHudController(plugin);
	}

	public boolean show(Player player, ScreenEffectRequest request) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(request, "request");
		ScreenEffectDefinition definition = configuration.current().find(request.screenId()).orElse(null);
		if (definition == null) return false;
		if (!Bukkit.isPrimaryThread()) {
			Bukkit.getScheduler().runTask(plugin, () -> show(player, request));
			return true;
		}
		replaceCurrent(player);
		ScreenEffectTiming timing = request.timing().orElse(definition.timing());
		Component title = request.title().orElseGet(() -> title(definition.title()));
		Component overlayComponent;
		try {
			overlayComponent = overlays.resolve(definition);
		} catch (IllegalStateException exception) {
			return false;
		}
		Title overlay = Title.title(title, overlayComponent, times(timing));
		GameMode gameMode = player.getGameMode();
		if (definition.hideHud()) hud.hide(player);
		BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> finish(player.getUniqueId()), Math.max(1L, timing.totalTicks()));
		active.put(player.getUniqueId(), new ActiveScreenEffect(task, definition.hideHud(), gameMode));
		player.showTitle(overlay);
		return true;
	}

	public void stop(UUID playerId) {
		ActiveScreenEffect effect = active.remove(playerId);
		if (effect == null) return;
		Player player = Bukkit.getPlayer(playerId);
		effect.cancel(player, hud, !hudRestoreBlocked.test(playerId));
	}

	public void reload() {
		configuration.reload();
	}

	public void blockHudRestoreWhen(Predicate<UUID> hudRestoreBlocked) {
		this.hudRestoreBlocked = Objects.requireNonNull(hudRestoreBlocked, "hudRestoreBlocked");
	}

	private void replaceCurrent(Player player) {
		ActiveScreenEffect current = active.remove(player.getUniqueId());
		if (current != null) {
			current.cancel(player, hud, !hudRestoreBlocked.test(player.getUniqueId()));
		}
	}

	private void finish(UUID playerId) {
		ActiveScreenEffect effect = active.remove(playerId);
		if (effect == null) return;
		Player player = Bukkit.getPlayer(playerId);
		effect.restore(player, hud, !hudRestoreBlocked.test(playerId));
	}

	private Component title(String configuredTitle) {
		return configuredTitle.isBlank() ? Component.empty() : miniMessage.deserialize(configuredTitle);
	}

	private Title.Times times(ScreenEffectTiming timing) {
		return Title.Times.times(
			Duration.ofMillis(timing.fadeInTicks() * 50L),
			Duration.ofMillis(timing.holdTicks() * 50L),
			Duration.ofMillis(timing.fadeOutTicks() * 50L)
		);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		stop(event.getPlayer().getUniqueId());
	}

	@Override
	public void close() {
		for (UUID playerId : java.util.List.copyOf(active.keySet())) {
			stop(playerId);
		}
	}
}
