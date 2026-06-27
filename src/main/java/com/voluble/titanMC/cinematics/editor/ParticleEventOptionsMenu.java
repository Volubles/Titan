package com.voluble.titanMC.cinematics.editor;

import com.voluble.titanMC.cinematics.model.CinematicEventPosition;
import com.voluble.titanMC.cinematics.model.ParticleCinematicEvent;
import com.voluble.titanMC.util.ChatUtils;
import io.voluble.michellelib.menu.MenuService;
import io.voluble.michellelib.menu.template.MenuDefinition;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

final class ParticleEventOptionsMenu {
	private final MenuService menus;
	private final CinematicEditorService editor;
	private final CinematicEditorItemFactory items;

	ParticleEventOptionsMenu(MenuService menus, CinematicEditorService editor, CinematicEditorItemFactory items) {
		this.menus = Objects.requireNonNull(menus, "menus");
		this.editor = Objects.requireNonNull(editor, "editor");
		this.items = Objects.requireNonNull(items, "items");
	}

	void open(Player player, ParticleCinematicEvent event) {
		MenuDefinition.chest(3)
			.title(MiniMessage.miniMessage().deserialize("<#b36bff>Particle Event <gray>| Tick <white>" + event.tick()))
			.onOpen(context -> {
				context.setItem(10, CinematicEditorChrome.display(items.event(event)));
				context.setItem(11, promptButton(player, event, Material.BLAZE_POWDER, "<#b36bff><bold>Set Particle", "Type the Bukkit particle name.", value ->
					new ParticleCinematicEvent(event.tick(), event.row(), event.position(), value, event.count(), event.offsetX(), event.offsetY(), event.offsetZ(), event.speed())));
				context.setItem(12, promptButton(player, event, Material.GLOWSTONE_DUST, "<#f7d774><bold>Set Count", "Type the particle count.", value ->
					new ParticleCinematicEvent(event.tick(), event.row(), event.position(), event.particle(), CinematicEditorParsing.positiveInt(value), event.offsetX(), event.offsetY(), event.offsetZ(), event.speed())));
				context.setItem(13, promptButton(player, event, Material.SUGAR, "<#f7d774><bold>Set Speed", "Type the particle speed.", value ->
					new ParticleCinematicEvent(event.tick(), event.row(), event.position(), event.particle(), event.count(), event.offsetX(), event.offsetY(), event.offsetZ(), CinematicEditorParsing.decimal(value))));
				context.setItem(14, promptButton(player, event, Material.MAP, "<#30bbf1><bold>Set Offsets", "Type offsets as: x y z", value -> {
					double[] vector = CinematicEditorParsing.vector3(value);
					return new ParticleCinematicEvent(event.tick(), event.row(), event.position(), event.particle(), event.count(), vector[0], vector[1], vector[2], event.speed());
				}));
				context.setItem(15, button(Material.ENDER_EYE, "<#42d829><bold>Capture Location", List.of("<gray>Use your current position."), click -> {
					ParticleCinematicEvent updated = new ParticleCinematicEvent(
						event.tick(), event.row(), CinematicEventPosition.at(player.getLocation()), event.particle(), event.count(),
						event.offsetX(), event.offsetY(), event.offsetZ(), event.speed()
					);
					editor.replaceEvent(player, event, updated);
					click.actions().transition(() -> open(player, updated));
				}));
				context.setItem(16, promptButton(player, event, Material.CLOCK, "<#f7d774><bold>Set Tick", "Type the new tick.", value ->
					new ParticleCinematicEvent(CinematicEditorParsing.nonNegativeInt(value), event.row(), event.position(), event.particle(), event.count(), event.offsetX(), event.offsetY(), event.offsetZ(), event.speed())));
				context.setItem(17, promptButton(player, event, Material.HOPPER, "<#f7d774><bold>Set Row", "Type the new row. Row 0 is reserved for cameras.", value ->
					new ParticleCinematicEvent(event.tick(), CinematicEditorParsing.positiveInt(value), event.position(), event.particle(), event.count(), event.offsetX(), event.offsetY(), event.offsetZ(), event.speed())));
				context.setItem(22, button(Material.ARROW, "<#30bbf1><bold>Back", List.of("<gray>Return to the timeline."), click ->
					click.actions().transition(() -> editor.openTimeline(player))));
				context.setItem(26, button(Material.REDSTONE_BLOCK, "<#d43030><bold>Delete", List.of("<gray>Remove this particle event."), click -> {
					editor.removeEvent(player, event);
					click.actions().transition(() -> editor.openTimeline(player));
				}));
			})
			.build()
			.open(menus, player);
	}

	private io.voluble.michellelib.menu.item.MenuItem promptButton(
		Player player,
		ParticleCinematicEvent event,
		Material material,
		String name,
		String prompt,
		Function<String, ParticleCinematicEvent> mapper
	) {
		return button(material, name, List.of("<green>Click to edit."), click -> {
			editor.input().prompt(
				player,
				prompt,
				value -> {
					try {
						ParticleCinematicEvent updated = mapper.apply(value);
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
}
