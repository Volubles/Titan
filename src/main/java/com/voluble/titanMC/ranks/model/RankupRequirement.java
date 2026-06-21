package com.voluble.titanMC.ranks.model;

import java.util.Objects;
import java.util.Optional;

public record RankupRequirement(long cost, Optional<RankId> requires) {
	public RankupRequirement {
		if (cost < 0) throw new IllegalArgumentException("cost must not be negative");
		requires = Objects.requireNonNull(requires, "requires");
	}

	public static RankupRequirement free() {
		return new RankupRequirement(0L, Optional.empty());
	}

	public static RankupRequirement of(long cost) {
		return new RankupRequirement(cost, Optional.empty());
	}

	public static RankupRequirement of(long cost, RankId requires) {
		return new RankupRequirement(cost, Optional.of(Objects.requireNonNull(requires, "requires")));
	}
}
