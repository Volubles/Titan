package com.voluble.titanMC.donatorTools.tools.bountifulPickaxe;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;

public class BountifulPickaxe {

	private static final String NAME = ChatColor.AQUA + "Bountiful Pickaxe";
	private static final String LORE = ChatColor.GOLD + "This pickaxe searches a 3x3 area and drops the best ore!";

	/**
	 * Creates the Bountiful Pickaxe item.
	 */
	public static ItemStack createPickaxe() {
		ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
		ItemMeta meta = pickaxe.getItemMeta();

		if (meta != null) {
			meta.setDisplayName(NAME);
			meta.setLore(Arrays.asList(LORE));
			meta.setUnbreakable(false);
			pickaxe.setItemMeta(meta);
		}
		return pickaxe;
	}

	/**
	 * Checks if an ItemStack is a Bountiful Pickaxe.
	 */
	public static boolean isBountifulPickaxe(ItemStack item) {
		if (item == null || !item.hasItemMeta()) return false;
		ItemMeta meta = item.getItemMeta();
		return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(NAME);
	}
}
