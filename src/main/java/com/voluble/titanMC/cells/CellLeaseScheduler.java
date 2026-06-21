package com.voluble.titanMC.cells;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class CellLeaseScheduler {
	private final Plugin plugin;private final CellManager cells;private final CellResetService resets;private final CellManagementService management;private BukkitTask task;
	public CellLeaseScheduler(Plugin plugin,CellManager cells,CellResetService resets,CellManagementService management){this.plugin=plugin;this.cells=cells;this.resets=resets;this.management=management;}
	public void start(){if(task!=null)return;task=plugin.getServer().getScheduler().runTaskTimer(plugin,()->{long now=System.currentTimeMillis();for(var cell:cells.cells()){var lease=cells.lease(cell.id());if(lease==null||lease.expiresAtEpochMillis()>now||cells.resetJobs().stream().anyMatch(job->job.cellId().equals(cell.id())))continue;if(!lease.autoRenew()){resets.reset(cell.id());continue;}management.renewAutomatically(lease).thenAccept(renewed->Bukkit.getScheduler().runTask(plugin,()->{var current=cells.lease(cell.id());if(!renewed&&current!=null&&current.generation()==lease.generation()&&cells.resetJobs().stream().noneMatch(job->job.cellId().equals(cell.id())))resets.reset(cell.id());}));}},20L,600L);}
	public void stop(){if(task!=null){task.cancel();task=null;}}
}
