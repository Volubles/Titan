package com.voluble.titanMC.cells;

import com.voluble.titanMC.cells.model.CellDefinition;
import com.voluble.titanMC.cells.model.CellLease;
import com.voluble.titanMC.cells.model.TrackedCellBlock;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CellTrackingListener implements Listener {
	private final CellManager cells;
	private final Map<BlockBreakEvent,List<TrackedCellBlock>> pendingBreaks=new HashMap<>();
	public CellTrackingListener(CellManager cells){this.cells=cells;}

	@EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
	public void protectShell(BlockBreakEvent event){CellDefinition cell=cells.at(event.getBlock().getLocation());if(cell==null||event.getPlayer().hasPermission("titanmc.protection.bypass"))return;if(!cells.isTracked(cell,event.getBlock().getLocation())){event.setCancelled(true);return;}pendingBreaks.put(event,trackedFootprint(cell,event.getBlock()));}

	@EventHandler(priority=EventPriority.MONITOR)
	public void finishBreak(BlockBreakEvent event){List<TrackedCellBlock> blocks=pendingBreaks.remove(event);if(!event.isCancelled()&&blocks!=null)cells.untrack(blocks);}

	@EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
	public void trackPlacement(BlockPlaceEvent event){CellDefinition cell=cells.at(event.getBlockPlaced().getLocation());if(cell==null||event.getPlayer().hasPermission("titanmc.protection.bypass"))return;CellLease lease=cells.lease(cell.id());if(lease==null)return;cells.track(CellBlockFootprint.collect(event.getBlockPlaced()).stream().map(block->tracked(cell,lease,block)).toList());}

	private List<TrackedCellBlock> trackedFootprint(CellDefinition cell,Block block){CellLease lease=cells.lease(cell.id());if(lease==null)return List.of();return CellBlockFootprint.collect(block).stream().filter(part->cells.isTracked(cell,part.getLocation())).map(part->tracked(cell,lease,part)).toList();}
	private static TrackedCellBlock tracked(CellDefinition cell,CellLease lease,Block block){return new TrackedCellBlock(cell.id(),lease.generation(),block.getWorld().getUID(),block.getX(),block.getY(),block.getZ());}
}
