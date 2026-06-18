package com.voluble.titanMC.donatorTools.tools.blockPickaxe;

import com.voluble.titanMC.TitanMC;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class BlockPickaxeListener implements Listener {

	// Mapping from ore to its corresponding block form.
	private static final Map<Material, Material> ORE_TO_BLOCK = new HashMap<>();

	static {
		ORE_TO_BLOCK.put(Material.IRON_ORE, Material.IRON_BLOCK);
		ORE_TO_BLOCK.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_BLOCK);
		ORE_TO_BLOCK.put(Material.GOLD_ORE, Material.GOLD_BLOCK);
		ORE_TO_BLOCK.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_BLOCK);
		ORE_TO_BLOCK.put(Material.DIAMOND_ORE, Material.DIAMOND_BLOCK);
		ORE_TO_BLOCK.put(Material.DEEPSLATE_DIAMOND_ORE, Material.DIAMOND_BLOCK);
		ORE_TO_BLOCK.put(Material.EMERALD_ORE, Material.EMERALD_BLOCK);
		ORE_TO_BLOCK.put(Material.DEEPSLATE_EMERALD_ORE, Material.EMERALD_BLOCK);
		ORE_TO_BLOCK.put(Material.LAPIS_ORE, Material.LAPIS_BLOCK);
		ORE_TO_BLOCK.put(Material.DEEPSLATE_LAPIS_ORE, Material.LAPIS_BLOCK);
		ORE_TO_BLOCK.put(Material.REDSTONE_ORE, Material.REDSTONE_BLOCK);
		ORE_TO_BLOCK.put(Material.DEEPSLATE_REDSTONE_ORE, Material.REDSTONE_BLOCK);
		ORE_TO_BLOCK.put(Material.COPPER_ORE, Material.COPPER_BLOCK);
		ORE_TO_BLOCK.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_BLOCK);
		ORE_TO_BLOCK.put(Material.COAL_ORE, Material.COAL_BLOCK);
		ORE_TO_BLOCK.put(Material.DEEPSLATE_COAL_ORE, Material.COAL_BLOCK);
		// Ancient Debris is already a block.
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		ItemStack tool = player.getInventory().getItemInMainHand();

		// 1) Ensure it's our Block Pickaxe
		if (!BlockPickaxe.isBlockPickaxe(tool)) return;

		// 2) Do not run in creative mode
		if (player.getGameMode() == GameMode.CREATIVE) return;

		// 3) Check if the block is allowed to trigger donator tool effects
		Block block = event.getBlock();
		if (!TitanMC.getInstance().isBlockAllowed(block.getType())) return;

		// 4) Cancel default drops
		event.setDropItems(false);
		World world = block.getWorld();

		// 6) If the block is an ore in our mapping, convert it
		Material oreType = block.getType();
		if (ORE_TO_BLOCK.containsKey(oreType)) {
			Material blockForm = ORE_TO_BLOCK.get(oreType);
			// Optionally: You might want to check for Silk Touch, Fortune, etc.
			// For a pure conversion, we simply drop the block form.
			world.dropItemNaturally(block.getLocation(), new ItemStack(blockForm, 1));
			// Feedback effects
			world.spawnParticle(Particle.ENCHANT, block.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
			block.setType(Material.AIR);
			recordMineBreak(block);
		} else {
			// If the block isn’t an ore, break normally
			block.breakNaturally(tool);
			recordMineBreak(block);
		}
	}

	private void recordMineBreak(Block block) {
		var plugin = TitanMC.getInstance();
		var mine = plugin.getMineManager().getFirstAt(block.getLocation());
		if (mine == null) return;
		mine.incrementBroken(1);
		if (mine.isEnabled() && mine.shouldAutoResetByDepletion()) {
			plugin.getMineScheduler().scheduleDepletionReset(mine.getName());
		}
	}
}
