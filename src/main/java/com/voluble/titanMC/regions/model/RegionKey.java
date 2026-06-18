package com.voluble.titanMC.regions.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record RegionKey(String namespace, String name) implements Comparable<RegionKey> {

	private static final Pattern PART = Pattern.compile("[a-z0-9][a-z0-9_-]{0,31}");

	public RegionKey {
		namespace = normalize(namespace, "namespace");
		name = normalize(name, "name");
	}

	public static RegionKey of(String namespace, String name) {
		return new RegionKey(namespace, name);
	}

	private static String normalize(String value, String label) {
		Objects.requireNonNull(value, label);
		String normalized = value.toLowerCase(Locale.ROOT);
		if (!PART.matcher(normalized).matches()) {
			throw new IllegalArgumentException(label + " must match " + PART.pattern());
		}
		return normalized;
	}

	@Override
	public int compareTo(RegionKey other) {
		int namespaceOrder = namespace.compareTo(other.namespace);
		return namespaceOrder != 0 ? namespaceOrder : name.compareTo(other.name);
	}

	@Override
	public String toString() {
		return namespace + ":" + name;
	}
}
