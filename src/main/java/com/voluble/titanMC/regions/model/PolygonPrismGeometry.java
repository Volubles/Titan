package com.voluble.titanMC.regions.model;

import java.util.List;
import java.util.Objects;

public record PolygonPrismGeometry(List<BlockPoint2> points, int minY, int maxYInclusive) implements RegionGeometry {

	public PolygonPrismGeometry {
		points = List.copyOf(points);
		if (points.size() < 3) throw new IllegalArgumentException("polygon must contain at least three points");
		if (points.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("polygon points must not contain null");
		}
		if (minY > maxYInclusive) throw new IllegalArgumentException("polygon minimum Y must not exceed maximum Y");
		if (maxYInclusive == Integer.MAX_VALUE) {
			throw new IllegalArgumentException("polygon maximum Y cannot be Integer.MAX_VALUE");
		}
	}

	@Override
	public BlockBox bounds() {
		int minX = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (BlockPoint2 point : points) {
			minX = Math.min(minX, point.x());
			minZ = Math.min(minZ, point.z());
			maxX = Math.max(maxX, point.x());
			maxZ = Math.max(maxZ, point.z());
		}
		if (maxX == Integer.MAX_VALUE || maxZ == Integer.MAX_VALUE) {
			throw new IllegalStateException("polygon maximum coordinate cannot be Integer.MAX_VALUE");
		}
		return new BlockBox(minX, minY, minZ, maxX + 1, maxYInclusive + 1, maxZ + 1);
	}

	@Override
	public boolean contains(int x, int y, int z) {
		if (y < minY || y > maxYInclusive) return false;
		boolean inside = false;
		BlockPoint2 previous = points.getLast();
		for (BlockPoint2 point : points) {
			if (point.x() == x && point.z() == z) return true;
			if (onSegment(previous, point, x, z)) return true;
			if ((point.z() > z) != (previous.z() > z)) {
				double crossingX = ((double) previous.x() - point.x()) * ((double) z - point.z())
					/ ((double) previous.z() - point.z()) + point.x();
				if (x < crossingX) inside = !inside;
			}
			previous = point;
		}
		return inside;
	}

	@Override
	public int complexity() {
		return points.size();
	}

	private static boolean onSegment(BlockPoint2 first, BlockPoint2 second, int x, int z) {
		long cross = (long) (x - first.x()) * (second.z() - first.z())
			- (long) (z - first.z()) * (second.x() - first.x());
		return cross == 0L
			&& x >= Math.min(first.x(), second.x()) && x <= Math.max(first.x(), second.x())
			&& z >= Math.min(first.z(), second.z()) && z <= Math.max(first.z(), second.z());
	}
}
