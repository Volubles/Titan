package com.voluble.titanMC.milestones.tracking;

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

	public MiningMilestoneListener(
		MilestoneService milestones,
		MilestoneCompletionHandler completions
	) {
		this.milestones = Objects.requireNonNull(milestones, "milestones");
		this.completions = Objects.requireNonNull(completions, "completions");
	}

	@EventHandler(ignoreCancelled = true)
	public void onMineBlockMined(MineBlockMinedEvent event) {
		completions.handle(event.player(), milestones.addProgress(
			event.player().getUniqueId(), MilestoneMetric.MINE_BLOCKS_BROKEN, "", 1L
		));
	}
}
