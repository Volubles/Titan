package com.voluble.titanMC.milestones.command;

import com.voluble.titanMC.milestones.ui.MilestoneMenuService;
import io.voluble.michellelib.commands.CommandModule;
import io.voluble.michellelib.commands.CommandRegistration;
import io.voluble.michellelib.commands.tree.CommandTree;

import java.util.Objects;

public final class MilestoneCommandModule implements CommandModule {
	private static final String USE_PERMISSION = "titanmc.milestones.use";

	private final MilestoneMenuService menus;

	public MilestoneCommandModule(MilestoneMenuService menus) {
		this.menus = Objects.requireNonNull(menus, "menus");
	}

	@Override
	public void register(CommandRegistration registration) {
		registration.register(CommandTree.root("milestones")
			.description("View milestone progress")
			.requiresPermission(USE_PERMISSION)
			.executesPlayer((player, context) -> {
				menus.openOverview(player);
				return CommandTree.ok();
			})
			.spec());
	}
}
