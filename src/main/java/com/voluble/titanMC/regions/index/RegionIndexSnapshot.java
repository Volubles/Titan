package com.voluble.titanMC.regions.index;

import com.voluble.titanMC.regions.model.BlockBox;
import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.RegionId;
import com.voluble.titanMC.regions.model.RegionKey;
import com.voluble.titanMC.regions.model.WorldId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RegionIndexSnapshot {

	private static final Comparator<RegionDefinition> QUERY_ORDER = Comparator
		.comparingInt(RegionDefinition::priority).reversed()
		.thenComparing(RegionDefinition::key)
		.thenComparing(RegionDefinition::id);

	private final long version;
	private final Map<RegionId, RegionDefinition> byId;
	private final Map<WorldScopedKey, RegionId> byKey;
	private final Map<WorldId, WorldIndex> worlds;

	private RegionIndexSnapshot(
		long version,
		Map<RegionId, RegionDefinition> byId,
		Map<WorldScopedKey, RegionId> byKey,
		Map<WorldId, WorldIndex> worlds
	) {
		this.version = version;
		this.byId = Map.copyOf(byId);
		this.byKey = Map.copyOf(byKey);
		this.worlds = Map.copyOf(worlds);
	}

	public static RegionIndexSnapshot empty() {
		return new RegionIndexSnapshot(0L, Map.of(), Map.of(), Map.of());
	}

	public static RegionIndexSnapshot build(
		long version,
		Collection<RegionDefinition> definitions,
		RegionIndexOptions options
	) throws RegionIndexBuildException {
		Objects.requireNonNull(definitions, "definitions");
		Objects.requireNonNull(options, "options");
		Map<RegionId, RegionDefinition> byId = new LinkedHashMap<>();
		Map<WorldScopedKey, RegionId> byKey = new HashMap<>();
		Map<WorldId, Map<Long, LinkedHashSet<RegionId>>> mutableWorlds = new HashMap<>();
		long totalEntries = 0L;

		for (RegionDefinition definition : definitions) {
			if (definition == null) throw new RegionIndexBuildException("definitions must not contain null");
			if (byId.putIfAbsent(definition.id(), definition) != null) {
				throw new RegionIndexBuildException("duplicate region id: " + definition.id());
			}
			WorldScopedKey scopedKey = new WorldScopedKey(definition.worldId(), definition.key());
			if (byKey.putIfAbsent(scopedKey, definition.id()) != null) {
				throw new RegionIndexBuildException("duplicate region key in world: " + definition.key());
			}
			if (definition.boxes().size() > options.maxBoxesPerRegion()) {
				throw new RegionIndexBuildException("region exceeds box limit: " + definition.key());
			}

			Set<Long> regionChunks = new LinkedHashSet<>();
			for (BlockBox box : definition.boxes()) {
				long width = (long) box.maxChunkX() - box.minChunkX() + 1L;
				long depth = (long) box.maxChunkZ() - box.minChunkZ() + 1L;
				long boxChunks;
				try {
					boxChunks = Math.multiplyExact(width, depth);
				} catch (ArithmeticException exception) {
					throw new RegionIndexBuildException("region chunk span overflow: " + definition.key());
				}
				if (boxChunks > options.maxChunksPerRegion()) {
					throw new RegionIndexBuildException("region exceeds chunk limit: " + definition.key());
				}
				for (int chunkX = box.minChunkX(); chunkX <= box.maxChunkX(); chunkX++) {
					for (int chunkZ = box.minChunkZ(); chunkZ <= box.maxChunkZ(); chunkZ++) {
						regionChunks.add(chunkKey(chunkX, chunkZ));
					}
				}
				if (regionChunks.size() > options.maxChunksPerRegion()) {
					throw new RegionIndexBuildException("region exceeds chunk limit: " + definition.key());
				}
			}
			totalEntries += regionChunks.size();
			if (totalEntries > options.maxTotalChunkEntries()) {
				throw new RegionIndexBuildException("region index exceeds total chunk-entry limit");
			}
			Map<Long, LinkedHashSet<RegionId>> chunks = mutableWorlds.computeIfAbsent(definition.worldId(), ignored -> new HashMap<>());
			for (long chunk : regionChunks) {
				chunks.computeIfAbsent(chunk, ignored -> new LinkedHashSet<>()).add(definition.id());
			}
		}

		Map<WorldId, WorldIndex> worlds = new HashMap<>();
		for (Map.Entry<WorldId, Map<Long, LinkedHashSet<RegionId>>> worldEntry : mutableWorlds.entrySet()) {
			Map<Long, List<RegionDefinition>> chunks = new HashMap<>();
			for (Map.Entry<Long, LinkedHashSet<RegionId>> chunkEntry : worldEntry.getValue().entrySet()) {
				List<RegionDefinition> candidates = chunkEntry.getValue().stream().map(byId::get).sorted(QUERY_ORDER).toList();
				chunks.put(chunkEntry.getKey(), candidates);
			}
			List<RegionDefinition> worldRegions = byId.values().stream()
				.filter(region -> region.worldId().equals(worldEntry.getKey()))
				.sorted(QUERY_ORDER)
				.toList();
			worlds.put(worldEntry.getKey(), new WorldIndex(chunks, worldRegions));
		}
		return new RegionIndexSnapshot(version, byId, byKey, worlds);
	}

	public long version() {
		return version;
	}

	public Collection<RegionDefinition> definitions() {
		return byId.values();
	}

	public RegionDefinition find(RegionId id) {
		return byId.get(id);
	}

	public RegionDefinition find(WorldId worldId, RegionKey key) {
		RegionId id = byKey.get(new WorldScopedKey(worldId, key));
		return id == null ? null : byId.get(id);
	}

	public List<RegionDefinition> findAll(WorldId worldId, int x, int y, int z) {
		WorldIndex world = worlds.get(worldId);
		if (world == null) return List.of();
		List<RegionDefinition> candidates = world.chunks.get(chunkKey(x >> 4, z >> 4));
		if (candidates == null) return List.of();
		return candidates.stream().filter(region -> region.contains(x, y, z)).toList();
	}

	public List<RegionDefinition> findIntersecting(WorldId worldId, BlockBox box) {
		WorldIndex world = worlds.get(worldId);
		if (world == null) return List.of();
		long chunkWidth = (long) box.maxChunkX() - box.minChunkX() + 1L;
		long chunkDepth = (long) box.maxChunkZ() - box.minChunkZ() + 1L;
		long queryChunks;
		try {
			queryChunks = Math.multiplyExact(chunkWidth, chunkDepth);
		} catch (ArithmeticException exception) {
			queryChunks = Long.MAX_VALUE;
		}
		if (queryChunks > Math.max(64L, (long) world.chunks.size() * 2L)) {
			return world.regions.stream().filter(region -> region.intersects(box)).toList();
		}
		Set<RegionDefinition> candidates = new LinkedHashSet<>();
		for (int chunkX = box.minChunkX(); chunkX <= box.maxChunkX(); chunkX++) {
			for (int chunkZ = box.minChunkZ(); chunkZ <= box.maxChunkZ(); chunkZ++) {
				List<RegionDefinition> chunkCandidates = world.chunks.get(chunkKey(chunkX, chunkZ));
				if (chunkCandidates != null) candidates.addAll(chunkCandidates);
			}
		}
		List<RegionDefinition> result = new ArrayList<>();
		for (RegionDefinition candidate : candidates) {
			if (candidate.intersects(box)) result.add(candidate);
		}
		result.sort(QUERY_ORDER);
		return List.copyOf(result);
	}

	private static long chunkKey(int chunkX, int chunkZ) {
		return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
	}

	private record WorldScopedKey(WorldId worldId, RegionKey key) {}

	private static final class WorldIndex {
		private final Map<Long, List<RegionDefinition>> chunks;
		private final List<RegionDefinition> regions;

		private WorldIndex(Map<Long, List<RegionDefinition>> chunks, List<RegionDefinition> regions) {
			this.chunks = Map.copyOf(chunks);
			this.regions = List.copyOf(regions);
		}
	}
}
