package com.voluble.titanMC.regions.protection.policy;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class RegionPolicyRegistry {

	private final Map<String, RegionProtectionPolicy> byNamespace;

	private RegionPolicyRegistry(Map<String, RegionProtectionPolicy> byNamespace) {
		this.byNamespace = Map.copyOf(byNamespace);
	}

	public static Builder builder() {
		return new Builder();
	}

	public RegionProtectionPolicy find(String namespace) {
		return byNamespace.get(Objects.requireNonNull(namespace, "namespace").toLowerCase(Locale.ROOT));
	}

	public RegionPolicyEvaluationRegistry openEvaluation(ProtectionEvaluationContext context) {
		Objects.requireNonNull(context, "context");
		Map<String, RegionPolicyEvaluationRegistry.Entry> evaluations = new LinkedHashMap<>();
		for (Map.Entry<String, RegionProtectionPolicy> entry : byNamespace.entrySet()) {
			RegionProtectionPolicy policy = entry.getValue();
			RegionPolicyEvaluator evaluator = Objects.requireNonNull(
				policy.openEvaluation(context), "policy returned null evaluator: " + policy.id()
			);
			evaluations.put(entry.getKey(), new RegionPolicyEvaluationRegistry.Entry(policy.id(), evaluator));
		}
		return new RegionPolicyEvaluationRegistry(evaluations);
	}

	public static final class Builder {
		private final Map<String, RegionProtectionPolicy> policies = new LinkedHashMap<>();

		public Builder register(RegionProtectionPolicy policy) {
			Objects.requireNonNull(policy, "policy");
			String namespace = Objects.requireNonNull(policy.namespace(), "policy namespace").toLowerCase(Locale.ROOT);
			if (namespace.isBlank()) throw new IllegalArgumentException("policy namespace must not be blank");
			if (policy.id() == null || policy.id().isBlank()) throw new IllegalArgumentException("policy id must not be blank");
			if (policies.putIfAbsent(namespace, policy) != null) {
				throw new IllegalArgumentException("policy already registered for namespace: " + namespace);
			}
			return this;
		}

		public RegionPolicyRegistry build() {
			return new RegionPolicyRegistry(policies);
		}
	}
}
