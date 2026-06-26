package com.voluble.titanMC.milestones.ui;

import com.voluble.titanMC.milestones.config.MilestoneConfigurationManager;
import com.voluble.titanMC.milestones.model.MilestoneCategory;
import com.voluble.titanMC.milestones.model.MilestoneTrack;
import com.voluble.titanMC.milestones.service.MilestoneService;
import io.voluble.michellelib.menu.MenuService;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class MilestoneMenuService {
	private final OverviewMilestoneMenu overviewMenu;
	private final CategoryMilestoneMenu categoryMenu;
	private final TrackMilestoneMenu trackMenu;

	public MilestoneMenuService(
		MenuService menus,
		MilestoneConfigurationManager configuration,
		MilestoneService milestones
	) {
		Objects.requireNonNull(menus, "menus");
		Objects.requireNonNull(configuration, "configuration");
		MilestoneItemFactory items = new MilestoneItemFactory(milestones);
		overviewMenu = new OverviewMilestoneMenu(menus, configuration, items, this);
		categoryMenu = new CategoryMilestoneMenu(menus, configuration, items, this);
		trackMenu = new TrackMilestoneMenu(menus, configuration, items, this);
	}

	public void openOverview(Player player) {
		overviewMenu.open(player);
	}

	public void openCategory(Player player, String categoryId) {
		openCategory(player, categoryId, 0);
	}

	void openCategory(Player player, String categoryId, int page) {
		categoryMenu.open(player, categoryId, page);
	}

	void openTrack(Player player, MilestoneCategory category, MilestoneTrack track) {
		openTrack(player, category, track, 0);
	}

	void openTrack(Player player, MilestoneCategory category, MilestoneTrack track, int page) {
		trackMenu.open(player, category, track, page);
	}
}
