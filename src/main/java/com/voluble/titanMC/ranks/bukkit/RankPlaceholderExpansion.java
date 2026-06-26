package com.voluble.titanMC.ranks.bukkit;

import com.voluble.titanMC.ranks.model.PlayerRank;
import com.voluble.titanMC.ranks.model.PrisonRank;
import com.voluble.titanMC.ranks.model.WardDefinition;
import com.voluble.titanMC.ranks.service.PlayerRankService;
import com.voluble.titanMC.ranks.service.RankCatalog;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class RankPlaceholderExpansion extends PlaceholderExpansion {
	private final Plugin plugin;
	private final Supplier<RankCatalog> catalog;
	private final PlayerRankService ranks;

	public RankPlaceholderExpansion(Plugin plugin, Supplier<RankCatalog> catalog, PlayerRankService ranks) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.ranks = Objects.requireNonNull(ranks, "ranks");
	}

	@Override
	public @NotNull String getIdentifier() {
		return "titanmc";
	}

	@Override
	public @NotNull String getAuthor() {
		return "Voluble";
	}

	@Override
	public @NotNull String getVersion() {
		return plugin.getPluginMeta().getVersion();
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public String onRequest(OfflinePlayer player, @NotNull String params) {
		if (player == null) return "";
		Optional<PlayerRank> currentRank = ranks.current(player.getUniqueId());
		if (currentRank.isEmpty()) return "";

		RankCatalog catalog = this.catalog.get();
		PrisonRank current = catalog.requireRank(currentRank.get().rankId());
		WardDefinition ward = catalog.requireWard(current.wardId());
		Optional<PrisonRank> next = catalog.nextRank(current.id());
		String key = params.toLowerCase(java.util.Locale.ROOT);

		return switch (key) {
			case "rank", "rank_display" -> current.displayName();
			case "rank_id" -> current.id().value();
			case "ward", "ward_display" -> ward.displayName();
			case "ward_id" -> ward.id().value();
			case "next_rank", "next_rank_display" -> next.map(PrisonRank::displayName).orElse("");
			case "next_rank_id" -> next.map(rank -> rank.id().value()).orElse("");
			case "next_ward", "next_ward_display" -> next
				.map(rank -> catalog.requireWard(rank.wardId()).displayName())
				.orElse("");
			case "next_ward_id" -> next
				.map(rank -> catalog.requireWard(rank.wardId()).id().value())
				.orElse("");
			case "rank_position" -> Integer.toString(catalog.progressionIndex(current.id()) + 1);
			case "rank_total" -> Integer.toString(catalog.ranks().size());
			case "rank_progress_percent" -> rankProgressPercent(current);
			default -> null;
		};
	}

	private String rankProgressPercent(PrisonRank current) {
		RankCatalog catalog = this.catalog.get();
		int total = catalog.ranks().size();
		if (total <= 1) return "100";
		int index = catalog.progressionIndex(current.id());
		return Integer.toString(Math.round((100.0f * index) / (total - 1)));
	}
}
