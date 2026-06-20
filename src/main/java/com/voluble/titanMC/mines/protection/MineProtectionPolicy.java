package com.voluble.titanMC.mines.protection;

import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import com.voluble.titanMC.regions.protection.policy.ActionRuleSet;
import com.voluble.titanMC.regions.protection.policy.RegionProtectionPolicy;

public final class MineProtectionPolicy implements RegionProtectionPolicy {

	public static final String NAMESPACE = "mine";

	private static final ActionRuleSet RULES = ActionRuleSet.builder()
		.allow(ProtectionAction.BLOCK_BREAK)
		.deny(ProtectionAction.BLOCK_PLACE)
		.build();

	@Override
	public String id() {
		return "mine-default";
	}

	@Override
	public String namespace() {
		return NAMESPACE;
	}

	@Override
	public ProtectionDecision decide(ProtectionRequest request, RegionDefinition region) {
		return RULES.decision(request.action());
	}
}
