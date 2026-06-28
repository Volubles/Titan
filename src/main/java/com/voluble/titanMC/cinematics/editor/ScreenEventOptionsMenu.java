package com.voluble.titanMC.cinematics.editor;

import com.voluble.titanMC.cinematics.model.ScreenCinematicEvent;
import com.voluble.titanMC.display.screen.ScreenEffectId;
import com.voluble.titanMC.display.screen.ScreenEffectTiming;
import com.voluble.titanMC.util.ChatUtils;
import io.voluble.michellelib.menu.MenuService;
import io.voluble.michellelib.menu.template.MenuDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

final class ScreenEventOptionsMenu {
	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

	private final MenuService menus;
	private final CinematicEditorService editor;
	private final CinematicEditorItemFactory items;

	ScreenEventOptionsMenu(MenuService menus, CinematicEditorService editor, CinematicEditorItemFactory items) {
		this.menus = Objects.requireNonNull(menus, "menus");
		this.editor = Objects.requireNonNull(editor, "editor");
		this.items = Objects.requireNonNull(items, "items");
	}

	void open(Player player, ScreenCinematicEvent event) {
		MenuDefinition.chest(3)
			.title(MINI_MESSAGE.deserialize("<#d43030>Screen Event <gray>| Slot <white>" + event.timelineSlot()))
			.onOpen(context -> {
				context.setItem(4, CinematicEditorChrome.display(items.selectedEvent(event)));
				context.setItem(10, promptButton(player, event, Material.BLACK_DYE, "<#d43030><bold>Set Screen", CinematicEditorLore.edit("Current screen", event.screenId().value()), "Type the screen effect id.", value ->
					new ScreenCinematicEvent(event.tick(), event.timelineSlot(), event.row(), ScreenEffectId.of(value), event.title(), event.timing())));
				context.setItem(11, promptButton(player, event, Material.NAME_TAG, "<#f7d774><bold>Set Title", titleLore(event), "Type a MiniMessage title, or default.", value ->
					new ScreenCinematicEvent(event.tick(), event.timelineSlot(), event.row(), event.screenId(), title(value), event.timing())));
				context.setItem(12, promptButton(player, event, Material.CLOCK, "<#f7d774><bold>Set Tick", CinematicEditorLore.edit("Current tick", CinematicTimeFormat.tickTime(event.tick())), "Type the new tick.", value ->
					new ScreenCinematicEvent(CinematicEditorParsing.nonNegativeInt(value), event.timelineSlot(), event.row(), event.screenId(), event.title(), event.timing())));
				context.setItem(13, promptButton(player, event, Material.REPEATER, "<#30bbf1><bold>Set Timing", timingLore(event), "Type timing as: fadeIn hold fadeOut", value -> {
					long[] timing = CinematicEditorParsing.timing3(value);
					return new ScreenCinematicEvent(
						event.tick(),
						event.timelineSlot(),
						event.row(),
						event.screenId(),
						event.title(),
						Optional.of(new ScreenEffectTiming(timing[0], timing[1], timing[2]))
					);
				}));
				context.setItem(14, button(Material.COMPARATOR, "<#30bbf1><bold>Use Default Timing", List.of(
					"<gray>Current: <white>" + timingValue(event),
					"<green>Click to use the screen default timing."
				), click -> {
					ScreenCinematicEvent updated = new ScreenCinematicEvent(event.tick(), event.timelineSlot(), event.row(), event.screenId(), event.title(), Optional.empty());
					editor.replaceEvent(player, event, updated);
					click.actions().transition(() -> open(player, updated));
				}));
				context.setItem(15, promptButton(player, event, Material.HOPPER, "<#f7d774><bold>Set Row", CinematicEditorLore.edit("Current row", String.valueOf(event.row())), "Type the new row. Row 0 is reserved for cameras.", value ->
					new ScreenCinematicEvent(event.tick(), event.timelineSlot(), CinematicEditorParsing.positiveInt(value), event.screenId(), event.title(), event.timing())));
				context.setItem(16, button(
					Material.REPEATER,
					"<#f7d774><bold>Move Slot",
					CinematicEditorClickSteps.slotControlLore("this screen event"),
					click -> CinematicEditorTimelineMutations.moveEventSlot(
						player,
						editor,
						event,
						CinematicEditorClickSteps.signedSlotDelta(click),
						click.actions()
					)
				));
				context.setItem(18, button(
					Material.PISTON,
					"<#30bbf1><bold>Shift Timeline From Here",
					CinematicEditorClickSteps.slotControlLore("this event and everything after it"),
					click -> CinematicEditorTimelineMutations.shiftTimeline(
						player,
						editor,
						event.timelineSlot(),
						CinematicEditorClickSteps.signedSlotDelta(click),
						click.actions()
					)
				));
				context.setItem(22, button(Material.ARROW, "<#30bbf1><bold>Back", List.of("<gray>Return to the timeline."), click ->
					click.actions().transition(() -> editor.openTimeline(player))));
				context.setItem(26, button(Material.REDSTONE_BLOCK, "<#d43030><bold>Delete", List.of("<gray>Remove this screen event."), click -> {
					editor.removeEvent(player, event);
					click.actions().transition(() -> editor.openTimeline(player));
				}));
			})
			.build()
			.open(menus, player);
	}

	private io.voluble.michellelib.menu.item.MenuItem promptButton(
		Player player,
		ScreenCinematicEvent event,
		Material material,
		String name,
		List<String> lore,
		String prompt,
		Function<String, ScreenCinematicEvent> mapper
	) {
		return button(material, name, lore, click -> {
			editor.input().prompt(
				player,
				prompt,
				value -> {
					try {
						ScreenCinematicEvent updated = mapper.apply(value);
						editor.replaceEvent(player, event, updated);
						open(player, updated);
					} catch (IllegalArgumentException exception) {
						player.sendMessage(ChatUtils.format("<#d43030>Invalid value."));
						open(player, event);
					}
				},
				() -> open(player, event)
			);
		});
	}

	private io.voluble.michellelib.menu.item.MenuItem button(
		Material material,
		String name,
		List<String> lore,
		java.util.function.Consumer<io.voluble.michellelib.menu.item.ClickContext> click
	) {
		return CinematicEditorChrome.button(items, material, name, lore, context -> {
			context.actions().close();
			context.actions().nextTick(() -> click.accept(context));
		});
	}

	private Optional<Component> title(String value) {
		if (value.equalsIgnoreCase("default")) return Optional.empty();
		return Optional.of(MINI_MESSAGE.deserialize(value));
	}

	private List<String> titleLore(ScreenCinematicEvent event) {
		String value = event.title()
			.map(MINI_MESSAGE::serialize)
			.orElse("Screen default");
		return List.of(
			"<gray>Current title: <white>" + value,
			"<gray>Type <white>default</white> to use the screen default.",
			"<green>Click to edit."
		);
	}

	private List<String> timingLore(ScreenCinematicEvent event) {
		return List.of(
			"<gray>Current timing: <white>" + timingValue(event),
			"<gray>Format: <white>fadeIn hold fadeOut",
			"<green>Click to edit."
		);
	}

	private String timingValue(ScreenCinematicEvent event) {
		return event.timing()
			.map(timing -> timing.fadeInTicks() + " / " + timing.holdTicks() + " / " + timing.fadeOutTicks())
			.orElse("Screen default");
	}
}
