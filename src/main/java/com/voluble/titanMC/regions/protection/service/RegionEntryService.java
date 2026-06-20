package com.voluble.titanMC.regions.protection.service;

import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.RegionId;
import com.voluble.titanMC.regions.model.RegionTextFlag;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import com.voluble.titanMC.regions.protection.policy.ProtectionBypass;
import com.voluble.titanMC.regions.service.RegionEngine;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class RegionEntryService {

	private static final Comparator<RegionDefinition> REGION_ORDER = Comparator
		.comparingInt(RegionDefinition::priority).reversed()
		.thenComparing(RegionDefinition::key)
		.thenComparing(RegionDefinition::id);

	private final RegionEngine regions;
	private final ProtectionBypass bypass;

	public RegionEntryService(RegionEngine regions, ProtectionBypass bypass) {
		this.regions = Objects.requireNonNull(regions, "regions");
		this.bypass = Objects.requireNonNull(bypass, "bypass");
	}

	public Transition evaluate(ProtectionActor actor, BlockPosition from, BlockPosition to) {
		Objects.requireNonNull(actor, "actor");
		Objects.requireNonNull(from, "from");
		Objects.requireNonNull(to, "to");
		List<RegionDefinition> source = matching(from);
		List<RegionDefinition> target = matching(to);
		Set<RegionId> sourceIds = ids(source);
		Set<RegionId> targetIds = ids(target);
		List<RegionDefinition> entered = target.stream()
			.filter(region -> !sourceIds.contains(region.id()))
			.toList();
		List<RegionDefinition> exited = source.stream()
			.filter(region -> !targetIds.contains(region.id()))
			.toList();
		if (entered.isEmpty()) {
			return new Transition(
				ProtectionDecision.ALLOW, Reason.DEFAULT_ALLOW, entered, exited, List.of(),
				Optional.empty(), message(exited, RegionTextFlag.EXIT_MESSAGE), Optional.empty()
			);
		}

		ProtectionRequest request = ProtectionRequest.at(actor, ProtectionAction.ENTRY, to);
		try {
			if (bypass.bypasses(request)) {
				return new Transition(
					ProtectionDecision.ALLOW, Reason.BYPASS, entered, exited, List.of(),
					message(entered, RegionTextFlag.ENTRY_MESSAGE),
					message(exited, RegionTextFlag.EXIT_MESSAGE),
					Optional.empty()
				);
			}
		} catch (RuntimeException exception) {
			return new Transition(
				ProtectionDecision.DENY, Reason.ERROR, entered, exited, List.of(),
				Optional.empty(), Optional.empty(), Optional.of("Region entry bypass check failed.")
			);
		}

		List<RegionDecision> decisions = entered.stream()
			.map(region -> new RegionDecision(region, region.flags().decision(ProtectionAction.ENTRY)))
			.filter(decision -> decision.decision().explicit())
			.toList();
		for (int start = 0; start < decisions.size();) {
			int priority = decisions.get(start).region().priority();
			int end = start + 1;
			while (end < decisions.size() && decisions.get(end).region().priority() == priority) end++;
			List<RegionDecision> level = decisions.subList(start, end);
			ProtectionDecision decision = level.stream().anyMatch(value -> value.decision() == ProtectionDecision.DENY)
				? ProtectionDecision.DENY
				: ProtectionDecision.ALLOW;
			Optional<String> deniedMessage = decision == ProtectionDecision.DENY
				? level.stream()
					.filter(value -> value.decision() == ProtectionDecision.DENY)
					.map(RegionDecision::region)
					.map(region -> region.text().value(RegionTextFlag.ENTRY_DENY_MESSAGE))
					.flatMap(Optional::stream)
					.findFirst()
				: Optional.empty();
			return new Transition(
				decision, Reason.REGION_FLAG, entered, exited, List.copyOf(level),
				decision == ProtectionDecision.ALLOW
					? message(entered, RegionTextFlag.ENTRY_MESSAGE)
					: Optional.empty(),
				decision == ProtectionDecision.ALLOW
					? message(exited, RegionTextFlag.EXIT_MESSAGE)
					: Optional.empty(),
				deniedMessage
			);
		}
		return new Transition(
			ProtectionDecision.ALLOW, Reason.DEFAULT_ALLOW, entered, exited, List.of(),
			message(entered, RegionTextFlag.ENTRY_MESSAGE),
			message(exited, RegionTextFlag.EXIT_MESSAGE),
			Optional.empty()
		);
	}

	private List<RegionDefinition> matching(BlockPosition position) {
		return regions.findAll(
			position.worldId(), position.x(), position.y(), position.z()
		).stream().sorted(REGION_ORDER).toList();
	}

	private static Set<RegionId> ids(List<RegionDefinition> regions) {
		Set<RegionId> ids = new HashSet<>();
		for (RegionDefinition region : regions) ids.add(region.id());
		return ids;
	}

	private static Optional<String> message(List<RegionDefinition> regions, RegionTextFlag flag) {
		return regions.stream()
			.map(region -> region.text().value(flag))
			.flatMap(Optional::stream)
			.findFirst();
	}

	public enum Reason {
		BYPASS,
		REGION_FLAG,
		DEFAULT_ALLOW,
		ERROR
	}

	public record RegionDecision(RegionDefinition region, ProtectionDecision decision) {
		public RegionDecision {
			Objects.requireNonNull(region, "region");
			Objects.requireNonNull(decision, "decision");
		}
	}

	public record Transition(
		ProtectionDecision decision,
		Reason reason,
		List<RegionDefinition> entered,
		List<RegionDefinition> exited,
		List<RegionDecision> decidingRegions,
		Optional<String> entryMessage,
		Optional<String> exitMessage,
		Optional<String> denyMessage
	) {
		public Transition {
			Objects.requireNonNull(decision, "decision");
			if (!decision.explicit()) throw new IllegalArgumentException("entry decision must be explicit");
			Objects.requireNonNull(reason, "reason");
			entered = List.copyOf(entered);
			exited = List.copyOf(exited);
			decidingRegions = List.copyOf(decidingRegions);
			entryMessage = Objects.requireNonNull(entryMessage, "entryMessage");
			exitMessage = Objects.requireNonNull(exitMessage, "exitMessage");
			denyMessage = Objects.requireNonNull(denyMessage, "denyMessage");
		}

		public boolean allowed() {
			return decision == ProtectionDecision.ALLOW;
		}
	}
}
