package com.voluble.titanMC.donatortools.tool.smelting;

import com.voluble.titanMC.donatortools.tool.SingleBlockDropContext;
import com.voluble.titanMC.donatortools.tool.SingleBlockDropResolution;
import com.voluble.titanMC.donatortools.tool.SingleBlockDropStrategy;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SmeltingDropStrategy implements SingleBlockDropStrategy {

	private static final Map<Material, Material> RESULTS = new EnumMap<>(Material.class);

	static {
		RESULTS.put(Material.IRON_ORE, Material.IRON_INGOT);
		RESULTS.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
		RESULTS.put(Material.GOLD_ORE, Material.GOLD_INGOT);
		RESULTS.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
		RESULTS.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
	}

	@Override
	public Optional<SingleBlockDropResolution> resolve(SingleBlockDropContext context) {
		Material result = RESULTS.get(context.brokenState().getType());
		if (result == null) return Optional.empty();
		int amount = context.vanillaDrops().stream().mapToInt(ItemStack::getAmount).sum();
		return Optional.of(new SingleBlockDropResolution(
			amount <= 0 ? List.of() : List.of(new ItemStack(result, amount)),
			false
		));
	}
}
