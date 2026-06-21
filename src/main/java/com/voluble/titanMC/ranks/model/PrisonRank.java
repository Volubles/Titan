package com.voluble.titanMC.ranks.model;

import java.util.Objects;
import java.util.Optional;

public record PrisonRank(RankId id, WardId wardId, String displayName, Optional<RankupRequirement> rankup) {
	public PrisonRank {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(wardId, "wardId");
		displayName = requireDisplayName(displayName, "rank display name");
		rankup = Objects.requireNonNull(rankup, "rankup");
	}

	public PrisonRank(RankId id, WardId wardId, String displayName) {
		this(id, wardId, displayName, Optional.empty());
	}

	public PrisonRank withRankup(RankupRequirement requirement) {
		return new PrisonRank(id, wardId, displayName, Optional.of(Objects.requireNonNull(requirement, "requirement")));
	}

	private static String requireDisplayName(String value, String name) {
		String normalized = Objects.requireNonNull(value, name).trim();
		if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
		return normalized;
	}
}
