package com.voluble.titanMC.milestones.tracking;

import com.voluble.titanMC.milestones.bukkit.MilestoneCompletionHandler;
import com.voluble.titanMC.milestones.model.MilestoneMetric;
import com.voluble.titanMC.milestones.service.MilestoneService;
import com.voluble.titanMC.ranks.event.PlayerRankChangedEvent;
import com.voluble.titanMC.ranks.model.PlayerRank;
import com.voluble.titanMC.ranks.model.PrisonRank;
import com.voluble.titanMC.ranks.model.RankId;
import com.voluble.titanMC.ranks.model.WardDefinition;
import com.voluble.titanMC.ranks.service.PlayerRankService;
import com.voluble.titanMC.ranks.service.RankCatalog;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.Objects;

public final class RankMilestoneListener implements Listener {
	private final RankCatalog catalog;
	private final PlayerRankService ranks;
	private final MilestoneService milestones;
	private final MilestoneCompletionHandler completions;

	public RankMilestoneListener(
		RankCatalog catalog,
		PlayerRankService ranks,
		MilestoneService milestones,
		MilestoneCompletionHandler completions
	) {
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.ranks = Objects.requireNonNull(ranks, "ranks");
		this.milestones = Objects.requireNonNull(milestones, "milestones");
		this.completions = Objects.requireNonNull(completions, "completions");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		ranks.current(event.getPlayer().getUniqueId()).ifPresent(rank -> markReached(event.getPlayer(), rank, false));
	}

	@EventHandler
	public void onRankChanged(PlayerRankChangedEvent event) {
		Player player = Bukkit.getPlayer(event.playerId());
		if (player == null) return;
		markReached(player, event.current(), true);
	}

	private void markReached(Player player, PlayerRank current, boolean notify) {
		int currentIndex = catalog.progressionIndex(current.rankId());
		List<PrisonRank> progression = catalog.ranks();
		for (int index = 0; index <= currentIndex && index < progression.size(); index++) {
			mark(player, MilestoneMetric.RANK_REACHED, progression.get(index).id().value(), notify);
		}
		for (WardDefinition ward : catalog.wards()) {
			RankId firstRank = ward.ranks().getFirst();
			if (catalog.progressionIndex(firstRank) <= currentIndex) {
				mark(player, MilestoneMetric.WARD_REACHED, ward.id().value(), notify);
			}
		}
	}

	private void mark(Player player, MilestoneMetric metric, String subject, boolean notify) {
		var update = milestones.setProgressAtLeast(player.getUniqueId(), metric, subject, 1L);
		if (notify) completions.handle(player, update);
	}
}
