package com.voluble.titanMC.donatorTools.tools.bountifulPickaxe;

import com.voluble.titanMC.TitanMC;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BountifulPickaxeListener implements Listener {

	private final Random random = new Random();

	// Ore ranking (lower index = higher value)
	private static final List<Material> ORE_PRIORITY = Arrays.asList(
			Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
			Material.ANCIENT_DEBRIS,
			Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
			Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
			Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
			Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
			Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
			Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
			Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE
	);

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		ItemStack tool = player.getInventory().getItemInMainHand();

		// 1) Ensure it's our Bountiful Pickaxe
		if (!BountifulPickaxe.isBountifulPickaxe(tool)) return;

		// 2) Prevent in creative mode
		if (player.getGameMode() == GameMode.CREATIVE) return;

		// 3) Check if the block is allowed to trigger donator tool effects
		Block centerBlock = event.getBlock();
		if (!TitanMC.getInstance().isBlockAllowed(centerBlock.getType())) return;

		// 4) Cancel default drop to control loot manually
		event.setDropItems(false);
		World world = centerBlock.getWorld();

		// 5) Gather blocks in a symmetric 3×3×3 area centered on the broken block
		// Only scan blocks that are allowed
		List<Block> blocksToScan = new ArrayList<>();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					int finalX = centerBlock.getX() + dx;
					int finalY = centerBlock.getY() + dy;
					int finalZ = centerBlock.getZ() + dz;
					Block block = world.getBlockAt(finalX, finalY, finalZ);
					// Only scan blocks that are allowed and not air
					if (block.getType() != Material.AIR && TitanMC.getInstance().isBlockAllowed(block.getType())) {
						blocksToScan.add(block);
					}
				}
			}
		}


		// 7) Scan the gathered 3×3×3 region to find the best ore.
		int bestRank = Integer.MAX_VALUE;
		Material bestOre = null;
		for (Block block : blocksToScan) {
			Material type = block.getType();
			int rank = ORE_PRIORITY.indexOf(type);
			if (rank != -1 && rank < bestRank) {
				bestRank = rank;
				bestOre = type;
			}
		}

		// 8) Drop the best ore (or default to normal block drop)
		dropBestOre(player, bestOre, centerBlock, tool);
	}

	/**
	 * Drops the best ore found, applying Fortune & Silk Touch.
	 */
	private void dropBestOre(Player player, Material bestOre, Block block, ItemStack tool) {
		if (bestOre == null) {
			// No ore found – drop the original block normally.
			block.breakNaturally(tool);
			return;
		}
		boolean hasSilkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH);
		if (hasSilkTouch) {
			block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(bestOre, 1));
		} else {
			Collection<ItemStack> drops = getDropsFor(bestOre, tool);
			for (ItemStack drop : drops) {
				if (tool.containsEnchantment(Enchantment.FORTUNE)) {
					int fortuneLevel = tool.getEnchantmentLevel(Enchantment.FORTUNE);
					int bonus = random.nextInt(fortuneLevel + 1);
					drop.setAmount(drop.getAmount() + bonus);
				}
				block.getWorld().dropItemNaturally(block.getLocation(), drop);
			}
		}
		// Feedback effects
		block.getWorld().spawnParticle(Particle.ENCHANT, block.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
		// Remove the block
		block.setType(Material.AIR);
	}

	/**
	 * Returns the proper drops for the given ore type as item(s).
	 * This is a simplified approach; you may expand it for more accurate amounts.
	 */
	private Collection<ItemStack> getDropsFor(Material oreType, ItemStack tool) {
		if (oreType == Material.COAL_ORE || oreType == Material.DEEPSLATE_COAL_ORE) {
			return Collections.singleton(new ItemStack(Material.COAL, 1));
		}
		if (oreType == Material.DIAMOND_ORE || oreType == Material.DEEPSLATE_DIAMOND_ORE) {
			return Collections.singleton(new ItemStack(Material.DIAMOND, 1));
		}
		if (oreType == Material.EMERALD_ORE || oreType == Material.DEEPSLATE_EMERALD_ORE) {
			return Collections.singleton(new ItemStack(Material.EMERALD, 1));
		}
		if (oreType == Material.REDSTONE_ORE || oreType == Material.DEEPSLATE_REDSTONE_ORE) {
			return Collections.singleton(new ItemStack(Material.REDSTONE, 4));
		}
		if (oreType == Material.LAPIS_ORE || oreType == Material.DEEPSLATE_LAPIS_ORE) {
			return Collections.singleton(new ItemStack(Material.LAPIS_LAZULI, 4));
		}
		if (oreType == Material.COPPER_ORE || oreType == Material.DEEPSLATE_COPPER_ORE) {
			return Collections.singleton(new ItemStack(Material.RAW_COPPER, 1));
		}
		if (oreType == Material.IRON_ORE || oreType == Material.DEEPSLATE_IRON_ORE) {
			return Collections.singleton(new ItemStack(Material.RAW_IRON, 1));
		}
		if (oreType == Material.GOLD_ORE || oreType == Material.DEEPSLATE_GOLD_ORE) {
			return Collections.singleton(new ItemStack(Material.RAW_GOLD, 1));
		}
		if (oreType == Material.ANCIENT_DEBRIS) {
			return Collections.singleton(new ItemStack(Material.ANCIENT_DEBRIS, 1));
		}
		return Collections.singleton(new ItemStack(oreType, 1));
	}
}
