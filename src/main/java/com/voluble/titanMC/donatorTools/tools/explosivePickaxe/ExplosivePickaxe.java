package com.voluble.titanMC.donatorTools.tools.explosivePickaxe;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;

public class ExplosivePickaxe {

	private static final String NAME = ChatColor.RED + "Explosive Pickaxe";
	private static final String LORE = ChatColor.YELLOW + "This pickaxe explodes in a 3x3 radius when mining!";

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

	public static boolean isExplosivePickaxe(ItemStack item) {
		if (item == null || !item.hasItemMeta()) return false;
		ItemMeta meta = item.getItemMeta();
		return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(NAME);
	}
}
