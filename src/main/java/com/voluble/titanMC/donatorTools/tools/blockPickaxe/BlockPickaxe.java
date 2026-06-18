package com.voluble.titanMC.donatorTools.tools.blockPickaxe;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class BlockPickaxe {

	private static final String NAME = ChatColor.LIGHT_PURPLE + "Block Pickaxe";
	private static final String LORE = ChatColor.YELLOW + "Converts mined ore into its block form!";

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

	public static boolean isBlockPickaxe(ItemStack item) {
		if (item == null || !item.hasItemMeta()) return false;
		ItemMeta meta = item.getItemMeta();
		return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(NAME);
	}
}
