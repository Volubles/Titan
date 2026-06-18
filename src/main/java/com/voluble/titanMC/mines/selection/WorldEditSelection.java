package com.voluble.titanMC.mines.selection;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.voluble.titanMC.util.RegionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class WorldEditSelection {

	private WorldEditSelection() {}

	public static RegionUtils.Cuboid getCuboid(Player player) throws SelectionException {
		if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit")
				&& !Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")) {
			throw new SelectionException("WorldEdit or FastAsyncWorldEdit is not installed or enabled.");
		}

		var actor = BukkitAdapter.adapt(player);
		var session = WorldEdit.getInstance().getSessionManager().get(actor);
		World selectionWorld = session.getSelectionWorld();
		if (selectionWorld == null) {
			throw new SelectionException("Make a WorldEdit cuboid selection first.");
		}

		Region selection;
		try {
			selection = session.getSelection(selectionWorld);
		} catch (IncompleteRegionException exception) {
			throw new SelectionException("Your WorldEdit selection is incomplete.");
		}
		if (!(selection instanceof CuboidRegion)) {
			throw new SelectionException("The WorldEdit selection must be a cuboid.");
		}

		org.bukkit.World world = BukkitAdapter.adapt(selectionWorld);
		BlockVector3 min = selection.getMinimumPoint();
		BlockVector3 max = selection.getMaximumPoint();
		return new RegionUtils.Cuboid(
			world.getUID(),
			min.x(), min.y(), min.z(),
			max.x(), max.y(), max.z()
		);
	}
}
