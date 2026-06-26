package com.voluble.titanMC.milestones.tracking;

import com.voluble.titanMC.donatortools.item.DonatorToolRegistry;
import com.voluble.titanMC.donatortools.item.DonatorToolType;
import com.voluble.titanMC.milestones.bukkit.MilestoneCompletionHandler;
import com.voluble.titanMC.milestones.model.MilestoneMetric;
import com.voluble.titanMC.milestones.service.MilestoneService;
import com.voluble.titanMC.mines.event.MineBlockMinedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;

public final class MiningMilestoneListener implements Listener {
	private final MilestoneService milestones;
	private final MilestoneCompletionHandler completions;
	private final DonatorToolRegistry donatorTools;

	public MiningMilestoneListener(
		MilestoneService milestones,
		MilestoneCompletionHandler completions,
		DonatorToolRegistry donatorTools
	) {
		this.milestones = Objects.requireNonNull(milestones, "milestones");
		this.completions = Objects.requireNonNull(completions, "completions");
		this.donatorTools = Objects.requireNonNull(donatorTools, "donatorTools");
	}

	@EventHandler(ignoreCancelled = true)
	public void onMineBlockMined(MineBlockMinedEvent event) {
		completions.handle(event.player(), milestones.addProgress(
			event.player().getUniqueId(), MilestoneMetric.MINE_BLOCKS_BROKEN, "", 1L
		));
		completions.handle(event.player(), milestones.addProgress(
			event.player().getUniqueId(), MilestoneMetric.MINE_BLOCKS_IN_MINE, event.mineName(), 1L
		));
		DonatorToolType tool = donatorTools.identify(event.player().getInventory().getItemInMainHand()).orElse(null);
		if (tool != null) {
			completions.handle(event.player(), milestones.addProgress(
				event.player().getUniqueId(), MilestoneMetric.DONATOR_TOOL_USED, tool.id(), 1L
			));
		}
	}
}
