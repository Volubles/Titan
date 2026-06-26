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

	void open(Player player, MilestoneCategory category, MilestoneTrack track, int requestedPage) {
		var config = configuration.current();
		int pages = MilestoneMenuChrome.pages(track.tiers().size(), MilestoneMenuLayout.TIER_SLOTS.size());
		int page = MilestoneMenuChrome.clampPage(requestedPage, pages);
		int start = page * MilestoneMenuLayout.TIER_SLOTS.size();
		MenuDefinition.chest(config.categoryMenu().rows())
			.title(MiniMessage.miniMessage().deserialize(config.categoryMenu().title().replace("{category}", track.name())))
			.onOpen(context -> {
				for (int slot : MilestoneMenuLayout.FRAME_SLOTS) {
					context.setItem(slot, new Items.DisplayItem(MilestoneMenuChrome.filler()));
				}
				context.setItem(4, new Items.DisplayItem(items.trackDetails(player, track)));
				for (int index = 0; index < MilestoneMenuLayout.TIER_SLOTS.size(); index++) {
					int tierIndex = start + index;
					if (tierIndex >= track.tiers().size()) break;
					context.setItem(
						MilestoneMenuLayout.TIER_SLOTS.get(index),
						new Items.DisplayItem(items.tier(player, track, track.tiers().get(tierIndex)))
					);
				}
				if (page > 0) context.setItem(MilestoneMenuLayout.PREVIOUS, MilestoneMenuChrome.pageButton(
					org.bukkit.Material.ARROW, "<yellow>Previous Page", () -> navigator.openTrack(player, category, track, page - 1)
				));
				if (page + 1 < pages) context.setItem(MilestoneMenuLayout.NEXT, MilestoneMenuChrome.pageButton(
					org.bukkit.Material.ARROW, "<yellow>Next Page", () -> navigator.openTrack(player, category, track, page + 1)
				));
				context.setItem(MilestoneMenuLayout.BACK_TRACK, new Items.BackItem(() -> navigator.openCategory(player, category.id())));
			})
			.build()
			.open(menus, player);
	}

}
