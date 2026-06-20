package com.voluble.titanMC.regions.protection.service;

import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import com.voluble.titanMC.regions.protection.model.ProtectionResolution;
import com.voluble.titanMC.regions.protection.model.RegionPolicyEvaluation;
import com.voluble.titanMC.regions.protection.policy.ProtectionBypass;
import com.voluble.titanMC.regions.protection.policy.ProtectionDefaults;
import com.voluble.titanMC.regions.protection.policy.RegionPolicyRegistry;
import com.voluble.titanMC.regions.protection.policy.RegionProtectionPolicy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

public final class ProtectionService {

	private static final Comparator<RegionDefinition> REGION_ORDER = Comparator
		.comparingInt(RegionDefinition::priority).reversed()
		.thenComparing(RegionDefinition::key)
		.thenComparing(RegionDefinition::id);

	private final RegionLookup regions;
	private final RegionPolicyRegistry policies;
	private final ProtectionDefaults defaults;
	private final ProtectionBypass bypass;

	public ProtectionService(
		RegionLookup regions,
		RegionPolicyRegistry policies,
		ProtectionDefaults defaults,
		ProtectionBypass bypass
	) {
		this.regions = Objects.requireNonNull(regions, "regions");
		this.policies = Objects.requireNonNull(policies, "policies");
		this.defaults = Objects.requireNonNull(defaults, "defaults");
		this.bypass = Objects.requireNonNull(bypass, "bypass");
	}

	public ProtectionResolution resolve(ProtectionRequest request) {
		Objects.requireNonNull(request, "request");
		try {
			if (bypass.bypasses(request)) {
				return resolution(ProtectionDecision.ALLOW, ProtectionResolution.Reason.BYPASS, OptionalInt.empty(), List.of(), "Actor bypassed protection");
			}
		} catch (RuntimeException exception) {
			return resolution(ProtectionDecision.DENY, ProtectionResolution.Reason.BYPASS_ERROR, OptionalInt.empty(), List.of(), safeError(exception));
		}

		BlockPosition target = request.target();
		List<RegionDefinition> applicable;
		try {
			List<RegionDefinition> found = Objects.requireNonNull(
				regions.findAll(target.worldId(), target.x(), target.y(), target.z()),
				"region lookup returned null"
			);
			if (found.stream().anyMatch(Objects::isNull)) {
				throw new NullPointerException("region lookup contained null");
			}
			applicable = new ArrayList<>(found);
		} catch (RuntimeException exception) {
			return resolution(
				ProtectionDecision.DENY,
				ProtectionResolution.Reason.LOOKUP_ERROR,
				OptionalInt.empty(),
				List.of(),
				safeError(exception)
			);
		}
		applicable.sort(REGION_ORDER);
		List<RegionPolicyEvaluation> trace = new ArrayList<>(applicable.size());

		for (int start = 0; start < applicable.size();) {
			int priority = applicable.get(start).priority();
			boolean allow = false;
			boolean deny = false;
			boolean policyError = false;
			int end = start;
			while (end < applicable.size() && applicable.get(end).priority() == priority) {
				RegionDefinition region = applicable.get(end++);
				RegionProtectionPolicy policy = policies.find(region.key().namespace());
				if (policy == null) {
					trace.add(RegionPolicyEvaluation.decided(
						region.id(), region.key(), priority, "unregistered:" + region.key().namespace(), ProtectionDecision.ABSTAIN
					));
					continue;
				}
				try {
					ProtectionDecision decision = Objects.requireNonNull(policy.decide(request, region), "policy returned null");
					trace.add(RegionPolicyEvaluation.decided(region.id(), region.key(), priority, policy.id(), decision));
					allow |= decision == ProtectionDecision.ALLOW;
					deny |= decision == ProtectionDecision.DENY;
				} catch (RuntimeException exception) {
					trace.add(RegionPolicyEvaluation.failed(region.id(), region.key(), priority, policy.id(), safeError(exception)));
					deny = true;
					policyError = true;
				}
			}

			if (deny || allow) {
				ProtectionDecision decision = deny ? ProtectionDecision.DENY : ProtectionDecision.ALLOW;
				ProtectionResolution.Reason reason = policyError
					? ProtectionResolution.Reason.POLICY_ERROR
					: ProtectionResolution.Reason.REGION_POLICY;
				String explanation = policyError
					? "A region policy failed; protection denied closed"
					: "Resolved by region policies at priority " + priority;
				return resolution(decision, reason, OptionalInt.of(priority), trace, explanation);
			}
			start = end;
		}

		try {
			ProtectionDecision decision = Objects.requireNonNull(defaults.decide(request), "defaults returned null");
			if (!decision.explicit()) {
				return resolution(ProtectionDecision.DENY, ProtectionResolution.Reason.DEFAULT_ERROR, OptionalInt.empty(), trace,
					"World default abstained; protection denied closed");
			}
			return resolution(decision, ProtectionResolution.Reason.WORLD_DEFAULT, OptionalInt.empty(), trace, "Resolved by world default");
		} catch (RuntimeException exception) {
			return resolution(ProtectionDecision.DENY, ProtectionResolution.Reason.DEFAULT_ERROR, OptionalInt.empty(), trace, safeError(exception));
		}
	}

	public boolean allowed(ProtectionRequest request) {
		return resolve(request).allowed();
	}

	private static ProtectionResolution resolution(
		ProtectionDecision decision,
		ProtectionResolution.Reason reason,
		OptionalInt priority,
		List<RegionPolicyEvaluation> evaluations,
		String explanation
	) {
		return new ProtectionResolution(decision, reason, priority, evaluations, explanation);
	}

	private static String safeError(RuntimeException exception) {
		String message = exception.getMessage();
		return exception.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
	}
}
