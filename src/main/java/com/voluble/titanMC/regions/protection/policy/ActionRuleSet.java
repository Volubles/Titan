package com.voluble.titanMC.regions.protection.policy;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class ActionRuleSet {

	private static final ActionRuleSet EMPTY = new ActionRuleSet(Map.of());

	private final Map<ProtectionAction, ProtectionDecision> rules;

	private ActionRuleSet(Map<ProtectionAction, ProtectionDecision> rules) {
		this.rules = Map.copyOf(rules);
	}

	public static ActionRuleSet empty() {
		return EMPTY;
	}

	public static Builder builder() {
		return new Builder();
	}

	public ProtectionDecision decision(ProtectionAction action) {
		return rules.getOrDefault(Objects.requireNonNull(action, "action"), ProtectionDecision.ABSTAIN);
	}

	public Map<ProtectionAction, ProtectionDecision> explicitRules() {
		return rules;
	}

	public static final class Builder {
		private final EnumMap<ProtectionAction, ProtectionDecision> rules = new EnumMap<>(ProtectionAction.class);

		public Builder rule(ProtectionAction action, ProtectionDecision decision) {
			Objects.requireNonNull(action, "action");
			Objects.requireNonNull(decision, "decision");
			if (decision == ProtectionDecision.ABSTAIN) rules.remove(action);
			else rules.put(action, decision);
			return this;
		}

		public Builder allow(ProtectionAction... actions) {
			return rules(ProtectionDecision.ALLOW, actions);
		}

		public Builder deny(ProtectionAction... actions) {
			return rules(ProtectionDecision.DENY, actions);
		}

		private Builder rules(ProtectionDecision decision, ProtectionAction... actions) {
			Objects.requireNonNull(actions, "actions");
			for (ProtectionAction action : actions) rule(action, decision);
			return this;
		}

		public ActionRuleSet build() {
			return rules.isEmpty() ? EMPTY : new ActionRuleSet(rules);
		}
	}
}
