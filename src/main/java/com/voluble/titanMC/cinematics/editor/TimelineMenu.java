package com.voluble.titanMC.cinematics.editor;

import com.voluble.titanMC.cinematics.model.CameraPoint;
import com.voluble.titanMC.cinematics.model.CinematicDefinition;
import com.voluble.titanMC.cinematics.model.CinematicEvent;
import com.voluble.titanMC.util.ChatUtils;
import io.voluble.michellelib.menu.MenuService;
import io.voluble.michellelib.menu.item.MenuItem;
import io.voluble.michellelib.menu.template.MenuDefinition;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

final class TimelineMenu {
	private final MenuService menus;
	private final CinematicEditorService editor;
	private final CinematicEditorItemFactory items;

	TimelineMenu(MenuService menus, CinematicEditorService editor, CinematicEditorItemFactory items) {
		this.menus = Objects.requireNonNull(menus, "menus");
		this.editor = Objects.requireNonNull(editor, "editor");
		this.items = Objects.requireNonNull(items, "items");
	}

	void open(Player player) {
		CinematicEditorSession session = editor.session(player);
		CinematicTimelineViewport viewport = new CinematicTimelineViewport(session.viewportTick(), session.viewportRow());
		CinematicDefinition definition = editor.definition(player).orElse(null);
		if (definition == null) {
			player.closeInventory();
			return;
		}
		MenuDefinition.chest(CinematicEditorLayout.ROWS)
			.title(MiniMessage.miniMessage().deserialize(title(definition, viewport)))
			.onOpen(context -> {
				renderTimeline(definition, viewport, player, context::setItem);
				renderFooter(player, definition, context::setItem);
			})
			.build()
			.open(menus, player);
	}

	private void renderTimeline(
		CinematicDefinition definition,
		CinematicTimelineViewport viewport,
		Player player,
		SlotWriter writer
	) {
		for (int visibleRow = 0; visibleRow < CinematicTimelineViewport.ROWS; visibleRow++) {
			int row = viewport.row(visibleRow);
			for (int column = 0; column < CinematicTimelineViewport.COLUMNS; column++) {
				int tick = viewport.tick(column);
				int slot = viewport.slot(visibleRow, column);
				if (!CinematicEditorLayout.timelineSlot(slot)) continue;
				MenuItem item = row == 0
					? cameraSlot(player, definition, tick)
					: eventSlot(player, definition, tick, row);
				writer.set(slot, item);
			}
		}
	}

	private MenuItem cameraSlot(Player player, CinematicDefinition definition, int tick) {
		CameraPoint point = definition.camera().points().stream()
			.filter(candidate -> candidate.tick() == tick)
			.findFirst()
			.orElse(null);
		if (point != null) {
			return CinematicEditorChrome.item(items.cameraPoint(point, definition), context -> {
				if (context.click().isKeyboardClick()) {
					editor.session(player).jumpTo(point.tick());
					context.actions().transition(() -> open(player));
					return;
				}
				context.actions().transition(() -> editor.openCameraOptions(player, point));
			});
		}
		return CinematicEditorChrome.item(items.emptySlot(tick, 0), context -> {
			editor.addCameraPoint(player, tick);
			context.actions().transition(() -> editor.openCameraOptions(player, CameraPoint.at(tick, player.getLocation())));
		});
	}

	private MenuItem eventSlot(Player player, CinematicDefinition definition, int tick, int row) {
		CinematicEvent event = definition.timeline().events().stream()
			.filter(candidate -> candidate.tick() == tick && candidate.row() == row)
			.findFirst()
			.orElse(null);
		if (event != null) {
			return CinematicEditorChrome.item(items.event(event), context -> context.actions().transition(() -> editor.openEventOptions(player, event)));
		}
		return CinematicEditorChrome.item(items.emptySlot(tick, row), context -> context.actions().transition(() -> editor.openAddNode(player, tick, row)));
	}

