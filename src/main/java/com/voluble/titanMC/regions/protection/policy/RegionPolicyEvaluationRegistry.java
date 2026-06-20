package com.voluble.titanMC.regions.protection.policy;

import java.util.Map;

public final class RegionPolicyEvaluationRegistry {

	private final Map<String, Entry> entries;

	RegionPolicyEvaluationRegistry(Map<String, Entry> entries) {
		this.entries = Map.copyOf(entries);
	}

	public Entry find(String namespace) {
		return entries.get(namespace);
	}

	public record Entry(String policyId, RegionPolicyEvaluator evaluator) {}
}
