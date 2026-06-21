package com.voluble.titanMC.donatortools.tool;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public record SingleBlockDropResolution(List<ItemStack> drops, boolean enchantedEffect) {

	public SingleBlockDropResolution {
		drops = List.copyOf(drops);
	}
}
