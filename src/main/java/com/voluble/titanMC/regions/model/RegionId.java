package com.voluble.titanMC.regions.model;

import java.util.Objects;
import java.util.UUID;

public record RegionId(UUID value) implements Comparable<RegionId> {

	public RegionId {
		Objects.requireNonNull(value, "value");
	}

	public static RegionId random() {
		return new RegionId(UUID.randomUUID());
	}

	public static RegionId parse(String value) {
		return new RegionId(UUID.fromString(value));
	}

	@Override
	public int compareTo(RegionId other) {
		return value.compareTo(other.value);
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
