package com.voluble.titanMC.milestones.ui;

import com.voluble.titanMC.milestones.config.MilestoneCatalog;
import com.voluble.titanMC.milestones.config.MilestoneConfigurationManager;
import com.voluble.titanMC.milestones.model.MilestoneCategory;
import com.voluble.titanMC.milestones.model.MilestoneTrack;
import io.voluble.michellelib.menu.MenuService;
import io.voluble.michellelib.menu.item.MenuItem;
import io.voluble.michellelib.menu.template.MenuDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

final class CategoryMilestoneMenu {
	private final MenuService menus;
	private final MilestoneConfigurationManager configuration;
	private final MilestoneItemFactory items;
	private final MilestoneMenuService navigator;

	CategoryMilestoneMenu(
		MenuService menus,
		MilestoneConfigurationManager configuration,
		MilestoneItemFactory items,
		MilestoneMenuService navigator
	) {
		this.menus = Objects.requireNonNull(menus, "menus");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.items = Objects.requireNonNull(items, "items");
		this.navigator = Objects.requireNonNull(navigator, "navigator");
	}

	void open(Player player, String categoryId) {
		var config = configuration.current();
		MilestoneCatalog catalog = config.catalog();
		MilestoneCategory category = catalog.category(categoryId).orElse(null);
		if (category == null || !category.enabled()) {
			navigator.openOverview(player);
			return;
		}
		MenuDefinition.chest(config.categoryMenu().rows())
			.title(title(config.categoryMenu().title(), category))
			.onOpen(context -> {
				List<MilestoneTrack> tracks = catalog.tracks(category.id());
				for (int index = 0; index < tracks.size() && index < MilestoneMenuLayout.TRACK_SLOTS.size(); index++) {
					context.setItem(MilestoneMenuLayout.TRACK_SLOTS.get(index), trackItem(player, category, tracks.get(index)));
				}
				context.setItem(MilestoneMenuLayout.CLOSE_CATEGORY, backItem());
			})
			.build()
			.open(menus, player);
	}

	private MenuItem trackItem(Player player, MilestoneCategory category, MilestoneTrack track) {
		ItemStack stack = items.track(player, track);
		return new MenuItem() {
			@Override
			public ItemStack render(Player viewer) {
				return stack.clone();
			}

			@Override
			public boolean onClick(io.voluble.michellelib.menu.item.ClickContext context) {
				context.actions().transition(() -> navigator.openTrack(context.player(), category, track));
				return true;
			}
		};
	}

	private MenuItem backItem() {
		ItemStack stack = items.back();
		return new MenuItem() {
			@Override
			public ItemStack render(Player viewer) {
				return stack.clone();
			}

			@Override
			public boolean onClick(io.voluble.michellelib.menu.item.ClickContext context) {
				context.actions().transition(() -> navigator.openOverview(context.player()));
				return true;
			}
		};
	}

	private Component title(String template, MilestoneCategory category) {
		return MiniMessage.miniMessage().deserialize(template.replace("{category}", category.name()));
	}
}
