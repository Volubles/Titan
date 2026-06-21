package com.voluble.titanMC.donatortools.tool.bountiful;

import com.voluble.titanMC.donatortools.config.DonatorToolsSettings;
import com.voluble.titanMC.donatortools.drop.VanillaLoot;
import com.voluble.titanMC.donatortools.tool.SingleBlockDropContext;
import com.voluble.titanMC.donatortools.tool.SingleBlockDropResolution;
import com.voluble.titanMC.donatortools.tool.SingleBlockDropStrategy;
import org.bukkit.Material;
import org.bukkit.block.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BountifulDropStrategy implements SingleBlockDropStrategy {

	private static final List<Material> PRIORITY = List.of(
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

	private final DonatorToolsSettings configuration;
	private final VanillaLoot vanillaLoot;

	public BountifulDropStrategy(
		DonatorToolsSettings configuration,
		VanillaLoot vanillaLoot
	) {
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.vanillaLoot = Objects.requireNonNull(vanillaLoot, "vanillaLoot");
	}

	@Override
	public Optional<SingleBlockDropResolution> resolve(SingleBlockDropContext context) {
		BlockState selected = candidates(context).stream()
			.filter(state -> PRIORITY.contains(state.getType()))
			.filter(state -> configuration.current().allows(state.getType()))
			.min(Comparator.comparingInt(state -> PRIORITY.indexOf(state.getType())))
			.orElse(null);
		if (selected == null) return Optional.empty();
		return Optional.of(new SingleBlockDropResolution(
			vanillaLoot.drops(selected, context.tool(), context.player()),
			true
		));
	}

	private static List<BlockState> candidates(SingleBlockDropContext context) {
		ArrayList<BlockState> states = new ArrayList<>(27);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) states.add(context.brokenState());
					else states.add(context.block().getRelative(dx, dy, dz).getState());
				}
			}
		}
		return states;
	}
}
