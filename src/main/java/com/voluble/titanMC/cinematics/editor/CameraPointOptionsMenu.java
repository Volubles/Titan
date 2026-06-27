package com.voluble.titanMC.cinematics.editor;

import com.voluble.titanMC.cinematics.model.CameraPoint;
import com.voluble.titanMC.util.ChatUtils;
import io.voluble.michellelib.menu.MenuService;
import io.voluble.michellelib.menu.template.MenuDefinition;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

final class CameraPointOptionsMenu {
	private final MenuService menus;
	private final CinematicEditorService editor;
	private final CinematicEditorItemFactory items;

	CameraPointOptionsMenu(MenuService menus, CinematicEditorService editor, CinematicEditorItemFactory items) {
		this.menus = Objects.requireNonNull(menus, "menus");
		this.editor = Objects.requireNonNull(editor, "editor");
		this.items = Objects.requireNonNull(items, "items");
	}

	void open(Player player, CameraPoint point) {
		MenuDefinition.chest(3)
			.title(MiniMessage.miniMessage().deserialize("<#30bbf1>Camera Point <gray>| Tick <white>" + point.tick()))
			.onOpen(context -> {
				context.setItem(10, CinematicEditorChrome.button(
					items,
					Material.ENDER_PEARL,
					"<#30bbf1><bold>Teleport",
					List.of("<gray>Teleport to this camera point."),
					click -> {
						player.teleport(point.toLocation());
						click.actions().transition(() -> open(player, point));
					}
				));
				context.setItem(11, CinematicEditorChrome.button(
					items,
					Material.ENDER_EYE,
					"<#42d829><bold>Capture Current Location",
					List.of("<gray>Replace this point with your current position."),
					click -> {
						CameraPoint updated = CameraPoint.at(point.tick(), player.getLocation());
						editor.replaceCameraPoint(player, point, updated);
						click.actions().transition(() -> open(player, updated));
					}
				));
				context.setItem(12, CinematicEditorChrome.button(
					items,
					Material.CLOCK,
					"<#f7d774><bold>Set Tick",
					List.of("<gray>Current: <white>" + point.tick()),
					click -> promptTick(player, point, click.actions())
				));
				context.setItem(15, CinematicEditorChrome.button(
					items,
					Material.REDSTONE_BLOCK,
					"<#d43030><bold>Delete",
					List.of("<gray>Remove this camera point.", "<red>At least one point must remain."),
					click -> {
						if (!editor.removeCameraPoint(player, point)) {
							player.sendMessage(ChatUtils.format("<#d43030>A cinematic must keep at least one camera point."));
						}
						click.actions().transition(() -> editor.openTimeline(player));
					}
				));
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

	private void promptTick(Player player, CameraPoint point, io.voluble.michellelib.menu.item.MenuActions actions) {
		actions.close();
		actions.nextTick(() -> editor.input().prompt(
			player,
			"Type the new camera tick.",
			value -> {
				try {
					int tick = CinematicEditorParsing.nonNegativeInt(value);
					CameraPoint updated = new CameraPoint(tick, point.world(), point.x(), point.y(), point.z(), point.yaw(), point.pitch());
					editor.replaceCameraPoint(player, point, updated);
					open(player, updated);
				} catch (NumberFormatException exception) {
					player.sendMessage(ChatUtils.format("<#d43030>Tick must be a whole number greater than or equal to 0."));
					open(player, point);
				}
			},
			() -> open(player, point)
		));
	}
}
