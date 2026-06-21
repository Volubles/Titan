package com.voluble.titanMC.donatortools.tool;

import com.voluble.titanMC.donatortools.config.DonatorToolsSettings;
import com.voluble.titanMC.donatortools.protection.BlockBreakPermission;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ExplosiveBreakPlanner {

	private final DonatorToolsSettings configuration;
	private final BlockBreakPermission protection;

	public ExplosiveBreakPlanner(
		DonatorToolsSettings configuration,
		BlockBreakPermission protection
	) {
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.protection = Objects.requireNonNull(protection, "protection");
	}

	public List<Block> additionalBlocks(Player player, Block center) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(center, "center");
		List<Block> blocks = new ArrayList<>(26);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) continue;
					Block block = center.getRelative(dx, dy, dz);
					if (block.getType().isAir()) continue;
					if (!configuration.current().allows(block.getType())) continue;
					if (!protection.canBreak(player, block)) continue;
					blocks.add(block);
				}
			}
		}
		return List.copyOf(blocks);
	}
}
