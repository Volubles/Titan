package com.voluble.titanMC.regions.protection.model;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ProtectionActor(
	Type type,
	String identifier,
	UUID playerId,
	Set<String> permissions
) {

	public ProtectionActor {
		Objects.requireNonNull(type, "type");
		identifier = Objects.requireNonNull(identifier, "identifier").trim();
		if (identifier.isEmpty()) throw new IllegalArgumentException("identifier must not be blank");
		if (type == Type.PLAYER && playerId == null) throw new IllegalArgumentException("player actor requires playerId");
		if (type != Type.PLAYER && playerId != null) throw new IllegalArgumentException("non-player actor must not have playerId");
		Objects.requireNonNull(permissions, "permissions");
		Set<String> normalized = new LinkedHashSet<>();
		for (String permission : permissions) {
			if (permission == null || permission.isBlank()) throw new IllegalArgumentException("permissions must not contain blanks");
			normalized.add(permission.toLowerCase(Locale.ROOT));
		}
		permissions = Set.copyOf(normalized);
	}

	public static ProtectionActor player(UUID playerId, Set<String> permissions) {
		return new ProtectionActor(Type.PLAYER, playerId.toString(), playerId, permissions);
	}

	public static ProtectionActor system(String identifier, Set<String> permissions) {
		return new ProtectionActor(Type.SYSTEM, identifier, null, permissions);
	}

	public static ProtectionActor environment(String identifier) {
		return new ProtectionActor(Type.ENVIRONMENT, identifier, null, Set.of());
	}

	public boolean hasPermission(String permission) {
		String normalized = Objects.requireNonNull(permission, "permission").toLowerCase(Locale.ROOT);
		return permissions.contains("*") || permissions.contains(normalized);
	}

	public enum Type {
		PLAYER,
		SYSTEM,
		ENVIRONMENT
	}
}
