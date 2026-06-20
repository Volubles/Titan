package com.voluble.titanMC.regions.protection.policy;

import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;

public interface RegionProtectionPolicy {

	String id();

	String namespace();

	ProtectionDecision decide(ProtectionRequest request, RegionDefinition region);

	default RegionPolicyEvaluator openEvaluation(ProtectionEvaluationContext context) {
		return this::decide;
	}
}
