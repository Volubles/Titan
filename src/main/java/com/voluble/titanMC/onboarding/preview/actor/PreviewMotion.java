package com.voluble.titanMC.onboarding.preview.actor;

import org.bukkit.Location;

public record PreviewMotion(double blocksPerTick, int minimumTicks) {
	private static final double DEFAULT_BLOCKS_PER_TICK = 0.12;
	private static final int DEFAULT_MINIMUM_TICKS = 6;

	public PreviewMotion {
		if (blocksPerTick <= 0.0) throw new IllegalArgumentException("blocks per tick must be positive");
		if (minimumTicks < 0) throw new IllegalArgumentException("minimum ticks must not be negative");
	}

	public static PreviewMotion defaults() {
		return new PreviewMotion(DEFAULT_BLOCKS_PER_TICK, DEFAULT_MINIMUM_TICKS);
	}

	int ticksBetween(Location from, Location to) {
		double distance = from.distance(to);
		if (distance < 0.01) return 0;
		return Math.max(minimumTicks, (int) Math.ceil(distance / blocksPerTick));
	}
}
