package com.voluble.titanMC.donatorTools.tools.smeltingPickaxe;

import com.voluble.titanMC.TitanMC;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SmeltingPickaxeListener implements Listener {

	private final Random random = new Random();
	private static final Map<Material, Material> SMELTABLES = new HashMap<>();

	static {
		SMELTABLES.put(Material.IRON_ORE, Material.IRON_INGOT);
		SMELTABLES.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
		SMELTABLES.put(Material.GOLD_ORE, Material.GOLD_INGOT);
		SMELTABLES.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
		SMELTABLES.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		ItemStack tool = player.getInventory().getItemInMainHand();

		// Validate tool
		if (!SmeltingPickaxe.isSmeltingPickaxe(tool)) return;

		// Prevent in creative mode
		if (player.getGameMode() == GameMode.CREATIVE) return;

		Material blockType = event.getBlock().getType();
		
		// Check if the block is allowed to trigger donator tool effects
		if (!TitanMC.getInstance().isBlockAllowed(blockType)) return;
		
		if (!SMELTABLES.containsKey(blockType)) return;

		event.setDropItems(false); // Prevent default drops

		Material smeltedMaterial = SMELTABLES.get(blockType);
		int amount = 1;

		// Handle Fortune enchantment
		if (tool.containsEnchantment(Enchantment.FORTUNE)) {
			int level = tool.getEnchantmentLevel(Enchantment.FORTUNE);
			int bonus = random.nextInt(level + 1);
			amount += bonus;
		}

		// Drop smelted items
		event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(smeltedMaterial, amount));
	}
}
