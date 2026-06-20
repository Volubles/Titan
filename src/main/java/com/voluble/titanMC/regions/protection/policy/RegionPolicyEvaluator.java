package com.voluble.titanMC.regions.protection.policy;

import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;

@FunctionalInterface
public interface RegionPolicyEvaluator {

	ProtectionDecision decide(ProtectionRequest request, RegionDefinition region);
}
