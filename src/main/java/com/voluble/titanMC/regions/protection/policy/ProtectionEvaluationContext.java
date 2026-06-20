package com.voluble.titanMC.regions.protection.policy;

import com.voluble.titanMC.regions.protection.model.ProtectionActor;

import java.time.Instant;
import java.util.Objects;

public record ProtectionEvaluationContext(ProtectionActor actor, Instant evaluatedAt) {

	public ProtectionEvaluationContext {
		Objects.requireNonNull(actor, "actor");
		Objects.requireNonNull(evaluatedAt, "evaluatedAt");
	}
}
