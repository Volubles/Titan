package com.voluble.titanMC.donatortools.protection;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface BlockBreakPermission {

	boolean canBreak(Player player, Block block);
}
