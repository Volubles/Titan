package com.voluble.titanMC.mines;

import com.voluble.titanMC.util.RegionUtils;
import org.bukkit.Location;

import java.util.Objects;

public final class Mine {

	private final String name;
	private RegionUtils.Cuboid cuboid;
	private int resetIntervalSeconds;
	private boolean enabled;
	private int batchSizePerTick;
	private WeightedPalette palette;
	private long nextResetEpochMs;
	// Safe spawn location to teleport players to during reset. Null means no teleport
	private Location safeSpawn;
	// Depletion-based auto reset: trigger when remaining percent <= threshold. -1 disables
	private int autoResetBelowPercent = -1;
	private int brokenBlocks = 0;

	public Mine(String name, RegionUtils.Cuboid cuboid, int resetIntervalSeconds, boolean enabled, int batchSizePerTick, WeightedPalette palette) {
		this.name = Objects.requireNonNull(name, "name");
		this.cuboid = Objects.requireNonNull(cuboid, "cuboid");
		this.resetIntervalSeconds = Math.max(1, resetIntervalSeconds);
		this.enabled = enabled;
		this.batchSizePerTick = Math.max(1, batchSizePerTick);
		this.palette = palette == null ? new WeightedPalette() : palette;
		this.nextResetEpochMs = System.currentTimeMillis() + (long) this.resetIntervalSeconds * 1000L;
	}

	public String getName() { return name; }
	public RegionUtils.Cuboid getCuboid() { return cuboid; }
	public void setCuboid(RegionUtils.Cuboid cuboid) { this.cuboid = Objects.requireNonNull(cuboid); }
	public int getResetIntervalSeconds() { return resetIntervalSeconds; }
	public void setResetIntervalSeconds(int seconds) { this.resetIntervalSeconds = Math.max(1, seconds); }
	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public int getBatchSizePerTick() { return batchSizePerTick; }
	public void setBatchSizePerTick(int batchSizePerTick) { this.batchSizePerTick = Math.max(1, batchSizePerTick); }
	public WeightedPalette getPalette() { return palette; }
	public void setPalette(WeightedPalette palette) { this.palette = Objects.requireNonNullElseGet(palette, WeightedPalette::new); }
	public long getNextResetEpochMs() { return nextResetEpochMs; }
	public void setNextResetEpochMs(long epochMs) { this.nextResetEpochMs = epochMs; }
	public void scheduleNextAfterInterval() { this.nextResetEpochMs = System.currentTimeMillis() + (long) resetIntervalSeconds * 1000L; }

	public Location getSafeSpawn() { return safeSpawn; }
	public void setSafeSpawn(Location location) { this.safeSpawn = location; }

	public int getAutoResetBelowPercent() { return autoResetBelowPercent; }
	public void setAutoResetBelowPercent(int percent) { this.autoResetBelowPercent = Math.max(-1, Math.min(100, percent)); }

	public void resetDepletionCounters() { this.brokenBlocks = 0; }
	public void incrementBroken(int amount) { if (amount > 0) this.brokenBlocks = Math.min(getTotalBlockCountSafe(), this.brokenBlocks + amount); }
	public void decrementBroken(int amount) { if (amount > 0) this.brokenBlocks = Math.max(0, this.brokenBlocks - amount); }
	public int getBrokenBlocks() { return brokenBlocks; }
	public void setBrokenBlocks(int amount) { this.brokenBlocks = Math.max(0, Math.min(getTotalBlockCountSafe(), amount)); }

	public int getTotalBlockCountSafe() {
		long dx = (long) (cuboid.maxX - cuboid.minX + 1);
		long dy = (long) (cuboid.maxY - cuboid.minY + 1);
		long dz = (long) (cuboid.maxZ - cuboid.minZ + 1);
		long vol = dx * dy * dz;
		return vol > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) vol;
	}

	public int getRemainingPercent() {
		int total = getTotalBlockCountSafe();
		if (total <= 0) return 0;
		int remaining = Math.max(0, total - brokenBlocks);
		return (int) Math.round(remaining * 100.0 / total);
	}

	public boolean shouldAutoResetByDepletion() {
		return autoResetBelowPercent >= 0 && getRemainingPercent() <= autoResetBelowPercent;
	}
}


