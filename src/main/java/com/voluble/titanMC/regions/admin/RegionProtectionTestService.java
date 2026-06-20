package com.voluble.titanMC.regions.admin;

import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import com.voluble.titanMC.regions.protection.model.ProtectionResolution;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import com.voluble.titanMC.regions.service.RegionEngine;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class RegionProtectionTestService {

	private static final Comparator<RegionDefinition> REGION_ORDER = Comparator
		.comparingInt(RegionDefinition::priority).reversed()
		.thenComparing(RegionDefinition::key);

	private final RegionEngine regions;
	private final ProtectionService protection;

	public RegionProtectionTestService(RegionEngine regions, ProtectionService protection) {
		this.regions = Objects.requireNonNull(regions, "regions");
		this.protection = Objects.requireNonNull(protection, "protection");
	}

	public Result test(ProtectionActor actor, ProtectionAction action, BlockPosition position) {
		Objects.requireNonNull(actor, "actor");
		Objects.requireNonNull(action, "action");
		Objects.requireNonNull(position, "position");
		List<RegionDefinition> matching = regions.findAll(
			position.worldId(), position.x(), position.y(), position.z()
		).stream().sorted(REGION_ORDER).toList();
		ProtectionResolution resolution = protection.resolve(
			ProtectionRequest.at(actor, action, position)
		);
		return new Result(action, position, matching, resolution);
	}

	public record Result(
		ProtectionAction action,
		BlockPosition position,
		List<RegionDefinition> matchingRegions,
		ProtectionResolution resolution
	) {
		public Result {
			Objects.requireNonNull(action, "action");
			Objects.requireNonNull(position, "position");
			matchingRegions = List.copyOf(matchingRegions);
			Objects.requireNonNull(resolution, "resolution");
		}
	}
}
