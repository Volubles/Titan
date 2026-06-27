package com.voluble.titanMC.cinematics.editor;

import com.voluble.titanMC.cinematics.config.CinematicConfigurationManager;
import com.voluble.titanMC.cinematics.model.CameraPathDefinition;
import com.voluble.titanMC.cinematics.model.CameraPoint;
import com.voluble.titanMC.cinematics.model.CinematicDefinition;
import com.voluble.titanMC.cinematics.model.CinematicEvent;
import com.voluble.titanMC.cinematics.model.CinematicId;
import com.voluble.titanMC.cinematics.model.CinematicTimeline;
import com.voluble.titanMC.cinematics.runtime.CinematicRuntime;
import io.voluble.michellelib.menu.MenuService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CinematicEditorService {
	private final CinematicConfigurationManager configuration;
	private final CinematicRuntime runtime;
	private final Map<UUID, CinematicEditorSession> sessions = new ConcurrentHashMap<>();
	private final CinematicEditorInputService input;
	private final TimelineMenu timelineMenu;
	private final AddNodeMenu addNodeMenu;
	private final CameraPointOptionsMenu cameraOptions;
	private final CommandEventOptionsMenu commandOptions;
	private final SoundEventOptionsMenu soundOptions;
	private final ParticleEventOptionsMenu particleOptions;

	public CinematicEditorService(
		Plugin plugin,
		MenuService menus,
		CinematicConfigurationManager configuration,
		CinematicRuntime runtime
	) {
		Objects.requireNonNull(plugin, "plugin");
		Objects.requireNonNull(menus, "menus");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.runtime = Objects.requireNonNull(runtime, "runtime");
		CinematicEditorItemFactory items = new CinematicEditorItemFactory();
		input = new CinematicEditorInputService(plugin);
		timelineMenu = new TimelineMenu(menus, this, items);
		addNodeMenu = new AddNodeMenu(menus, this, items);
		cameraOptions = new CameraPointOptionsMenu(menus, this, items);
		commandOptions = new CommandEventOptionsMenu(menus, this, items);
		soundOptions = new SoundEventOptionsMenu(menus, this, items);
		particleOptions = new ParticleEventOptionsMenu(menus, this, items);
	}

	public CinematicEditorInputService input() {
		return input;
	}

	public void open(Player player, CinematicId id) {
		configuration.createIfMissing(id, player.getLocation());
		sessions.compute(player.getUniqueId(), (uuid, existing) -> existing == null || !existing.cinematicId().equals(id)
			? new CinematicEditorSession(id)
			: existing);
		timelineMenu.open(player);
	}

	void openTimeline(Player player) {
		timelineMenu.open(player);
	}

	void openAddNode(Player player, int tick, int row) {
		addNodeMenu.open(player, tick, row);
	}

	void openCameraOptions(Player player, CameraPoint point) {
		cameraOptions.open(player, point);
	}

	void openEventOptions(Player player, CinematicEvent event) {
		switch (event) {
			case com.voluble.titanMC.cinematics.model.CommandCinematicEvent command -> commandOptions.open(player, command);
			case com.voluble.titanMC.cinematics.model.ParticleCinematicEvent particle -> particleOptions.open(player, particle);
			case com.voluble.titanMC.cinematics.model.SoundCinematicEvent sound -> soundOptions.open(player, sound);
		}
	}

	CinematicEditorSession session(Player player) {
		CinematicEditorSession session = sessions.get(player.getUniqueId());
		if (session == null) throw new IllegalStateException("player has no cinematic editor session");
		return session;
	}

	Optional<CinematicDefinition> definition(Player player) {
		return configuration.current().find(session(player).cinematicId());
	}

	void addCameraPoint(Player player, int tick) {
		CinematicDefinition definition = requireDefinition(player);
		CameraPoint point = CameraPoint.at(tick, player.getLocation());
		List<CameraPoint> points = new ArrayList<>(definition.camera().points().stream()
			.filter(existing -> existing.tick() != tick)
			.toList());
		points.add(point);
		save(new CinematicDefinition(
			definition.id(),
			Math.max(definition.durationTicks(), tick),
			new CameraPathDefinition(definition.camera().restorePlayer(), points),
			definition.timeline()
		));
	}

	void replaceCameraPoint(Player player, CameraPoint oldPoint, CameraPoint newPoint) {
		CinematicDefinition definition = requireDefinition(player);
		List<CameraPoint> points = new ArrayList<>();
		boolean replaced = false;
		for (CameraPoint point : definition.camera().points()) {
			if (point.equals(oldPoint)) {
				points.add(newPoint);
				replaced = true;
			} else if (point.tick() != newPoint.tick()) {
				points.add(point);
			}
		}
		if (!replaced) points.add(newPoint);
		save(new CinematicDefinition(
			definition.id(),
			Math.max(definition.durationTicks(), newPoint.tick()),
			new CameraPathDefinition(definition.camera().restorePlayer(), points),
			definition.timeline()
		));
	}

	boolean removeCameraPoint(Player player, CameraPoint point) {
		CinematicDefinition definition = requireDefinition(player);
		if (definition.camera().points().size() <= 1) return false;
		List<CameraPoint> points = definition.camera().points().stream()
			.filter(existing -> !existing.equals(point))
			.toList();
		save(new CinematicDefinition(
			definition.id(),
			definition.durationTicks(),
			new CameraPathDefinition(definition.camera().restorePlayer(), points),
			definition.timeline()
		));
		return true;
	}

	void addEvent(Player player, CinematicEvent event) {
		CinematicDefinition definition = requireDefinition(player);
		List<CinematicEvent> events = new ArrayList<>(definition.timeline().events().stream()
			.filter(existing -> existing.tick() != event.tick() || existing.row() != event.row())
			.toList());
		events.add(event);
		save(new CinematicDefinition(
			definition.id(),
			Math.max(definition.durationTicks(), event.tick()),
			definition.camera(),
			new CinematicTimeline(events)
		));
	}

	void replaceEvent(Player player, CinematicEvent oldEvent, CinematicEvent newEvent) {
		CinematicDefinition definition = requireDefinition(player);
		List<CinematicEvent> events = new ArrayList<>(definition.timeline().events().stream()
			.filter(existing -> existing.equals(oldEvent) || existing.tick() != newEvent.tick() || existing.row() != newEvent.row())
			.toList());
		int index = events.indexOf(oldEvent);
		if (index >= 0) {
			events.set(index, newEvent);
		} else {
			events.add(newEvent);
		}
		save(new CinematicDefinition(
			definition.id(),
			Math.max(definition.durationTicks(), newEvent.tick()),
			definition.camera(),
			new CinematicTimeline(events)
		));
	}

	void removeEvent(Player player, CinematicEvent event) {
		CinematicDefinition definition = requireDefinition(player);
		save(new CinematicDefinition(
			definition.id(),
			definition.durationTicks(),
			definition.camera(),
			definition.timeline().without(event)
		));
	}

	void setDuration(Player player, int durationTicks) {
		CinematicDefinition definition = requireDefinition(player);
		save(new CinematicDefinition(definition.id(), durationTicks, definition.camera(), definition.timeline()));
	}

	void preview(Player player) {
		runtime.start(player, session(player).cinematicId());
	}

	private CinematicDefinition requireDefinition(Player player) {
		return definition(player).orElseThrow(() -> new IllegalStateException("Unknown cinematic: " + session(player).cinematicId().value()));
	}

	private void save(CinematicDefinition definition) {
		configuration.saveDefinition(definition);
	}
}
