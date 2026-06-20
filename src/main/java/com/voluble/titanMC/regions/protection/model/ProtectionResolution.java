package com.voluble.titanMC.regions.protection.model;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

public record ProtectionResolution(
	ProtectionDecision decision,
	Reason reason,
	OptionalInt decidingPriority,
	List<RegionPolicyEvaluation> evaluations,
	String explanation
) {

	public ProtectionResolution {
		Objects.requireNonNull(decision, "decision");
		if (!decision.explicit()) throw new IllegalArgumentException("final protection resolution must be explicit");
		Objects.requireNonNull(reason, "reason");
		decidingPriority = Objects.requireNonNull(decidingPriority, "decidingPriority");
		evaluations = List.copyOf(evaluations);
		explanation = Objects.requireNonNull(explanation, "explanation");
	}

	public boolean allowed() {
		return decision == ProtectionDecision.ALLOW;
	}

	public enum Reason {
		BYPASS,
		REGION_POLICY,
		WORLD_DEFAULT,
		POLICY_ERROR,
		LOOKUP_ERROR,
		BYPASS_ERROR,
		DEFAULT_ERROR
	}
}
