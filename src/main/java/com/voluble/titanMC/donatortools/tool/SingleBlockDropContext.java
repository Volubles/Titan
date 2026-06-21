package com.voluble.titanMC.donatortools.tool;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

public record SingleBlockDropContext(
	Block block,
	BlockState brokenState,
	Player player,
	ItemStack tool,
	List<ItemStack> vanillaDrops
) {
	public SingleBlockDropContext {
		Objects.requireNonNull(block, "block");
		Objects.requireNonNull(brokenState, "brokenState");
		Objects.requireNonNull(player, "player");
		tool = Objects.requireNonNull(tool, "tool").clone();
		vanillaDrops = List.copyOf(vanillaDrops);
	}
}
