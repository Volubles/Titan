package com.voluble.titanMC.regions.protection.model;

import com.voluble.titanMC.regions.model.RegionId;
import com.voluble.titanMC.regions.model.RegionKey;

import java.util.Objects;
import java.util.Optional;

public record RegionPolicyEvaluation(
	RegionId regionId,
	RegionKey regionKey,
	int priority,
	String policyId,
	ProtectionDecision decision,
	Optional<String> error
) {

	public RegionPolicyEvaluation {
		Objects.requireNonNull(regionId, "regionId");
		Objects.requireNonNull(regionKey, "regionKey");
		policyId = Objects.requireNonNull(policyId, "policyId");
		Objects.requireNonNull(decision, "decision");
		error = Objects.requireNonNull(error, "error");
	}

	public static RegionPolicyEvaluation decided(
		RegionId regionId,
		RegionKey regionKey,
		int priority,
		String policyId,
		ProtectionDecision decision
	) {
		return new RegionPolicyEvaluation(regionId, regionKey, priority, policyId, decision, Optional.empty());
	}

	public static RegionPolicyEvaluation failed(
		RegionId regionId,
		RegionKey regionKey,
		int priority,
		String policyId,
		String error
	) {
		return new RegionPolicyEvaluation(
			regionId, regionKey, priority, policyId, ProtectionDecision.DENY, Optional.ofNullable(error)
		);
	}
}
