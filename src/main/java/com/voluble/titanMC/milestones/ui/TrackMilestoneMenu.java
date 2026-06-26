package com.voluble.titanMC.milestones.ui;

import com.voluble.titanMC.milestones.config.MilestoneConfigurationManager;
import com.voluble.titanMC.milestones.model.MilestoneCategory;
import com.voluble.titanMC.milestones.model.MilestoneTrack;
import io.voluble.michellelib.menu.MenuService;
import io.voluble.michellelib.menu.item.Items;
import io.voluble.michellelib.menu.template.MenuDefinition;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.Objects;

final class TrackMilestoneMenu {
	private final MenuService menus;
	private final MilestoneConfigurationManager configuration;
	private final MilestoneItemFactory items;
	private final MilestoneMenuService navigator;

	TrackMilestoneMenu(
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

	void open(Player player, MilestoneCategory category, MilestoneTrack track) {
		var config = configuration.current();
		MenuDefinition.chest(config.categoryMenu().rows())
			.title(MiniMessage.miniMessage().deserialize(config.categoryMenu().title().replace("{category}", track.name())))
			.onOpen(context -> {
				context.setItem(4, new Items.DisplayItem(items.trackDetails(player, track)));
				for (int index = 0; index < track.tiers().size() && index < MilestoneMenuLayout.TIER_SLOTS.size(); index++) {
					context.setItem(
						MilestoneMenuLayout.TIER_SLOTS.get(index),
						new Items.DisplayItem(items.tier(player, track, track.tiers().get(index)))
					);
				}
				context.setItem(MilestoneMenuLayout.BACK_TRACK, new Items.BackItem(() -> navigator.openCategory(player, category.id())));
			})
			.build()
			.open(menus, player);
	}

}
