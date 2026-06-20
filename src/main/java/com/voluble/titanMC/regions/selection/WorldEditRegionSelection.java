package com.voluble.titanMC.regions.selection;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.polyhedron.Triangle;
import com.sk89q.worldedit.world.World;
import com.voluble.titanMC.regions.model.BlockBox;
import com.voluble.titanMC.regions.model.BlockPoint2;
import com.voluble.titanMC.regions.model.ConvexPolyhedronGeometry;
import com.voluble.titanMC.regions.model.CuboidGeometry;
import com.voluble.titanMC.regions.model.PolygonPrismGeometry;
import com.voluble.titanMC.regions.model.PolyhedronPlane;
import com.voluble.titanMC.regions.model.RegionGeometry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public final class WorldEditRegionSelection {

	private WorldEditRegionSelection() {}

	public static SelectedRegion read(Player player) throws SelectionException {
		if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit")
				&& !Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")) {
			throw new SelectionException("WorldEdit or FastAsyncWorldEdit is not installed or enabled.");
		}

		var actor = BukkitAdapter.adapt(player);
		var session = WorldEdit.getInstance().getSessionManager().get(actor);
		World selectionWorld = session.getSelectionWorld();
		if (selectionWorld == null) throw new SelectionException("Make a WorldEdit selection first.");

		Region selection;
		try {
			selection = session.getSelection(selectionWorld);
		} catch (IncompleteRegionException exception) {
			throw new SelectionException("Your WorldEdit selection is incomplete.");
		}

		org.bukkit.World world = BukkitAdapter.adapt(selectionWorld);
		return new SelectedRegion(world.getUID(), convert(selection));
	}

	private static RegionGeometry convert(Region selection) throws SelectionException {
		if (selection instanceof CuboidRegion) {
			return new CuboidGeometry(bounds(selection));
		}
		if (selection instanceof Polygonal2DRegion polygon) {
			List<BlockPoint2> points = polygon.getPoints().stream()
				.map(point -> new BlockPoint2(point.x(), point.z()))
				.toList();
			return new PolygonPrismGeometry(points, polygon.getMinimumY(), polygon.getMaximumY());
		}
		if (selection instanceof ConvexPolyhedralRegion polyhedron) {
			List<PolyhedronPlane> planes = polyhedron.getTriangles().stream()
				.map(WorldEditRegionSelection::plane)
				.toList();
			return new ConvexPolyhedronGeometry(bounds(polyhedron), planes);
		}
		throw new SelectionException(
			"Unsupported WorldEdit selection type: " + selection.getClass().getSimpleName()
				+ ". Use cuboid, polygon, or convex polyhedron."
		);
	}

	private static BlockBox bounds(Region region) throws SelectionException {
		BlockVector3 min = region.getMinimumPoint();
		BlockVector3 max = region.getMaximumPoint();
		try {
			return BlockBox.inclusive(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
		} catch (IllegalArgumentException exception) {
			throw new SelectionException("The WorldEdit selection exceeds TitanMC's supported coordinate range.");
		}
	}

	private static PolyhedronPlane plane(Triangle triangle) {
		Vector3 first = triangle.getVertex(0);
		Vector3 second = triangle.getVertex(1);
		Vector3 third = triangle.getVertex(2);
		Vector3 normal = second.subtract(first).cross(third.subtract(first)).normalize();
		double maximumDotProduct = Math.max(
			normal.dot(first),
			Math.max(normal.dot(second), normal.dot(third))
		);
		return new PolyhedronPlane(normal.x(), normal.y(), normal.z(), maximumDotProduct);
	}
}
