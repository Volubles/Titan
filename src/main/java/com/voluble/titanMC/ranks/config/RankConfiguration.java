package com.voluble.titanMC.ranks.config;

import com.voluble.titanMC.ranks.model.PrisonRank;
import com.voluble.titanMC.ranks.model.RankId;
import com.voluble.titanMC.ranks.model.RankupRequirement;
import com.voluble.titanMC.ranks.model.WardDefinition;
import com.voluble.titanMC.ranks.model.WardId;
import com.voluble.titanMC.ranks.service.RankCatalog;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record RankConfiguration(RankCatalog catalog) {
	public RankConfiguration {
		Objects.requireNonNull(catalog, "catalog");
	}

	public static RankConfiguration load(ConfigurationSection yaml) {
		Objects.requireNonNull(yaml, "yaml");
		List<Map<?, ?>> configuredWards = yaml.getMapList("wards");
		if (configuredWards.isEmpty()) throw new IllegalArgumentException("wards must contain at least one ward");

		List<WardDefinition> wards = new ArrayList<>();
		List<PrisonRank> ranks = new ArrayList<>();
		boolean firstRankSeen = false;
		for (int wardIndex = 0; wardIndex < configuredWards.size(); wardIndex++) {
			Map<?, ?> configuredWard = configuredWards.get(wardIndex);
			String path = "wards[" + wardIndex + "]";
			WardId wardId = WardId.of(requiredString(configuredWard, "id", path));
			String displayName = optionalString(
				configuredWard, "display-name", wardId.value().toUpperCase(Locale.ROOT) + " Ward"
			);
			List<RankId> wardRanks = loadRanks(configuredWard, path, wardId, ranks, firstRankSeen);
			firstRankSeen = true;
			wards.add(new WardDefinition(wardId, displayName, wardRanks));
		}
		return new RankConfiguration(new RankCatalog(wards, ranks));
	}

	private static List<RankId> loadRanks(
			Map<?, ?> configuredWard,
			String path,
			WardId wardId,
			List<PrisonRank> definitions,
			boolean firstRankSeen
	) {
		Object configuredRanks = configuredWard.get("ranks");
		if (!(configuredRanks instanceof List<?> values) || values.isEmpty()) {
			throw new IllegalArgumentException(path + ".ranks must contain at least one rank");
		}
		List<RankId> rankIds = new ArrayList<>();
		for (int rankIndex = 0; rankIndex < values.size(); rankIndex++) {
			String rankPath = path + ".ranks[" + rankIndex + "]";
			Map<?, ?> entry = asMap(values.get(rankIndex), rankPath);
			String rawId = requiredString(entry, "id", rankPath);
			RankId rankId = RankId.of(rawId);
			rankIds.add(rankId);

			boolean isStarter = !firstRankSeen && rankIndex == 0;
			PrisonRank rank = new PrisonRank(rankId, wardId, rawId.trim().toUpperCase(Locale.ROOT));
			RankupRequirement requirement = loadRequirement(entry, rankPath, isStarter);
			if (requirement != null) rank = rank.withRankup(requirement);
			definitions.add(rank);
		}
		return rankIds;
	}

	private static RankupRequirement loadRequirement(Map<?, ?> entry, String path, boolean isStarter) {
		Object costValue = entry.get("cost");
		Object requiresValue = entry.get("requires");
		if (isStarter) {
			if (costValue != null || requiresValue != null) {
				throw new IllegalArgumentException(path + " is the starter rank and cannot declare cost or requires");
			}
			return null;
		}
		if (costValue == null) {
			throw new IllegalArgumentException(path + ".cost must be set for non-starter ranks");
		}
		long cost = asLong(costValue, path + ".cost");
		if (requiresValue == null) return RankupRequirement.of(cost);
		if (!(requiresValue instanceof String requiresText) || requiresText.isBlank()) {
			throw new IllegalArgumentException(path + ".requires must be a non-blank string");
		}
		return RankupRequirement.of(cost, RankId.of(requiresText));
	}

	private static Map<?, ?> asMap(Object value, String path) {
		if (value instanceof Map<?, ?> map) return map;
		throw new IllegalArgumentException(path + " must be a mapping with an id");
	}

	private static long asLong(Object value, String path) {
		if (value instanceof Number number) return number.longValue();
		throw new IllegalArgumentException(path + " must be an integer");
	}

	private static String requiredString(Map<?, ?> values, String key, String path) {
		Object value = values.get(key);
		if (!(value instanceof String text) || text.isBlank()) {
			throw new IllegalArgumentException(path + "." + key + " must be a non-blank string");
		}
		return text;
	}

	private static String optionalString(Map<?, ?> values, String key, String fallback) {
		Object value = values.get(key);
		if (value == null) return fallback;
		if (!(value instanceof String text) || text.isBlank()) {
			throw new IllegalArgumentException(key + " must be a non-blank string");
		}
		return text;
	}
}
