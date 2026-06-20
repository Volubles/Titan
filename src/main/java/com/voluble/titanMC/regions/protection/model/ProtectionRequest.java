package com.voluble.titanMC.regions.protection.model;

import com.voluble.titanMC.regions.model.BlockPosition;

import java.util.Objects;
import java.util.Optional;

public record ProtectionRequest(
	ProtectionActor actor,
	ProtectionAction action,
	BlockPosition target,
	Optional<BlockPosition> source
) {

	public ProtectionRequest {
		Objects.requireNonNull(actor, "actor");
		Objects.requireNonNull(action, "action");
		Objects.requireNonNull(target, "target");
		source = Objects.requireNonNull(source, "source");
		if (source.isPresent() && !source.get().worldId().equals(target.worldId())) {
			throw new IllegalArgumentException("source and target must be in the same world");
		}
	}

	public static ProtectionRequest at(ProtectionActor actor, ProtectionAction action, BlockPosition target) {
		return new ProtectionRequest(actor, action, target, Optional.empty());
	}

	public static ProtectionRequest moving(
		ProtectionActor actor,
		ProtectionAction action,
		BlockPosition source,
		BlockPosition target
	) {
		return new ProtectionRequest(actor, action, target, Optional.of(source));
	}
}
