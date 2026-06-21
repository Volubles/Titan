package com.voluble.titanMC.util;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

/**
 * Region utilities optimized for cuboid containment checks and fast lookups.
 *
 * Usage options:
 * - Simple one-off check: RegionUtils.isInsideCuboid(loc, a, b)
 * - High-performance many-regions lookups: use RegionIndex with Cuboid
 */
public final class RegionUtils {

	private RegionUtils() {}

	/**
	 * Fast inclusive cuboid containment check using block coordinates.
	 * Corners can be in any order; worlds must match.
	 */
	public static boolean isInsideCuboid(Location location, Location cornerA, Location cornerB) {
		if (location == null || cornerA == null || cornerB == null) return false;
		World locationWorld = location.getWorld();
		World worldA = cornerA.getWorld();
		World worldB = cornerB.getWorld();
		if (locationWorld == null || worldA == null || worldB == null) return false;
		UUID worldId = locationWorld.getUID();
		// Require both corners be in the same world as the location
		if (!worldA.getUID().equals(worldId) || !worldB.getUID().equals(worldId)) return false;

		int lx = location.getBlockX();
		int ly = location.getBlockY();
		int lz = location.getBlockZ();

		int minX = Math.min(cornerA.getBlockX(), cornerB.getBlockX());
		int maxX = Math.max(cornerA.getBlockX(), cornerB.getBlockX());
		int minY = Math.min(cornerA.getBlockY(), cornerB.getBlockY());
		int maxY = Math.max(cornerA.getBlockY(), cornerB.getBlockY());
		int minZ = Math.min(cornerA.getBlockZ(), cornerB.getBlockZ());
		int maxZ = Math.max(cornerA.getBlockZ(), cornerB.getBlockZ());

		return lx >= minX && lx <= maxX
			&& ly >= minY && ly <= maxY
			&& lz >= minZ && lz <= maxZ;
	}

	/**
	 * Immutable inclusive cuboid defined in a specific world by UUID.
	 */
	public static final class Cuboid {
		public final UUID worldId;
		public final int minX;
		public final int minY;
		public final int minZ;
		public final int maxX;
		public final int maxY;
		public final int maxZ;

		public Cuboid(UUID worldId, int x1, int y1, int z1, int x2, int y2, int z2) {
			if (worldId == null) throw new IllegalArgumentException("worldId cannot be null");
			this.worldId = worldId;
			this.minX = Math.min(x1, x2);
			this.minY = Math.min(y1, y2);
			this.minZ = Math.min(z1, z2);
			this.maxX = Math.max(x1, x2);
			this.maxY = Math.max(y1, y2);
			this.maxZ = Math.max(z1, z2);
		}

		public static Cuboid fromCorners(Location a, Location b) {
			if (a == null || b == null) throw new IllegalArgumentException("Corners cannot be null");
			World wa = a.getWorld();
			World wb = b.getWorld();
			if (wa == null || wb == null) throw new IllegalArgumentException("Corner worlds cannot be null");
			if (!wa.getUID().equals(wb.getUID())) throw new IllegalArgumentException("Corners must be in the same world");
			return new Cuboid(
					wa.getUID(),
					a.getBlockX(), a.getBlockY(), a.getBlockZ(),
					b.getBlockX(), b.getBlockY(), b.getBlockZ()
			);
		}

	public boolean contains(Location location) {
		if (location == null) return false;
		World lw = location.getWorld();
		if (lw == null || !lw.getUID().equals(worldId)) return false;
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		return x >= minX && x <= maxX
				&& y >= minY && y <= maxY
				&& z >= minZ && z <= maxZ;
	}

	public boolean intersects(Cuboid other) {
		if (other == null || !this.worldId.equals(other.worldId)) return false;
		return this.maxX >= other.minX && this.minX <= other.maxX
				&& this.maxY >= other.minY && this.minY <= other.maxY
				&& this.maxZ >= other.minZ && this.minZ <= other.maxZ;
	}

	/**
	 * Calculates the minimum distance from a location to this cuboid.
	 * Returns 0 if the location is inside the cuboid.
	 *
	 * @param location The location to measure from
	 * @return The distance in blocks, or Double.MAX_VALUE if worlds don't match
	 */
	public double distanceTo(Location location) {
		double squared = distanceSquaredTo(location);
		return squared == Double.MAX_VALUE ? squared : Math.sqrt(squared);
	}

	public double distanceSquaredTo(Location location) {
		if (location == null) return Double.MAX_VALUE;
		World lw = location.getWorld();
		if (lw == null || !lw.getUID().equals(worldId)) return Double.MAX_VALUE;

		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();

		// If inside, distance is 0
		if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
			return 0;
		}

