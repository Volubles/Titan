package com.voluble.titanMC.mines.reset;

import com.voluble.titanMC.mines.Mine;
import com.voluble.titanMC.mines.WeightedPalette;
import com.voluble.titanMC.util.RegionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

/**
 * Performs a batched reset of a mine's cuboid area.
 * Invoke processBatch() each tick until it returns true (finished).
 */
public final class MineResetRunner implements MineResetTask {

	private final Plugin plugin;
	private final Mine mine;
	private final RegionUtils.Cuboid cuboid;
	private final WeightedPalette palette;

	private int x;
	private int y;
	private int z;
	private boolean initialized;
	private boolean finished;
	private volatile boolean cancelled;

	public MineResetRunner(Plugin plugin, Mine mine) {
		this.plugin = plugin;
		this.mine = mine;
		this.cuboid = mine.getCuboid();
		this.palette = mine.getPalette();
		this.initialized = false;
		this.finished = false;
		this.cancelled = false;
	}

	public boolean isFinished() { return finished; }

	@Override
	public String name() {
		return mine.getName();
	}

	@Override
	public int maxBlocksPerSlice() {
		return mine.getBatchSizePerTick();
	}

	@Override
	public void cancel() { this.cancelled = true; }

	@Override
	public MineResetWork process(int maxBlocks, long deadlineNanos) {
		if (maxBlocks <= 0) throw new IllegalArgumentException("maxBlocks must be positive");
		if (finished || cancelled) return new MineResetWork(0, 0, true);
		World world = Bukkit.getWorld(cuboid.worldId);
		if (world == null) {
			// World not available; abort this cycle as finished to avoid blocking scheduler
			finished = true;
			return new MineResetWork(0, 0, true);
		}
		if (!initialized) {
			teleportPlayersOut();
			clearDroppedItems();
			x = cuboid.minX;
			y = cuboid.minY;
			z = cuboid.minZ;
			initialized = true;
		}
		int scanned = 0;
		int changed = 0;
		while (scanned < maxBlocks && !finished && !cancelled
				&& (scanned == 0 || System.nanoTime() < deadlineNanos)) {
			Block block = world.getBlockAt(x, y, z);
			Material m = palette.pickRandomThreadLocal();
			if (block.getType() != m) {
				block.setType(m, false);
				changed++;
			}
			scanned++;
			advanceCursor();
		}
		return new MineResetWork(scanned, changed, finished || cancelled);
	}

	private void advanceCursor() {
		if (z < cuboid.maxZ) { z++; return; }
		z = cuboid.minZ;
		if (y < cuboid.maxY) { y++; return; }
		y = cuboid.minY;
		if (x < cuboid.maxX) { x++; return; }
		finished = true;
	}

	private void teleportPlayersOut() {
		Location safeSpawn = mine.getSafeSpawn();
		if (safeSpawn == null) return;
		World world = Bukkit.getWorld(cuboid.worldId);
		if (world == null) return;
		Collection<? extends Player> players = world.getPlayers();
		for (Player p : players) {
			Location loc = p.getLocation();
			if (cuboid.contains(loc)) {
				p.teleport(safeSpawn);
			}
		}
	}

	private void clearDroppedItems() {
		World world = Bukkit.getWorld(cuboid.worldId);
		if (world == null) return;
		
		// Get bounding box center and radius
		double centerX = (cuboid.minX + cuboid.maxX) / 2.0;
		double centerY = (cuboid.minY + cuboid.maxY) / 2.0;
		double centerZ = (cuboid.minZ + cuboid.maxZ) / 2.0;
		
		// Calculate max distance from center to corner for bounding sphere
		double dx = cuboid.maxX - centerX;
		double dy = cuboid.maxY - centerY;
		double dz = cuboid.maxZ - centerZ;
		double radius = Math.sqrt(dx * dx + dy * dy + dz * dz) + 1.0; // +1 for safety
		
		Location center = new Location(world, centerX, centerY, centerZ);
		
		// Get all Item entities in the bounding sphere
		Collection<org.bukkit.entity.Entity> nearby = world.getNearbyEntities(center, radius, radius, radius);
		for (org.bukkit.entity.Entity entity : nearby) {
			if (entity instanceof Item) {
				Location entityLoc = entity.getLocation();
				// Double-check with exact cuboid containment
				if (cuboid.contains(entityLoc)) {
					entity.remove();
				}
			}
		}
	}
}


