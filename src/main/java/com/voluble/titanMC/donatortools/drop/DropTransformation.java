package com.voluble.titanMC.donatortools.drop;

import org.bukkit.inventory.ItemStack;

import java.util.List;

@FunctionalInterface
public interface DropTransformation {

	List<ItemStack> transform(List<ItemStack> vanillaDrops);
}
