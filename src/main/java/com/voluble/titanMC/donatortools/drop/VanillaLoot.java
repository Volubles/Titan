package com.voluble.titanMC.donatortools.drop;

import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@FunctionalInterface
public interface VanillaLoot {

	List<ItemStack> drops(BlockState state, ItemStack tool, Player player);
}
