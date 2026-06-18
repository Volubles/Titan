package com.voluble.titanMC.regions.model;

import java.util.Objects;
import java.util.UUID;

public record WorldId(UUID value) {

	public WorldId {
		Objects.requireNonNull(value, "value");
	}

	public static WorldId parse(String value) {
		return new WorldId(UUID.fromString(value));
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
