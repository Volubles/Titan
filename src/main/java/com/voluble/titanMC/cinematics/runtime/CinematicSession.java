package com.voluble.titanMC.cinematics.runtime;

import com.voluble.titanMC.cinematics.model.CinematicDefinition;
import com.voluble.titanMC.cinematics.runtime.camera.CinematicCameraDriver;
import com.voluble.titanMC.cinematics.runtime.camera.CinematicCameraDrivers;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class CinematicSession {
	private static final int HOLD_SYNC_INTERVAL_TICKS = 10;

	private final Plugin plugin;
	private final Player player;
	private final CinematicDefinition definition;
	private final CinematicPlaybackOptions options;
	private final Consumer<UUID> completion;
	private final CinematicEventExecutor events;
	private final CinematicCameraDriver camera;
	private CinematicPlayerState playerState;
	private CinematicPlayerPresentation presentation;
	private BukkitTask task;
	private int frame;
	private int holdTicks;
	private boolean held;
	private boolean stopped;

	public CinematicSession(
		Plugin plugin,
		Player player,
		CinematicDefinition definition,
		Consumer<UUID> completion
	) {
		this(plugin, player, definition, CinematicPlaybackOptions.defaults(), completion);
	}

	public CinematicSession(
		Plugin plugin,
		Player player,
		CinematicDefinition definition,
		CinematicPlaybackOptions options,
		Consumer<UUID> completion
	) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.player = Objects.requireNonNull(player, "player");
		this.definition = Objects.requireNonNull(definition, "definition");
		this.options = Objects.requireNonNull(options, "options");
		this.completion = Objects.requireNonNull(completion, "completion");
		this.events = new CinematicEventExecutor(plugin);
		this.camera = CinematicCameraDrivers.create(plugin, player);
	}

	public UUID playerId() {
		return player.getUniqueId();
	}

	public CinematicDefinition definition() {
		return definition;
	}

	public void start() {
		if (task != null) return;
		setup();
		task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0L, 1L);
	}

	public void stop(boolean restorePlayer) {
		if (stopped) return;
		stopped = true;
		if (task != null) {
			task.cancel();
			task = null;
		}
		camera.stop();
		if (restorePlayer && player.isOnline() && playerState != null && definition.camera().restorePlayer()) {
			playerState.restore(player);
		}
		if (presentation != null) {
			presentation.restore();
			presentation = null;
		}
		completion.accept(player.getUniqueId());
	}

	private void setup() {
		playerState = CinematicPlayerState.capture(player);
		presentation = CinematicPlayerPresentation.apply(plugin, player);
		player.setAllowFlight(true);
		player.setFlying(true);
		player.setInvulnerable(true);
		camera.start(CameraPathInterpolator.locationAt(definition.camera().points(), 0));
	}

	private void tick() {
		try {
			if (!player.isOnline()) {
				stop(false);
				return;
			}
			if (held) {
				holdTick();
				return;
			}
			if (frame > definition.durationTicks()) {
				finishPlayback();
				return;
			}
			camera.move(frame, CameraPathInterpolator.locationAt(definition.camera().points(), frame));
			for (var event : definition.timeline().atTick(frame)) {
				events.execute(player, event);
			}
			frame++;
		} catch (Exception exception) {
			plugin.getLogger().warning(
				"Stopped cinematic " + definition.id().value() + " for " + player.getName() + ": " + exception.getMessage()
			);
			stop(true);
		}
	}

	private void finishPlayback() {
		if (options.endMode() == CinematicEndMode.HOLD_LAST_FRAME) {
			enterHold();
			return;
		}
		stop(true);
	}

	private void enterHold() {
		held = true;
		frame = definition.durationTicks();
		holdTicks = 0;
		camera.move(frame, CameraPathInterpolator.locationAt(definition.camera().points(), frame));
		options.notifyHeld();
	}

	private void holdTick() {
		if (holdTicks % HOLD_SYNC_INTERVAL_TICKS == 0) {
			camera.move(frame + holdTicks, CameraPathInterpolator.locationAt(definition.camera().points(), frame));
		}
		holdTicks++;
	}
}
