package com.voluble.titanMC.regions.protection.policy;

import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;

@FunctionalInterface
public interface ProtectionDefaults {

	ProtectionDecision decide(ProtectionRequest request);
}
