package com.voluble.titanMC.regions.model;

public record BlockBox(
	int minX,
	int minY,
	int minZ,
	int maxXExclusive,
	int maxYExclusive,
	int maxZExclusive
) {

	public BlockBox {
		if (minX >= maxXExclusive || minY >= maxYExclusive || minZ >= maxZExclusive) {
			throw new IllegalArgumentException("box must have positive volume on every axis");
		}
	}

	public static BlockBox inclusive(int x1, int y1, int z1, int x2, int y2, int z2) {
		int minX = Math.min(x1, x2);
		int minY = Math.min(y1, y2);
		int minZ = Math.min(z1, z2);
		int maxX = Math.max(x1, x2);
		int maxY = Math.max(y1, y2);
		int maxZ = Math.max(z1, z2);
		if (maxX == Integer.MAX_VALUE || maxY == Integer.MAX_VALUE || maxZ == Integer.MAX_VALUE) {
			throw new IllegalArgumentException("inclusive maximum cannot be Integer.MAX_VALUE");
		}
		return new BlockBox(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
	}

	public boolean contains(int x, int y, int z) {
		return x >= minX && x < maxXExclusive
			&& y >= minY && y < maxYExclusive
			&& z >= minZ && z < maxZExclusive;
	}

	public boolean intersects(BlockBox other) {
		return minX < other.maxXExclusive && maxXExclusive > other.minX
			&& minY < other.maxYExclusive && maxYExclusive > other.minY
			&& minZ < other.maxZExclusive && maxZExclusive > other.minZ;
	}

	public long volume() {
		long x = (long) maxXExclusive - minX;
		long y = (long) maxYExclusive - minY;
		long z = (long) maxZExclusive - minZ;
		return Math.multiplyExact(Math.multiplyExact(x, y), z);
	}

	public int minChunkX() {
		return minX >> 4;
	}

	public int maxChunkX() {
		return (maxXExclusive - 1) >> 4;
	}

	public int minChunkZ() {
		return minZ >> 4;
	}

	public int maxChunkZ() {
		return (maxZExclusive - 1) >> 4;
	}
}
