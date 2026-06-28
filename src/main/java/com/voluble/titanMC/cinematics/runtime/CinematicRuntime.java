package com.voluble.titanMC.cinematics.runtime;

import com.voluble.titanMC.cinematics.config.CinematicConfigurationManager;
import com.voluble.titanMC.cinematics.model.CinematicDefinition;
import com.voluble.titanMC.cinematics.model.CinematicId;
import com.voluble.titanMC.display.screen.ScreenEffectService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CinematicRuntime implements AutoCloseable {
	private final Plugin plugin;
	private final CinematicConfigurationManager configuration;
	private final CinematicScreenEffects screenEffects;
	private final Map<UUID, CinematicSession> sessions = new ConcurrentHashMap<>();

	public CinematicRuntime(Plugin plugin, CinematicConfigurationManager configuration, ScreenEffectService screenEffects) {
		this(plugin, configuration, screenEffects::show);
	}

	CinematicRuntime(Plugin plugin, CinematicConfigurationManager configuration, CinematicScreenEffects screenEffects) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.screenEffects = Objects.requireNonNull(screenEffects, "screenEffects");
	}

	public StartResult start(Player player, CinematicId id) {
		return start(player, id, CinematicPlaybackOptions.defaults());
	}

	public StartResult start(Player player, CinematicId id, CinematicPlaybackOptions options) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(options, "options");
		if (!configuration.current().enabled()) return StartResult.DISABLED;
		Optional<CinematicDefinition> definition = configuration.current().find(id);
		if (definition.isEmpty()) return StartResult.UNKNOWN;
		stop(player.getUniqueId(), true);
		CinematicSession session = new CinematicSession(plugin, player, definition.get(), options, sessions::remove, screenEffects);
		sessions.put(player.getUniqueId(), session);
		session.start();
		return StartResult.STARTED;
	}

	public boolean stop(UUID playerId, boolean restorePlayer) {
		CinematicSession session = sessions.remove(playerId);
		if (session == null) return false;
		session.stop(restorePlayer);
		return true;
	}

	public boolean active(UUID playerId) {
		return sessions.containsKey(playerId);
	}

	@Override
	public void close() {
		for (UUID playerId : java.util.List.copyOf(sessions.keySet())) {
			stop(playerId, true);
		}
	}

	public enum StartResult {
		STARTED,
		DISABLED,
		UNKNOWN
	}
}
