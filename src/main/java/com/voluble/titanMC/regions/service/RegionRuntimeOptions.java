package com.voluble.titanMC.regions.service;

import com.voluble.titanMC.regions.index.RegionIndexOptions;

import java.time.Duration;
import java.util.Objects;

public record RegionRuntimeOptions(
	RegionIndexOptions indexOptions,
	int mutationQueueCapacity,
	Duration closeTimeout
) {

	public RegionRuntimeOptions {
		Objects.requireNonNull(indexOptions, "indexOptions");
		Objects.requireNonNull(closeTimeout, "closeTimeout");
		if (mutationQueueCapacity <= 0) throw new IllegalArgumentException("mutationQueueCapacity must be positive");
		if (closeTimeout.isNegative() || closeTimeout.isZero()) throw new IllegalArgumentException("closeTimeout must be positive");
	}

	public static RegionRuntimeOptions defaults() {
		return new RegionRuntimeOptions(RegionIndexOptions.defaults(), 1_024, Duration.ofSeconds(10));
	}
}
