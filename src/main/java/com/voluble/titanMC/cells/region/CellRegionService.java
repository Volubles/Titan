package com.voluble.titanMC.cells.region;

import com.voluble.titanMC.cells.model.CellDefinition;
import com.voluble.titanMC.regions.model.BlockBox;
import com.voluble.titanMC.regions.model.CuboidGeometry;
import com.voluble.titanMC.regions.model.RegionAccessSet;
import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.RegionKey;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.service.RegionEngine;
import com.voluble.titanMC.regions.service.RegionBatchResult;
import com.voluble.titanMC.regions.service.RegionMutationBatch;
import com.voluble.titanMC.regions.service.RegionMutationResult;
import com.voluble.titanMC.util.RegionUtils;

import java.util.Objects;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CellRegionService {
	public static final int PRIORITY = 200;
	private final RegionEngine regions;

	public CellRegionService(RegionEngine regions) {
		this.regions = Objects.requireNonNull(regions, "regions");
	}

	private static RegionKey key(String id) {
		return RegionKey.of(CellProtectionPolicy.NAMESPACE, id);
	}

	private static WorldId world(CellDefinition cell) {
		return new WorldId(cell.cuboid().worldId);
	}

	private static CuboidGeometry geometry(RegionUtils.Cuboid c) {
		return new CuboidGeometry(BlockBox.inclusive(c.minX, c.minY, c.minZ, c.maxX, c.maxY, c.maxZ));
	}

	private static void requireSuccess(RegionMutationResult result) {
		if (result instanceof RegionMutationResult.Failure f)
			throw new IllegalStateException("Cell region operation failed: " + f.message());
	}

	public boolean hasManagedRegions() {
		return regions.snapshot().definitions().stream()
			.anyMatch(region -> region.key().namespace().equals(CellProtectionPolicy.NAMESPACE));
	}

	public void create(CellDefinition cell) {
		Objects.requireNonNull(cell, "cell");
		requireSuccess(regions.create(
			key(cell.id()), world(cell), PRIORITY, geometry(cell.cuboid())
		).join());
	}

	public void reconcile(Collection<CellDefinition> cells, Map<String, RegionAccessSet> accessByCell) {
		Objects.requireNonNull(cells, "cells");
		Objects.requireNonNull(accessByCell, "accessByCell");
		Map<String, CellDefinition> desired = desiredCells(cells);
		Map<String, List<RegionDefinition>> existing = existingCellRegions();
		List<RegionMutationBatch.Operation> operations = new ArrayList<>();

		for (var entry : desired.entrySet()) {
			CellDefinition cell = entry.getValue();
			List<RegionDefinition> candidates = existing.remove(entry.getKey());
			RegionDefinition retained = selectRetained(candidates, cell);
			if (candidates != null) {
				for (RegionDefinition candidate : candidates) {
					if (candidate != retained) {
						operations.add(new RegionMutationBatch.Delete(candidate.id(), candidate.revision()));
					}
				}
			}
			if (retained == null) {
				operations.add(new RegionMutationBatch.Create(RegionDefinition.create(
					key(cell.id()), world(cell), PRIORITY, geometry(cell.cuboid())
				)));
			} else if (!matches(retained, cell)) {
				operations.add(new RegionMutationBatch.Update(
					retained.id(), retained.revision(), retained.key(), world(cell), PRIORITY, geometry(cell.cuboid())
				));
			}
		}

		for (List<RegionDefinition> orphaned : existing.values()) {
			for (RegionDefinition orphan : orphaned) {
				operations.add(new RegionMutationBatch.Delete(orphan.id(), orphan.revision()));
			}
		}
		if (!operations.isEmpty()) requireBatchSuccess(regions.submit(new RegionMutationBatch(operations)).join());

		for (CellDefinition cell : desired.values()) {
			setAccess(cell, accessByCell.getOrDefault(cell.id(), RegionAccessSet.empty()));
		}
	}

	private static Map<String, CellDefinition> desiredCells(Collection<CellDefinition> cells) {
		Map<String, CellDefinition> desired = new LinkedHashMap<>();
		for (CellDefinition cell : cells) {
			Objects.requireNonNull(cell, "cells must not contain null");
			if (desired.putIfAbsent(cell.id(), cell) != null) {
				throw new CellRegionException("Duplicate cell id in region projection: " + cell.id());
			}
		}
		return desired;
	}

	private Map<String, List<RegionDefinition>> existingCellRegions() {
		Map<String, List<RegionDefinition>> existing = new LinkedHashMap<>();
		for (RegionDefinition region : regions.snapshot().definitions()) {
			if (!region.key().namespace().equals(CellProtectionPolicy.NAMESPACE)) continue;
			existing.computeIfAbsent(region.key().name(), ignored -> new ArrayList<>()).add(region);
		}
		return existing;
	}

	private static RegionDefinition selectRetained(List<RegionDefinition> candidates, CellDefinition cell) {
		if (candidates == null || candidates.isEmpty()) return null;
		return candidates.stream()
			.filter(candidate -> candidate.worldId().equals(world(cell)))
			.findFirst()
			.orElse(candidates.getFirst());
	}

	private static boolean matches(RegionDefinition region, CellDefinition cell) {
		return region.worldId().equals(world(cell))
			&& region.priority() == PRIORITY
			&& region.geometry().equals(geometry(cell.cuboid()));
	}

	private static void requireBatchSuccess(RegionBatchResult result) {
		if (result instanceof RegionBatchResult.Failure failure) {
			throw new CellRegionException(
				"Cell region reconciliation failed: " + failure.reason() + " (" + failure.message() + ")"
			);
		}
	}

	public void delete(CellDefinition cell) {
		RegionDefinition existing = find(cell);
		if (existing != null) requireSuccess(regions.delete(existing.id(), existing.revision()).join());
	}

	public void setAccess(CellDefinition cell, RegionAccessSet access) {
		RegionDefinition existing = Objects.requireNonNull(find(cell), "Missing cell region: " + cell.id());
		if (existing.access().equals(access)) return;
		requireSuccess(regions.setAccess(existing.id(), existing.revision(), access).join());
	}

	public RegionDefinition find(CellDefinition cell) {
		return regions.find(world(cell), key(cell.id()));
	}
}
