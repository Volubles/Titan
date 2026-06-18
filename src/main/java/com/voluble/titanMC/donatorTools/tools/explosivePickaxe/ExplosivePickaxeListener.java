package com.voluble.titanMC.donatorTools.tools.explosivePickaxe;

import com.voluble.titanMC.TitanMC;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ExplosivePickaxeListener implements Listener {

	private final Random random = new Random();
	private boolean checkingProtection;

	// Define block transformations (stone->cobble, grass->dirt, etc.)
	private static final Map<Material, Material> VANILLA_DROPS = new HashMap<>();

	static {
		VANILLA_DROPS.put(Material.STONE, Material.COBBLESTONE);
		VANILLA_DROPS.put(Material.GRASS_BLOCK, Material.DIRT);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (checkingProtection) return;
		Player player = event.getPlayer();
		ItemStack tool = player.getInventory().getItemInMainHand();

		// 1) Ensure it's our Explosive Pickaxe
		if (!ExplosivePickaxe.isExplosivePickaxe(tool)) return;

		// 2) Prevent in creative mode
		if (player.getGameMode() == GameMode.CREATIVE) return;

		// 3) Check if the block is allowed to trigger donator tool effects
		Block centerBlock = event.getBlock();
		if (!TitanMC.getInstance().isBlockAllowed(centerBlock.getType())) return;

		// 4) Cancel the original event to handle dropping ourselves
		event.setCancelled(true);
		World world = centerBlock.getWorld();
		Location centerLoc = centerBlock.getLocation();

		// 5) Play an explosion sound at the broken block
		world.playSound(centerLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);

		// 6) Gather blocks in a symmetric 3×3×3 area centered on the broken block
		// This creates a cube explosion effect, but only for allowed blocks
		List<Block> blocksToBreak = new ArrayList<>();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					int finalX = centerBlock.getX() + dx;
					int finalY = centerBlock.getY() + dy;
					int finalZ = centerBlock.getZ() + dz;
					Block block = world.getBlockAt(finalX, finalY, finalZ);
					// Only add blocks that are allowed and not air
					if (block.getType() != Material.AIR
							&& TitanMC.getInstance().isBlockAllowed(block.getType())
							&& canBreak(player, block)) {
						blocksToBreak.add(block);
					}
				}
			}
		}

		// 9) Break each block
		for (Block block : blocksToBreak) {
			breakBlock(player, block, tool);
		}
	}

	private boolean canBreak(Player player, Block block) {
		BlockBreakEvent protectionCheck = new BlockBreakEvent(block, player);
		checkingProtection = true;
		try {
			Bukkit.getPluginManager().callEvent(protectionCheck);
			return !protectionCheck.isCancelled();
		} finally {
			checkingProtection = false;
		}
	}


	/**
	 * Break a single block, respecting:
	 * - Fortune
	 * - Silk Touch
	 * - Vanilla transformations
	 */
	private void breakBlock(Player player, Block block, ItemStack tool) {
		Material blockType = block.getType();
		if (blockType == Material.AIR) return;

		// Check if Silk Touch is applied
		boolean hasSilkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH);

		// If Silk Touch is applied, drop the block itself
		if (hasSilkTouch) {
			block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(blockType, 1));
		} else {
			// If the block has a vanilla override (like stone -> cobblestone), use that
			Material dropType = VANILLA_DROPS.getOrDefault(blockType, null);

			if (dropType != null) {
				// Drop the transformed block (e.g., stone -> cobblestone)
				block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(dropType, 1));
			} else {
				// Drop items as Bukkit would naturally handle them (applies Fortune automatically)
				Collection<ItemStack> drops = block.getDrops(tool);

				for (ItemStack drop : drops) {
					int amount = drop.getAmount();

					// Apply Fortune manually if it's an ore
					if (tool.containsEnchantment(Enchantment.FORTUNE) && isOre(blockType)) {
						int fortuneLevel = tool.getEnchantmentLevel(Enchantment.FORTUNE);
						amount += random.nextInt(fortuneLevel + 1); // Add bonus drops
					}

					drop.setAmount(amount);
					block.getWorld().dropItemNaturally(block.getLocation(), drop);
				}
			}
		}

		// Small explosion particle for visuals
		block.getWorld().spawnParticle(Particle.EXPLOSION, block.getLocation(), 6, 0.25, 0.25, 0.25, 0.05);

		// Remove the block
		block.setType(Material.AIR);
	}

	/**
	 * Helper method to check if a block is an ore
	 */
	private boolean isOre(Material material) {
		return material.name().endsWith("_ORE") || material == Material.ANCIENT_DEBRIS;
	}
}
