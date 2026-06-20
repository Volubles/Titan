package com.voluble.titanMC.mines.selection;

import com.voluble.titanMC.regions.model.BlockBox;
import com.voluble.titanMC.regions.model.CuboidGeometry;
import com.voluble.titanMC.regions.selection.SelectedRegion;
import com.voluble.titanMC.util.RegionUtils;
import com.voluble.titanMC.regions.selection.SelectionException;
import com.voluble.titanMC.regions.selection.WorldEditRegionSelection;
import org.bukkit.entity.Player;

public final class WorldEditSelection {

	private WorldEditSelection() {}

	public static RegionUtils.Cuboid getCuboid(Player player) throws SelectionException {
		SelectedRegion selection = WorldEditRegionSelection.read(player);
		if (!(selection.geometry() instanceof CuboidGeometry cuboid)) {
			throw new SelectionException("The WorldEdit selection must be a cuboid.");
		}
		BlockBox bounds = cuboid.bounds();
		return new RegionUtils.Cuboid(
			selection.worldId(),
			bounds.minX(), bounds.minY(), bounds.minZ(),
			bounds.maxXExclusive() - 1, bounds.maxYExclusive() - 1, bounds.maxZExclusive() - 1
		);
	}
}
