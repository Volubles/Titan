package com.voluble.titanMC.milestones.tracking;

import com.voluble.titanMC.donatortools.item.DonatorToolRegistry;
import com.voluble.titanMC.donatortools.item.DonatorToolType;
import com.voluble.titanMC.milestones.bukkit.MilestoneCompletionHandler;
import com.voluble.titanMC.milestones.model.MilestoneMetric;
import com.voluble.titanMC.milestones.service.MilestoneService;
import com.voluble.titanMC.mines.event.MineBlockMinedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class MiningMilestoneListener implements Listener {
	private final MilestoneService milestones;
	private final MilestoneCompletionHandler completions;
	private final DonatorToolRegistry donatorTools;
	private final MineResetWindowTracker resetWindows;
	private final Map<UUID, Map<String, Long>> resetParticipation = new HashMap<>();

	public MiningMilestoneListener(
		MilestoneService milestones,
		MilestoneCompletionHandler completions,
		DonatorToolRegistry donatorTools,
		MineResetWindowTracker resetWindows
	) {
		this.milestones = Objects.requireNonNull(milestones, "milestones");
		this.completions = Objects.requireNonNull(completions, "completions");
		this.donatorTools = Objects.requireNonNull(donatorTools, "donatorTools");
		this.resetWindows = Objects.requireNonNull(resetWindows, "resetWindows");
	}

	@EventHandler(ignoreCancelled = true)
	public void onMineBlockMined(MineBlockMinedEvent event) {
		completions.handle(event.player(), milestones.addProgress(
			event.player().getUniqueId(), MilestoneMetric.MINE_BLOCKS_BROKEN, "", 1L
		));
		completions.handle(event.player(), milestones.addProgress(
			event.player().getUniqueId(), MilestoneMetric.MINE_BLOCKS_IN_MINE, event.mineName(), 1L
		));
		resetWindows.activeReset(event.mineName(), System.currentTimeMillis()).ifPresent(reset -> {
			completions.handle(event.player(), milestones.addProgress(
				event.player().getUniqueId(), MilestoneMetric.MINE_BLOCKS_AFTER_RESET, "", 1L
			));
			if (markResetParticipation(event.player().getUniqueId(), reset)) {
				completions.handle(event.player(), milestones.addProgress(
					event.player().getUniqueId(), MilestoneMetric.MINE_RESET_PARTICIPATION, "", 1L
				));
			}
		});
		DonatorToolType tool = donatorTools.identify(event.player().getInventory().getItemInMainHand()).orElse(null);
		if (tool != null) {
			completions.handle(event.player(), milestones.addProgress(
				event.player().getUniqueId(), MilestoneMetric.DONATOR_TOOL_USED, tool.id(), 1L
			));
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		resetParticipation.remove(event.getPlayer().getUniqueId());
	}

	private boolean markResetParticipation(UUID playerId, MineResetWindowTracker.ResetWindow reset) {
		Map<String, Long> playerResets = resetParticipation.computeIfAbsent(playerId, ignored -> new HashMap<>());
		Long previous = playerResets.put(reset.mineName(), reset.completedAtEpochMillis());
		return previous == null || previous.longValue() != reset.completedAtEpochMillis();
	}
}
