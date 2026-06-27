package com.voluble.titanMC.cinematics.editor;

import com.voluble.titanMC.cinematics.model.CameraPoint;
import com.voluble.titanMC.cinematics.model.CinematicDefinition;
import com.voluble.titanMC.cinematics.model.CinematicEvent;
import com.voluble.titanMC.cinematics.model.CommandCinematicEvent;
import com.voluble.titanMC.cinematics.model.ParticleCinematicEvent;
import com.voluble.titanMC.cinematics.model.SoundCinematicEvent;
import com.voluble.titanMC.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

final class CinematicEditorItemFactory {
	ItemStack emptySlot(int tick, int row) {
		Material material = row == 0 ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
		return item(
			material,
			row == 0 ? "<#30bbf1>Empty Camera Tick" : "<gray>Empty Event Tick",
			List.of(
				"<gray>Tick: <white>" + tick,
				"<gray>Row: <white>" + rowName(row),
				"",
				row == 0 ? "<green>Click to add a camera point here." : "<green>Click to add a timeline event here."
			)
		);
	}

	ItemStack cameraPoint(CameraPoint point, CinematicDefinition definition) {
		return item(
			Material.ENDER_EYE,
			"<#30bbf1><bold>Camera Point",
			List.of(
				"<gray>Tick: <white>" + point.tick(),
				"<gray>Row: <white>Camera Path",
				"<gray>Duration: <white>" + cameraDuration(point, definition) + " ticks",
				"",
				"<gray>World: <white>" + point.world(),
				"<gray>X/Y/Z: <white>" + rounded(point.x()) + ", " + rounded(point.y()) + ", " + rounded(point.z()),
				"",
				"<green>Click to edit this camera point."
			)
		);
	}

	ItemStack event(CinematicEvent event) {
		List<String> lore = new ArrayList<>();
		lore.add("<gray>Tick: <white>" + event.tick());
		lore.add("<gray>Row: <white>" + rowName(event.row()));
		lore.add("");
		switch (event) {
			case CommandCinematicEvent command -> {
				lore.add("<gray>Run as: <white>" + (command.console() ? "Console" : "Player"));
				lore.add("<gray>Command: <white>" + command.command());
			}
			case ParticleCinematicEvent particle -> {
				lore.add("<gray>Particle: <white>" + particle.particle());
				lore.add("<gray>Count: <white>" + particle.count());
				lore.add("<gray>Speed: <white>" + particle.speed());
			}
			case SoundCinematicEvent sound -> {
				lore.add("<gray>Sound: <white>" + sound.key());
				lore.add("<gray>Volume/Pitch: <white>" + sound.volume() + " / " + sound.pitch());
				lore.add("<gray>Category: <white>" + sound.category());
			}
		}
		lore.add("");
		lore.add("<green>Click to edit this event.");
		return item(material(event), name(event), lore);
	}

	ItemStack summary(CinematicDefinition definition, CinematicTimelineViewport viewport) {
		return item(
			Material.CLOCK,
			"<#f7d774><bold>" + definition.id().value(),
			List.of(
				"<gray>Duration: <white>" + definition.durationTicks() + " ticks",
				"<gray>Camera points: <white>" + definition.camera().points().size(),
				"<gray>Timeline events: <white>" + definition.timeline().events().size(),
				"",
				"<gray>Viewing ticks <white>" + viewport.startTick() + " - " + (viewport.startTick() + CinematicTimelineViewport.COLUMNS - 1),
				"<gray>Rows <white>" + viewport.startRow() + " - " + (viewport.startRow() + CinematicTimelineViewport.ROWS - 1)
			)
		);
	}

	ItemStack item(Material material, String name, List<String> lore) {
		ItemStack stack = new ItemStack(material);
		ItemMeta meta = stack.getItemMeta();
		if (meta == null) return stack;
		meta.displayName(ChatUtils.formatItem(name));
		meta.lore(lore.stream()
			.map(line -> line.isEmpty() ? Component.empty() : ChatUtils.formatItem(line))
			.toList());
		stack.setItemMeta(meta);
		return stack;
	}

	private int cameraDuration(CameraPoint point, CinematicDefinition definition) {
		return definition.camera().points().stream()
			.filter(next -> next.tick() > point.tick())
			.mapToInt(next -> next.tick() - point.tick())
			.findFirst()
			.orElse(Math.max(0, definition.durationTicks() - point.tick()));
	}

	private Material material(CinematicEvent event) {
		return switch (event.type()) {
			case COMMAND -> Material.COMMAND_BLOCK;
			case PARTICLE -> Material.BLAZE_POWDER;
			case SOUND -> Material.NOTE_BLOCK;
		};
	}

	private String name(CinematicEvent event) {
		return switch (event.type()) {
			case COMMAND -> "<#f7d774><bold>Command Event";
			case PARTICLE -> "<#b36bff><bold>Particle Event";
			case SOUND -> "<#42d829><bold>Sound Event";
		};
	}

	private String rowName(int row) {
		return row == 0 ? "Camera Path" : "Event Row " + row;
	}

	private String rounded(double value) {
		return String.format(java.util.Locale.US, "%.2f", value);
	}
}