	private void renderFooter(Player player, CinematicDefinition definition, SlotWriter writer) {
		writer.set(CinematicEditorLayout.TICKS_BACK, CinematicEditorChrome.button(
			items,
			Material.ARROW,
			"<#f7d774><bold>Previous Ticks",
			List.of(
				"<gray>Left click: <white>-1 tick",
				"<gray>Right click: <white>-20 ticks",
				"<gray>Shift click: <white>-60 ticks"
			),
			context -> moveTicks(player, -tickStep(context), context)
		));
		writer.set(CinematicEditorLayout.TICKS_FORWARD, CinematicEditorChrome.button(
			items,
			Material.ARROW,
			"<#f7d774><bold>Next Ticks",
			List.of(
				"<gray>Left click: <white>+1 tick",
				"<gray>Right click: <white>+20 ticks",
				"<gray>Shift click: <white>+60 ticks"
			),
			context -> moveTicks(player, tickStep(context), context)
		));
		writer.set(CinematicEditorLayout.ROW_UP, CinematicEditorChrome.button(
			items,
			Material.FEATHER,
			"<#30bbf1><bold>Rows Up",
			List.of("<gray>Show earlier timeline rows."),
			context -> {
				editor.session(player).moveRows(-1);
				context.actions().transition(() -> open(player));
			}
		));
		writer.set(CinematicEditorLayout.ROW_DOWN, CinematicEditorChrome.button(
			items,
			Material.FEATHER,
			"<#30bbf1><bold>Rows Down",
			List.of("<gray>Show later timeline rows."),
			context -> {
				editor.session(player).moveRows(1);
				context.actions().transition(() -> open(player));
			}
		));
		writer.set(CinematicEditorLayout.PLAY, CinematicEditorChrome.button(
			items,
			Material.LIME_DYE,
			"<#42d829><bold>Play Preview",
			List.of("<gray>Close the editor and preview this cinematic."),
			context -> {
				context.actions().close();
				context.actions().nextTick(() -> editor.preview(player));
			}
		));
		writer.set(CinematicEditorLayout.SUMMARY, CinematicEditorChrome.display(items.summary(
			definition,
			new CinematicTimelineViewport(editor.session(player).viewportTick(), editor.session(player).viewportRow())
		)));
		writer.set(CinematicEditorLayout.CLOSE, CinematicEditorChrome.button(
			items,
			Material.BARRIER,
			"<#d43030><bold>Close",
			List.of("<gray>Changes are saved immediately."),
			context -> context.actions().close()
		));
		writer.set(CinematicEditorLayout.DURATION, CinematicEditorChrome.button(
			items,
			Material.CLOCK,
			"<#f7d774><bold>Set Duration",
			List.of(
				"<gray>Current: <white>" + definition.durationTicks() + " ticks",
				"<green>Click to type a new duration."
			),
			context -> {
				context.actions().close();
				context.actions().nextTick(() -> editor.input().prompt(
					player,
					"Type the cinematic duration in ticks.",
					value -> setDuration(player, value),
					() -> open(player)
				));
			}
		));
	}

	private void moveTicks(Player player, int delta, io.voluble.michellelib.menu.item.ClickContext context) {
		editor.session(player).moveTicks(delta);
		context.actions().transition(() -> open(player));
	}

	private int tickStep(io.voluble.michellelib.menu.item.ClickContext context) {
		if (context.shiftClick()) return 60;
		if (context.click().isRightClick()) return 20;
		return 1;
	}

	private void setDuration(Player player, String value) {
		try {
			int duration = Integer.parseInt(value);
			if (duration <= 0) throw new NumberFormatException("duration must be positive");
			editor.setDuration(player, duration);
		} catch (NumberFormatException exception) {
			player.sendMessage(ChatUtils.format("<#d43030>Duration must be a positive number of ticks."));
		}
		open(player);
	}

	private String title(CinematicDefinition definition, CinematicTimelineViewport viewport) {
		return "<#30bbf1>" + definition.id().value()
			+ " <gray>| Tick <white>" + viewport.startTick()
			+ "<gray>/<white>" + definition.durationTicks();
	}

	@FunctionalInterface
	private interface SlotWriter {
		void set(int slot, MenuItem item);
	}
}
