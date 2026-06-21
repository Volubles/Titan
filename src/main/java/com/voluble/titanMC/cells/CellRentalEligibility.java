package com.voluble.titanMC.cells;

import com.voluble.titanMC.ranks.model.PrisonRank;
import com.voluble.titanMC.ranks.model.RankId;
import com.voluble.titanMC.ranks.model.WardId;
import com.voluble.titanMC.ranks.service.RankCatalog;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CellRentalEligibility {
	private final RankCatalog ranks;
	private final Map<WardId, RankId> minimumRanks;

	public CellRentalEligibility(RankCatalog ranks, Map<WardId, RankId> minimumRanks) {
		this.ranks = Objects.requireNonNull(ranks, "ranks");
		Map<WardId, RankId> validated = new LinkedHashMap<>();
		for (var entry : Objects.requireNonNull(minimumRanks, "minimumRanks").entrySet()) {
			WardId wardId = Objects.requireNonNull(entry.getKey(), "minimum rank ward");
			ranks.requireWard(wardId);
			PrisonRank rank = ranks.requireRank(Objects.requireNonNull(entry.getValue(), "minimum rank"));
			if (!rank.wardId().equals(wardId)) {
				throw new IllegalArgumentException("Minimum cell rank " + rank.id() + " does not belong to ward " + wardId);
			}
			validated.put(wardId, rank.id());
		}
		for (var ward : ranks.wards()) {
			if (!validated.containsKey(ward.id())) {
				throw new IllegalArgumentException("Missing minimum cell rank for ward " + ward.id());
			}
		}
		this.minimumRanks = Map.copyOf(validated);
	}

	public RankId requiredRank(WardId wardId) {
		RankId required = minimumRanks.get(Objects.requireNonNull(wardId, "wardId"));
		if (required == null) throw new IllegalArgumentException("Cell renting is not configured for ward " + wardId);
		return required;
	}

	public boolean allows(RankId currentRank, WardId wardId) {
		return ranks.meetsRequirement(Objects.requireNonNull(currentRank, "currentRank"), requiredRank(wardId));
	}
}
