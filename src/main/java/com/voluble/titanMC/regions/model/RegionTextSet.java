package com.voluble.titanMC.regions.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RegionTextSet {

	private static final RegionTextSet EMPTY = new RegionTextSet(Map.of());
	private static final int MAX_LENGTH = 512;

	private final Map<RegionTextFlag, String> values;

	private RegionTextSet(Map<RegionTextFlag, String> values) {
		this.values = Map.copyOf(values);
	}

	public static RegionTextSet empty() {
		return EMPTY;
	}

	public static RegionTextSet of(Map<RegionTextFlag, String> values) {
		Objects.requireNonNull(values, "values");
		EnumMap<RegionTextFlag, String> checked = new EnumMap<>(RegionTextFlag.class);
		values.forEach((flag, value) -> checked.put(
			Objects.requireNonNull(flag, "values must not contain null flags"),
			validate(value)
		));
		return checked.isEmpty() ? EMPTY : new RegionTextSet(checked);
	}

	public Optional<String> value(RegionTextFlag flag) {
		return Optional.ofNullable(values.get(Objects.requireNonNull(flag, "flag")));
	}

	public Map<RegionTextFlag, String> explicitValues() {
		return values;
	}

	public RegionTextSet with(RegionTextFlag flag, String value) {
		Objects.requireNonNull(flag, "flag");
		EnumMap<RegionTextFlag, String> updated = new EnumMap<>(RegionTextFlag.class);
		updated.putAll(values);
		if (value == null) updated.remove(flag);
		else updated.put(flag, validate(value));
		return updated.isEmpty() ? EMPTY : new RegionTextSet(updated);
	}

	private static String validate(String value) {
		Objects.requireNonNull(value, "text value");
		String trimmed = value.trim();
		if (trimmed.isEmpty()) throw new IllegalArgumentException("Region message must not be blank.");
		if (trimmed.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Region message must not exceed " + MAX_LENGTH + " characters.");
		}
		return trimmed;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof RegionTextSet set && values.equals(set.values);
	}

	@Override
	public int hashCode() {
		return values.hashCode();
	}
}
