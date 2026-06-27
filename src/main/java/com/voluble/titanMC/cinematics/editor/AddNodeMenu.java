package com.voluble.titanMC.cinematics.editor;

import com.voluble.titanMC.cinematics.model.CameraPoint;
import com.voluble.titanMC.cinematics.model.CinematicEventPosition;
import com.voluble.titanMC.cinematics.model.CommandCinematicEvent;
import com.voluble.titanMC.cinematics.model.ParticleCinematicEvent;
import com.voluble.titanMC.cinematics.model.SoundCinematicEvent;
import io.voluble.michellelib.menu.MenuService;
import io.voluble.michellelib.menu.template.MenuDefinition;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

final class AddNodeMenu {
	private final MenuService menus;
	private final CinematicEditorService editor;
	private final CinematicEditorItemFactory items;

	AddNodeMenu(MenuService menus, CinematicEditorService editor, CinematicEditorItemFactory items) {
		this.menus = Objects.requireNonNull(menus, "menus");
		this.editor = Objects.requireNonNull(editor, "editor");
		this.items = Objects.requireNonNull(items, "items");
	}

	void open(Player player, int tick, int row) {
		MenuDefinition.chest(3)
			.title(MiniMessage.miniMessage().deserialize("<#30bbf1>Add Node <gray>| Tick <white>" + tick))
			.onOpen(context -> {
				if (row == 0) {
					context.setItem(13, CinematicEditorChrome.button(
						items,
						Material.ENDER_EYE,
						"<#30bbf1><bold>Add Camera Point",
						List.of("<gray>Captures your current position.", "<green>Click to add."),
						click -> {
							editor.addCameraPoint(player, tick);
							click.actions().transition(() -> editor.openCameraOptions(player, CameraPoint.at(tick, player.getLocation())));
						}
					));
				} else {
					context.setItem(11, sound(player, tick, row));
					context.setItem(13, command(player, tick, row));
					context.setItem(15, particle(player, tick, row));
				}
				context.setItem(22, CinematicEditorChrome.button(
					items,
					Material.ARROW,
					"<#30bbf1><bold>Back",
					List.of("<gray>Return to the timeline."),
					click -> click.actions().transition(() -> editor.openTimeline(player))
				));
			})
			.build()
			.open(menus, player);
	}

	private io.voluble.michellelib.menu.item.MenuItem sound(Player player, int tick, int row) {
		return CinematicEditorChrome.button(
			items,
			Material.NOTE_BLOCK,
			"<#42d829><bold>Add Sound",
			List.of("<gray>Creates a sound event at your current position.", "<green>Click to add."),
			context -> {
				SoundCinematicEvent event = new SoundCinematicEvent(
					tick,
					row,
					CinematicEventPosition.at(player.getLocation()),
					"minecraft:block.note_block.pling",
					1.0f,
					1.0f,
					"MASTER"
				);
				editor.addEvent(player, event);
				context.actions().transition(() -> editor.openEventOptions(player, event));
			}
		);
	}

	private io.voluble.michellelib.menu.item.MenuItem command(Player player, int tick, int row) {
		return CinematicEditorChrome.button(
			items,
			Material.COMMAND_BLOCK,
			"<#f7d774><bold>Add Command",
			List.of("<gray>Creates a console command event.", "<green>Click to add."),
			context -> {
				CommandCinematicEvent event = new CommandCinematicEvent(tick, row, "tell {player} Cinematic command event", true);
				editor.addEvent(player, event);
				context.actions().transition(() -> editor.openEventOptions(player, event));
			}
		);
	}

	private io.voluble.michellelib.menu.item.MenuItem particle(Player player, int tick, int row) {
		return CinematicEditorChrome.button(
			items,
			Material.BLAZE_POWDER,
			"<#b36bff><bold>Add Particle",
			List.of("<gray>Creates a particle event at your current position.", "<green>Click to add."),
			context -> {
				ParticleCinematicEvent event = new ParticleCinematicEvent(
					tick,
					row,
					CinematicEventPosition.at(player.getLocation()),
					"CLOUD",
					8,
					0.0,
					0.0,
					0.0,
					0.0
				);
				editor.addEvent(player, event);
				context.actions().transition(() -> editor.openEventOptions(player, event));
			}
		);
	}
}
