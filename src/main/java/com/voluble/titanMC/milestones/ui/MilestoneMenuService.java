package com.voluble.titanMC.milestones.ui;

import com.voluble.titanMC.milestones.config.MilestoneCatalog;
import com.voluble.titanMC.milestones.config.MilestoneConfigurationManager;
import com.voluble.titanMC.milestones.model.MilestoneCategory;
import com.voluble.titanMC.milestones.model.MilestoneTrack;
import com.voluble.titanMC.milestones.service.MilestoneService;
import io.voluble.michellelib.menu.MenuService;
import io.voluble.michellelib.menu.item.Items;
import io.voluble.michellelib.menu.item.MenuItem;
import io.voluble.michellelib.menu.template.MenuDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

public final class MilestoneMenuService {
	private final MenuService menus;
	private final MilestoneConfigurationManager configuration;
	private final MilestoneItemFactory items;

	public MilestoneMenuService(
		MenuService menus,
		MilestoneConfigurationManager configuration,
		MilestoneService milestones
	) {
		this.menus = Objects.requireNonNull(menus, "menus");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.items = new MilestoneItemFactory(milestones);
	}

	public void openOverview(Player player) {
		var config = configuration.current();
		MilestoneCatalog catalog = config.catalog();
		MenuDefinition.chest(config.overviewMenu().rows())
			.title(MiniMessage.miniMessage().deserialize(config.overviewMenu().title()))
			.onOpen(context -> {
				List<MilestoneCategory> categories = catalog.categories();
				for (int index = 0; index < categories.size() && index < MilestoneMenuLayout.CATEGORY_SLOTS.size(); index++) {
					MilestoneCategory category = categories.get(index);
					context.setItem(MilestoneMenuLayout.CATEGORY_SLOTS.get(index), categoryItem(player, category, catalog));
				}
				context.setItem(MilestoneMenuLayout.CLOSE_OVERVIEW, new Items.CloseItem());
			})
			.build()
			.open(menus, player);
	}

	public void openCategory(Player player, String categoryId) {
		var config = configuration.current();
		MilestoneCatalog catalog = config.catalog();
		MilestoneCategory category = catalog.category(categoryId).orElse(null);
		if (category == null || !category.enabled()) {
			openOverview(player);
			return;
		}
		MenuDefinition.chest(config.categoryMenu().rows())
			.title(title(config.categoryMenu().title(), category))
			.onOpen(context -> {
				List<MilestoneTrack> tracks = catalog.tracks(category.id());
				for (int index = 0; index < tracks.size() && index < MilestoneMenuLayout.TRACK_SLOTS.size(); index++) {
					context.setItem(MilestoneMenuLayout.TRACK_SLOTS.get(index), new Items.DisplayItem(items.track(player, tracks.get(index))));
				}
				context.setItem(MilestoneMenuLayout.CLOSE_CATEGORY, backItem(player));
			})
			.build()
			.open(menus, player);
	}

	private MenuItem categoryItem(Player player, MilestoneCategory category, MilestoneCatalog catalog) {
		ItemStack stack = items.category(player, category, catalog);
		return new MenuItem() {
			@Override
			public ItemStack render(Player viewer) {
				return stack.clone();
			}

			@Override
			public boolean onClick(io.voluble.michellelib.menu.item.ClickContext context) {
				if (category.enabled()) context.actions().transition(() -> openCategory(context.player(), category.id()));
				return true;
			}
		};
	}

	private MenuItem backItem(Player player) {
		ItemStack stack = items.back();
		return new MenuItem() {
			@Override
			public ItemStack render(Player viewer) {
				return stack.clone();
			}

			@Override
			public boolean onClick(io.voluble.michellelib.menu.item.ClickContext context) {
				context.actions().transition(() -> openOverview(player));
				return true;
			}
		};
	}

	private Component title(String template, MilestoneCategory category) {
		return MiniMessage.miniMessage().deserialize(template.replace("{category}", category.name()));
	}
}
