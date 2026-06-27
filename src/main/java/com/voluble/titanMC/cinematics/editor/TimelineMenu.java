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
		CinematicTimelineViewport viewport = new CinematicTimelineViewport(session.viewportSlot(), session.viewportRow());
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
				int timelineSlot = viewport.timelineSlot(column);
				int slot = viewport.slot(visibleRow, column);
				if (!CinematicEditorLayout.timelineSlot(slot)) continue;
				MenuItem item = row == 0
					? cameraSlot(player, definition, timelineSlot)
					: eventSlot(player, definition, timelineSlot, row);
				writer.set(slot, item);
			}
		}
	}

	private MenuItem cameraSlot(Player player, CinematicDefinition definition, int timelineSlot) {
		CameraPoint point = definition.camera().points().stream()
			.filter(candidate -> candidate.timelineSlot() == timelineSlot)
			.findFirst()
			.orElse(null);
		if (point != null) {
			return CinematicEditorChrome.item(items.cameraPoint(point, definition), context -> {
				if (context.click().isKeyboardClick()) {
					editor.session(player).jumpToSlot(point.timelineSlot());
					context.actions().transition(() -> open(player));
					return;
				}
				context.actions().transition(() -> editor.openCameraOptions(player, point));
			});
		}
		return CinematicEditorChrome.item(items.emptySlot(timelineSlot, 0), context -> {
			CameraPoint added = editor.addCameraPoint(player, timelineSlot);
			context.actions().transition(() -> editor.openCameraOptions(player, added));
		});
	}

	private MenuItem eventSlot(Player player, CinematicDefinition definition, int timelineSlot, int row) {
		CinematicEvent event = definition.timeline().events().stream()
			.filter(candidate -> candidate.timelineSlot() == timelineSlot && candidate.row() == row)
			.findFirst()
			.orElse(null);
		if (event != null) {
			return CinematicEditorChrome.item(items.event(event), context -> context.actions().transition(() -> editor.openEventOptions(player, event)));
		}
		return CinematicEditorChrome.item(items.emptySlot(timelineSlot, row), context -> context.actions().transition(() -> editor.openAddNode(player, timelineSlot, row)));
	}

	private void renderFooter(Player player, CinematicDefinition definition, SlotWriter writer) {
		writer.set(CinematicEditorLayout.SLOTS_BACK, CinematicEditorChrome.button(
			items,
			Material.ARROW,
			"<#f7d774><bold>Previous Slots",
			List.of(
				"<gray>Left click: <white>-1 slot",
				"<gray>Right click: <white>-9 slots",
				"<gray>Shift click: <white>-27 slots"
			),
			context -> moveSlots(player, -slotStep(context), context)
		));
		writer.set(CinematicEditorLayout.SLOTS_FORWARD, CinematicEditorChrome.button(
			items,
			Material.ARROW,
			"<#f7d774><bold>Next Slots",
			List.of(
				"<gray>Left click: <white>+1 slot",
				"<gray>Right click: <white>+9 slots",
				"<gray>Shift click: <white>+27 slots"
			),
			context -> moveSlots(player, slotStep(context), context)
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
			new CinematicTimelineViewport(editor.session(player).viewportSlot(), editor.session(player).viewportRow())
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
			"<#f7d774><bold>Set Length",
			List.of(
				"<gray>Current: <white>" + CinematicTimeFormat.tickTime(definition.durationTicks()),
				"<green>Click to type a new end tick."
			),
			context -> {
				context.actions().close();
				context.actions().nextTick(() -> editor.input().prompt(
					player,
					"Type the cinematic length in ticks.",
					value -> setDuration(player, value),
					() -> open(player)
				));
			}
		));
	}

	private void moveSlots(Player player, int delta, io.voluble.michellelib.menu.item.ClickContext context) {
		editor.session(player).moveSlots(delta);
		context.actions().transition(() -> open(player));
	}

	private int slotStep(io.voluble.michellelib.menu.item.ClickContext context) {
		if (context.shiftClick()) return 27;
		if (context.click().isRightClick()) return 9;
		return 1;
	}

	private void setDuration(Player player, String value) {
		try {
			int duration = Integer.parseInt(value);
			if (duration <= 0) throw new NumberFormatException("duration must be positive");
			editor.setDuration(player, duration);
		} catch (NumberFormatException exception) {
			player.sendMessage(ChatUtils.format("<#d43030>Length must be a positive number of ticks."));
		}
		open(player);
	}

	private String title(CinematicDefinition definition, CinematicTimelineViewport viewport) {
		return "<#30bbf1>" + definition.id().value()
			+ " <gray>| Slot <white>" + viewport.startSlot()
			+ " <gray>| Length <white>" + definition.durationTicks()
			+ " <gray>| <white>" + CinematicTimeFormat.seconds(definition.durationTicks()) + "s";
	}

	@FunctionalInterface
	private interface SlotWriter {
		void set(int slot, MenuItem item);
	}
}
