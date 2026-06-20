package com.voluble.titanMC.regions.admin;

import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.RegionGeometry;
import com.voluble.titanMC.regions.model.RegionKey;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.service.RegionEngine;
import com.voluble.titanMC.regions.service.RegionMutationResult;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class RegionAdminService {

	public static final String NAMESPACE = "custom";
	public static final int DEFAULT_PRIORITY = 100;

	private final RegionEngine regions;

	public RegionAdminService(RegionEngine regions) {
		this.regions = Objects.requireNonNull(regions, "regions");
	}

	public RegionMutationResult create(
		String name,
		WorldId worldId,
		int priority,
		RegionGeometry geometry
	) {
		return regions.create(key(name), worldId, priority, geometry).join();
	}

	public RegionMutationResult redefine(
		String name,
		WorldId worldId,
		RegionGeometry geometry
	) {
		RegionDefinition existing = find(worldId, name);
		if (existing == null) return notFound(name);
		return regions.update(
			existing.id(), existing.revision(), existing.key(), worldId, existing.priority(), geometry
		).join();
	}

	public RegionMutationResult delete(String name, WorldId worldId) {
		RegionDefinition existing = find(worldId, name);
		if (existing == null) return notFound(name);
		return regions.delete(existing.id(), existing.revision()).join();
	}

	public RegionMutationResult setFlag(
		String name,
		WorldId worldId,
		ProtectionAction action,
		ProtectionDecision decision
	) {
		RegionDefinition existing = find(worldId, name);
		if (existing == null) return notFound(name);
		return regions.setFlags(
			existing.id(), existing.revision(), existing.flags().with(action, decision)
		).join();
	}

	public RegionMutationResult setPriority(String name, WorldId worldId, int priority) {
		RegionDefinition existing = find(worldId, name);
		if (existing == null) return notFound(name);
		return regions.update(
			existing.id(),
			existing.revision(),
			existing.key(),
			existing.worldId(),
			priority,
			existing.geometry()
		).join();
	}

	public RegionDefinition find(WorldId worldId, String name) {
		return regions.find(worldId, key(name));
	}

	public List<RegionDefinition> list(WorldId worldId) {
		return regions.snapshot().definitions().stream()
			.filter(region -> region.worldId().equals(worldId))
			.filter(region -> region.key().namespace().equals(NAMESPACE))
			.sorted(Comparator.comparing(RegionDefinition::key))
			.toList();
	}

	public List<String> names() {
		return regions.snapshot().definitions().stream()
			.filter(region -> region.key().namespace().equals(NAMESPACE))
			.map(region -> region.key().name())
			.distinct()
			.sorted()
			.toList();
	}

	private static RegionKey key(String name) {
		return RegionKey.of(NAMESPACE, name);
	}

	private static RegionMutationResult.Failure notFound(String name) {
		return new RegionMutationResult.Failure(
			RegionMutationResult.Reason.NOT_FOUND, "Unknown region: " + name
		);
	}
}
