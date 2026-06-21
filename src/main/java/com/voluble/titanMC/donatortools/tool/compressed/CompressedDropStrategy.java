package com.voluble.titanMC.donatortools.tool.compressed;

import com.voluble.titanMC.donatortools.tool.SingleBlockDropContext;
import com.voluble.titanMC.donatortools.tool.SingleBlockDropResolution;
import com.voluble.titanMC.donatortools.tool.SingleBlockDropStrategy;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CompressedDropStrategy implements SingleBlockDropStrategy {

	private static final Map<Material, Material> RESULTS = new EnumMap<>(Material.class);

	static {
		RESULTS.put(Material.IRON_ORE, Material.IRON_BLOCK);
		RESULTS.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_BLOCK);
		RESULTS.put(Material.GOLD_ORE, Material.GOLD_BLOCK);
		RESULTS.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_BLOCK);
		RESULTS.put(Material.DIAMOND_ORE, Material.DIAMOND_BLOCK);
		RESULTS.put(Material.DEEPSLATE_DIAMOND_ORE, Material.DIAMOND_BLOCK);
		RESULTS.put(Material.EMERALD_ORE, Material.EMERALD_BLOCK);
		RESULTS.put(Material.DEEPSLATE_EMERALD_ORE, Material.EMERALD_BLOCK);
		RESULTS.put(Material.LAPIS_ORE, Material.LAPIS_BLOCK);
		RESULTS.put(Material.DEEPSLATE_LAPIS_ORE, Material.LAPIS_BLOCK);
		RESULTS.put(Material.REDSTONE_ORE, Material.REDSTONE_BLOCK);
		RESULTS.put(Material.DEEPSLATE_REDSTONE_ORE, Material.REDSTONE_BLOCK);
		RESULTS.put(Material.COPPER_ORE, Material.COPPER_BLOCK);
		RESULTS.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_BLOCK);
		RESULTS.put(Material.COAL_ORE, Material.COAL_BLOCK);
		RESULTS.put(Material.DEEPSLATE_COAL_ORE, Material.COAL_BLOCK);
	}

	@Override
	public Optional<SingleBlockDropResolution> resolve(SingleBlockDropContext context) {
		Material result = RESULTS.get(context.brokenState().getType());
		if (result == null) return Optional.empty();
		return Optional.of(new SingleBlockDropResolution(
			context.vanillaDrops().isEmpty()
				? List.of()
				: List.of(new ItemStack(result)),
			true
		));
	}
}
