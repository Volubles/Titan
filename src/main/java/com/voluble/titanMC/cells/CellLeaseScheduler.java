package com.voluble.titanMC.cells;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class CellLeaseScheduler {
	private final Plugin plugin;private final CellManager cells;private final CellResetService resets;private BukkitTask task;
	public CellLeaseScheduler(Plugin plugin,CellManager cells,CellResetService resets){this.plugin=plugin;this.cells=cells;this.resets=resets;}
	public void start(){if(task!=null)return;task=plugin.getServer().getScheduler().runTaskTimer(plugin,()->{long now=System.currentTimeMillis();for(var cell:cells.cells()){var lease=cells.lease(cell.id());if(lease!=null&&lease.expiresAtEpochMillis()<=now&&cells.resetJobs().stream().noneMatch(job->job.cellId().equals(cell.id())))resets.reset(cell.id());}},20L,600L);}
	public void stop(){if(task!=null){task.cancel();task=null;}}
}
