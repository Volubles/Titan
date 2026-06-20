package com.voluble.titanMC.regions.protection.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class RegionFlagSet {

	private static final RegionFlagSet EMPTY = new RegionFlagSet(Map.of());

	private final Map<ProtectionAction, ProtectionDecision> decisions;

	private RegionFlagSet(Map<ProtectionAction, ProtectionDecision> decisions) {
		this.decisions = Map.copyOf(decisions);
	}

	public static RegionFlagSet empty() {
		return EMPTY;
	}

	public static RegionFlagSet of(Map<ProtectionAction, ProtectionDecision> decisions) {
		Objects.requireNonNull(decisions, "decisions");
		EnumMap<ProtectionAction, ProtectionDecision> explicit = new EnumMap<>(ProtectionAction.class);
		decisions.forEach((action, decision) -> {
			Objects.requireNonNull(action, "flag action");
			Objects.requireNonNull(decision, "flag decision");
			if (decision.explicit()) explicit.put(action, decision);
		});
		return explicit.isEmpty() ? EMPTY : new RegionFlagSet(explicit);
	}

	public ProtectionDecision decision(ProtectionAction action) {
		return decisions.getOrDefault(Objects.requireNonNull(action, "action"), ProtectionDecision.ABSTAIN);
	}

	public Map<ProtectionAction, ProtectionDecision> explicitDecisions() {
		return decisions;
	}

	public RegionFlagSet with(ProtectionAction action, ProtectionDecision decision) {
		Objects.requireNonNull(action, "action");
		Objects.requireNonNull(decision, "decision");
		EnumMap<ProtectionAction, ProtectionDecision> updated = new EnumMap<>(ProtectionAction.class);
		updated.putAll(decisions);
		if (decision.explicit()) updated.put(action, decision);
		else updated.remove(action);
		return of(updated);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof RegionFlagSet flags && decisions.equals(flags.decisions);
	}

	@Override
	public int hashCode() {
		return decisions.hashCode();
	}

	@Override
	public String toString() {
		return decisions.toString();
	}
}
