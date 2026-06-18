package com.voluble.titanMC.util.region;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public final class RegionPerBlockGlow {

	private final Plugin plugin;

	// Which preview entity sits at a given block, per player
	private final Map<UUID, Map<Location, UUID>> glowingBlocksByPlayer = new HashMap<>();
	// Optional: per-player glow color (ARGB hex without alpha)
	private final Map<UUID, Integer> playerGlowColor = new HashMap<>();

	public RegionPerBlockGlow(Plugin plugin) {
		this.plugin = plugin;
	}

	/** Set the preferred glow color for this player (e.g., 0xFFAA00). */
	public void setPlayerGlowColor(Player player, int rgb) {
		playerGlowColor.put(player.getUniqueId(), rgb & 0xFFFFFF);
	}

	/** Spawn a glowing display that mimics the real block for THIS player only. */
	public void setGlowing(Player player, Block block) {
		setGlowing(player, block.getLocation(), block.getBlockData());
	}

	/** Core spawner. */
	public void setGlowing(Player player, Location blockLoc, BlockData data) {
		UUID puid = player.getUniqueId();
		Map<Location, UUID> map = glowingBlocksByPlayer.computeIfAbsent(puid, k -> new HashMap<>());

		Location key = blockLoc.toBlockLocation();
		if (map.containsKey(key)) return; // already glowing for this player

		// Paper: spawn at (corner + tiny nudge) and shrink slightly → avoids z-fighting & outline drift
		Location spawnAt = key.clone().add(0.001, 0.001, 0.001);
		World world = spawnAt.getWorld();
		if (world == null) return;

		BlockDisplay display = world.spawn(spawnAt, BlockDisplay.class, d -> {
			d.setBlock(data);
			d.setGlowing(true);
			d.setBrightness(new Display.Brightness(15, 15));
			d.setViewRange(48f);
			d.setInterpolationDelay(0);
			d.setInterpolationDuration(0);
			d.setTransformation(new Transformation(
					new Vector3f(0f, 0f, 0f),      // translation
					new AxisAngle4f(),             // left rot
					new Vector3f(0.998f, 0.998f, 0.998f), // scale
					new AxisAngle4f()              // right rot
			));
			Integer rgb = playerGlowColor.get(puid);
			if (rgb != null) d.setGlowColorOverride(Color.fromRGB(rgb));
		});

		// Show only to this player, hide from everyone else in the same world
		World w = player.getWorld();
		for (Player other : w.getPlayers()) {
			if (other.equals(player)) {
				other.showEntity(plugin, display);
			} else {
				other.hideEntity(plugin, display);
			}
		}

		map.put(key, display.getUniqueId());
	}

	/** Remove the glowing display at this block for THIS player (if present). */
	public void unsetGlowing(Player player, Location blockLoc) {
		Map<Location, UUID> map = glowingBlocksByPlayer.get(player.getUniqueId());
		if (map == null) return;
		UUID id = map.remove(blockLoc.toBlockLocation());
		if (id == null) return;

		Entity e = blockLoc.getWorld() != null ? blockLoc.getWorld().getEntity(id) : null;
		if (e != null) e.remove();
		if (map.isEmpty()) glowingBlocksByPlayer.remove(player.getUniqueId());
	}

	/** Clear ALL glowing displays for THIS player. */
	public void clearAll(Player player) {
		Map<Location, UUID> map = glowingBlocksByPlayer.remove(player.getUniqueId());
		if (map == null) return;
		World w = player.getWorld();
		if (w == null) return;
		for (UUID id : map.values()) {
			Entity e = w.getEntity(id);
			if (e != null) e.remove();
		}
	}

	/** Safe call on player quit/world change to avoid leftovers. */
	public void handlePlayerExit(Player player) {
		clearAll(player);
		playerGlowColor.remove(player.getUniqueId());
	}
}
