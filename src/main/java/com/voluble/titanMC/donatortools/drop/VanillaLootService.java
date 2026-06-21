package com.voluble.titanMC.donatortools.drop;

import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

public final class VanillaLootService implements VanillaLoot {

	@Override
	public List<ItemStack> drops(BlockState state, ItemStack tool, Player player) {
		Objects.requireNonNull(state, "state");
		Objects.requireNonNull(tool, "tool");
		Objects.requireNonNull(player, "player");
		return state.getDrops(tool, player).stream().map(ItemStack::clone).toList();
	}
}
