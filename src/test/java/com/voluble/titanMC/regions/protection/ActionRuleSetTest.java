package com.voluble.titanMC.regions.protection;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.policy.ActionRuleSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActionRuleSetTest {

	@Test
	void unspecifiedActionsAbstain() {
		ActionRuleSet rules = ActionRuleSet.builder()
			.allow(ProtectionAction.BLOCK_BREAK)
			.deny(ProtectionAction.BLOCK_PLACE)
			.build();

		assertEquals(ProtectionDecision.ALLOW, rules.decision(ProtectionAction.BLOCK_BREAK));
		assertEquals(ProtectionDecision.DENY, rules.decision(ProtectionAction.BLOCK_PLACE));
		assertEquals(ProtectionDecision.ABSTAIN, rules.decision(ProtectionAction.BLOCK_INTERACT));
	}

	@Test
	void abstainRemovesAnExplicitRule() {
		ActionRuleSet rules = ActionRuleSet.builder()
			.allow(ProtectionAction.BLOCK_BREAK)
			.rule(ProtectionAction.BLOCK_BREAK, ProtectionDecision.ABSTAIN)
			.build();

		assertEquals(ProtectionDecision.ABSTAIN, rules.decision(ProtectionAction.BLOCK_BREAK));
		assertEquals(0, rules.explicitRules().size());
	}

	@Test
	void rejectsNullActions() {
		assertThrows(NullPointerException.class, () -> ActionRuleSet.builder().allow(ProtectionAction.BLOCK_BREAK, null));
	}
}
