package com.voluble.titanMC.regions.protection.model;

import java.util.Objects;
import java.util.Optional;

public record TransitionResolution(
	TransitionRule rule,
	ProtectionDecision decision,
	Optional<ProtectionResolution> source,
	Optional<ProtectionResolution> target
) {

	public TransitionResolution {
		Objects.requireNonNull(rule, "rule");
		Objects.requireNonNull(decision, "decision");
		if (!decision.explicit()) throw new IllegalArgumentException("transition decision must be explicit");
		source = Objects.requireNonNull(source, "source");
		target = Objects.requireNonNull(target, "target");
	}

	public boolean allowed() {
		return decision == ProtectionDecision.ALLOW;
	}
}
