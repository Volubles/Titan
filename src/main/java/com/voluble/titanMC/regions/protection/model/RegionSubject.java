package com.voluble.titanMC.regions.protection.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record RegionSubject(Type type, String value) implements Comparable<RegionSubject> {

	private static final Pattern GROUP_NAME = Pattern.compile("[a-z0-9][a-z0-9_.-]{0,63}");

	public static final RegionSubject EVERYONE = new RegionSubject(Type.EVERYONE, "");
	public static final RegionSubject OWNERS = new RegionSubject(Type.OWNERS, "");
	public static final RegionSubject MEMBERS = new RegionSubject(Type.MEMBERS, "");
	public static final RegionSubject NONOWNERS = new RegionSubject(Type.NONOWNERS, "");
	public static final RegionSubject NONMEMBERS = new RegionSubject(Type.NONMEMBERS, "");

	public RegionSubject {
		Objects.requireNonNull(type, "type");
		value = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
		if (type == Type.GROUP) {
			if (!GROUP_NAME.matcher(value).matches()) {
				throw new IllegalArgumentException("group name must match " + GROUP_NAME.pattern());
			}
		} else if (!value.isEmpty()) {
			throw new IllegalArgumentException(type.name().toLowerCase(Locale.ROOT) + " subject cannot have a value");
		}
	}

	public static RegionSubject group(String name) {
		return new RegionSubject(Type.GROUP, name);
	}

	public static RegionSubject parse(String input) {
		String normalized = Objects.requireNonNull(input, "input").trim().toLowerCase(Locale.ROOT);
		return switch (normalized) {
			case "everyone" -> EVERYONE;
			case "owners" -> OWNERS;
			case "members" -> MEMBERS;
			case "nonowners" -> NONOWNERS;
			case "nonmembers" -> NONMEMBERS;
			default -> {
				if (!normalized.startsWith("group:")) {
					throw new IllegalArgumentException(
						"Subject must be everyone, owners, members, nonowners, nonmembers, or group:<name>."
					);
				}
				yield group(normalized.substring("group:".length()));
			}
		};
	}

	public int specificity() {
		return switch (type) {
			case OWNERS -> 500;
			case MEMBERS -> 400;
			case GROUP -> 300;
			case NONOWNERS, NONMEMBERS -> 200;
			case EVERYONE -> 100;
		};
	}

	public String externalName() {
		return type == Type.GROUP ? "group:" + value : type.name().toLowerCase(Locale.ROOT);
	}

	@Override
	public int compareTo(RegionSubject other) {
		int specificityOrder = Integer.compare(other.specificity(), specificity());
		if (specificityOrder != 0) return specificityOrder;
		int typeOrder = type.compareTo(other.type);
		return typeOrder != 0 ? typeOrder : value.compareTo(other.value);
	}

	public enum Type {
		OWNERS,
		MEMBERS,
		GROUP,
		NONOWNERS,
		NONMEMBERS,
		EVERYONE
	}
}