		// Calculate closest point on cuboid
		int closestX = Math.max(minX, Math.min(maxX, x));
		int closestY = Math.max(minY, Math.min(maxY, y));
		int closestZ = Math.max(minZ, Math.min(maxZ, z));

		// Calculate distance
		double dx = x - closestX;
		double dy = y - closestY;
		double dz = z - closestZ;

		return dx * dx + dy * dy + dz * dz;
	}

		public int minChunkX() { return minX >> 4; }
		public int maxChunkX() { return maxX >> 4; }
		public int minChunkZ() { return minZ >> 4; }
		public int maxChunkZ() { return maxZ >> 4; }

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Cuboid cuboid = (Cuboid) o;
			return minX == cuboid.minX && minY == cuboid.minY && minZ == cuboid.minZ
					&& maxX == cuboid.maxX && maxY == cuboid.maxY && maxZ == cuboid.maxZ
					&& worldId.equals(cuboid.worldId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(worldId, minX, minY, minZ, maxX, maxY, maxZ);
		}

		@Override
		public String toString() {
			return "Cuboid{" +
					"worldId=" + worldId +
					", minX=" + minX +
					", minY=" + minY +
					", minZ=" + minZ +
					", maxX=" + maxX +
					", maxY=" + maxY +
					", maxZ=" + maxZ +
					'}';
		}
	}

	/**
	 * Chunk-indexed region lookup for O(1) candidate filtering.
	 * Not thread-safe; use from the Bukkit main thread or synchronize externally.
	 */
	public static final class RegionIndex {
		private final Map<UUID, Map<Long, List<Cuboid>>> worldChunkToRegions = new HashMap<>();

		public void clear() {
			worldChunkToRegions.clear();
		}

		public void add(Cuboid cuboid) {
			if (cuboid == null) return;
			Map<Long, List<Cuboid>> chunkMap = worldChunkToRegions.computeIfAbsent(cuboid.worldId, id -> new HashMap<>());
			for (int cx = cuboid.minChunkX(); cx <= cuboid.maxChunkX(); cx++) {
				for (int cz = cuboid.minChunkZ(); cz <= cuboid.maxChunkZ(); cz++) {
					long key = chunkKey(cx, cz);
					List<Cuboid> list = chunkMap.computeIfAbsent(key, k -> new ArrayList<>());
					if (!list.contains(cuboid)) list.add(cuboid);
				}
			}
		}

		public void remove(Cuboid cuboid) {
			if (cuboid == null) return;
			Map<Long, List<Cuboid>> chunkMap = worldChunkToRegions.get(cuboid.worldId);
			if (chunkMap == null) return;
			for (int cx = cuboid.minChunkX(); cx <= cuboid.maxChunkX(); cx++) {
				for (int cz = cuboid.minChunkZ(); cz <= cuboid.maxChunkZ(); cz++) {
					long key = chunkKey(cx, cz);
					List<Cuboid> list = chunkMap.get(key);
					if (list != null) {
						list.remove(cuboid);
						if (list.isEmpty()) chunkMap.remove(key);
					}
				}
			}
			if (chunkMap.isEmpty()) worldChunkToRegions.remove(cuboid.worldId);
		}

		public List<Cuboid> getAllAt(Location location) {
			if (location == null) return Collections.emptyList();
			World world = location.getWorld();
			if (world == null) return Collections.emptyList();
			Map<Long, List<Cuboid>> chunkMap = worldChunkToRegions.get(world.getUID());
			if (chunkMap == null) return Collections.emptyList();
			int chunkX = location.getBlockX() >> 4;
			int chunkZ = location.getBlockZ() >> 4;
			List<Cuboid> candidates = chunkMap.get(chunkKey(chunkX, chunkZ));
			if (candidates == null || candidates.isEmpty()) return Collections.emptyList();
			List<Cuboid> result = new ArrayList<>();
			for (Cuboid cuboid : candidates) {
				if (cuboid.contains(location)) result.add(cuboid);
			}
			return result;
		}

		public boolean containsAny(Location location) {
			return getFirstAt(location) != null;
		}

		public Cuboid getFirstAt(Location location) {
			if (location == null) return null;
			World world = location.getWorld();
			if (world == null) return null;
			Map<Long, List<Cuboid>> chunkMap = worldChunkToRegions.get(world.getUID());
			if (chunkMap == null) return null;
			int chunkX = location.getBlockX() >> 4;
			int chunkZ = location.getBlockZ() >> 4;
			List<Cuboid> candidates = chunkMap.get(chunkKey(chunkX, chunkZ));
			if (candidates == null) return null;
			for (Cuboid cuboid : candidates) {
				if (cuboid.contains(location)) return cuboid;
			}
			return null;
		}
	}

	private static long chunkKey(int chunkX, int chunkZ) {
		return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
	}
}


