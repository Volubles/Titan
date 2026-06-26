package com.voluble.titanMC.milestones.tracking;

import com.voluble.titanMC.display.notice.MessageDefaults;
import com.voluble.titanMC.display.notice.PluginMessageService;
import com.voluble.titanMC.milestones.config.MilestoneConfigurationManager;
import com.voluble.titanMC.milestones.model.MilestoneMetric;
import com.voluble.titanMC.milestones.service.MilestoneService;
import com.voluble.titanMC.mines.event.MineBlockMinedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;

public final class MiningMilestoneListener implements Listener {
	private final MilestoneService milestones;
	private final MilestoneConfigurationManager configuration;
	private final PluginMessageService messages;

	public MiningMilestoneListener(
		MilestoneService milestones,
		MilestoneConfigurationManager configuration,
		PluginMessageService messages
	) {
		this.milestones = Objects.requireNonNull(milestones, "milestones");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.messages = Objects.requireNonNull(messages, "messages");
	}

	@EventHandler(ignoreCancelled = true)
	public void onMineBlockMined(MineBlockMinedEvent event) {
		var update = milestones.addProgress(
			event.player().getUniqueId(), MilestoneMetric.MINE_BLOCKS_BROKEN, "", 1L
		);
		for (var completion : update.completions()) {
			configuration.current().catalog().trackForTier(completion.tierId())
				.flatMap(track -> track.tiers().stream().filter(tier -> tier.id().equals(completion.tierId())).findFirst())
				.ifPresent(tier -> messages.send(event.player(), MessageDefaults.MILESTONE_COMPLETED,
					args -> args.plain("milestone", tier.name())));
		}
	}
}
