package com.voluble.titanMC.ranks.config;

import com.voluble.titanMC.ranks.model.PrisonRank;
import com.voluble.titanMC.ranks.model.RankId;
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
		for (int wardIndex = 0; wardIndex < configuredWards.size(); wardIndex++) {
			Map<?, ?> configuredWard = configuredWards.get(wardIndex);
			String path = "wards[" + wardIndex + "]";
			WardId wardId = WardId.of(requiredString(configuredWard, "id", path));
			String displayName = optionalString(configuredWard, "display-name", wardId.value().toUpperCase(Locale.ROOT) + " Ward");
			List<RankId> wardRanks = loadRanks(configuredWard, path, wardId, ranks);
			wards.add(new WardDefinition(wardId, displayName, wardRanks));
		}
		return new RankConfiguration(new RankCatalog(wards, ranks));
	}

	private static List<RankId> loadRanks(
		Map<?, ?> configuredWard,
		String path,
		WardId wardId,
		List<PrisonRank> definitions
	) {
		Object configuredRanks = configuredWard.get("ranks");
		if (!(configuredRanks instanceof List<?> values) || values.isEmpty()) {
			throw new IllegalArgumentException(path + ".ranks must contain at least one rank");
		}
		List<RankId> rankIds = new ArrayList<>();
		for (int rankIndex = 0; rankIndex < values.size(); rankIndex++) {
			Object value = values.get(rankIndex);
			if (!(value instanceof String text) || text.isBlank()) {
				throw new IllegalArgumentException(path + ".ranks[" + rankIndex + "] must be a non-blank string");
			}
			RankId rankId = RankId.of(text);
			rankIds.add(rankId);
			definitions.add(new PrisonRank(rankId, wardId, text.trim().toUpperCase(Locale.ROOT)));
		}
		return rankIds;
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
