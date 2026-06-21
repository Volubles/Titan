package com.voluble.titanMC.cells;

import com.voluble.titanMC.cells.model.CellDefinition;
import com.voluble.titanMC.cells.model.CellLease;
import com.voluble.titanMC.cells.model.CellResetJob;
import com.voluble.titanMC.cells.model.TrackedCellBlock;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Bed;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class CellResetService {
	private static final int BLOCKS_PER_TICK = 750;
	private final Plugin plugin;
	private final CellManager cells;
	private Consumer<CellDefinition> stateListener = ignored -> {
	};

	public CellResetService(Plugin plugin, CellManager cells) {
		this.plugin = plugin;
		this.cells = cells;
	}

	public void stateListener(Consumer<CellDefinition> listener) {
		stateListener = java.util.Objects.requireNonNull(listener);
	}

	public void resume() {
		for (CellResetJob job : cells.resetJobs()) start(job);
	}

	public void reset(String cellId) {
		start(cells.beginReset(cellId));
	}

	private void start(CellResetJob job) {
		CellDefinition cell = cells.get(job.cellId());
		if (cell == null) return;
		World world = Bukkit.getWorld(cell.cuboid().worldId);
		if (world == null) {
			plugin.getLogger().severe("Cannot reset cell " + cell.id() + ": world is unavailable");
			return;
		}
		List<CompletableFuture<Chunk>> loads = new ArrayList<>();
		for (int x = cell.cuboid().minChunkX(); x <= cell.cuboid().maxChunkX(); x++)
			for (int z = cell.cuboid().minChunkZ(); z <= cell.cuboid().maxChunkZ(); z++)
				loads.add(world.getChunkAtAsync(x, z));
		CompletableFuture.allOf(loads.toArray(CompletableFuture[]::new)).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
			List<Chunk> chunks = loads.stream().map(CompletableFuture::join).toList();
			chunks.forEach(chunk -> chunk.addPluginChunkTicket(plugin));
			new Scan(job, cell, world, chunks).runTaskTimer(plugin, 1, 1);
		}));
	}

	private final class Scan extends BukkitRunnable {
		private final CellResetJob job;
		private final CellDefinition cell;
		private final World world;
		private final List<Chunk> chunks;
		private final List<Inventory> inventories = new ArrayList<>();
		private final Set<Inventory> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		private final List<ItemStack> items = new ArrayList<>();
		private int x, y, z;

		private Scan(CellResetJob job, CellDefinition cell, World world, List<Chunk> chunks) {
			this.job = job;
			this.cell = cell;
			this.world = world;
			this.chunks = chunks;
			x = cell.cuboid().minX;
			y = cell.cuboid().minY;
			z = cell.cuboid().minZ;
		}

		@Override
		public void run() {
			int done = 0;
			while (done++ < BLOCKS_PER_TICK) {
				var block = world.getBlockAt(x, y, z);
				if (block.getState() instanceof Container container) {
					Inventory inventory = container.getInventory();
					if (seen.add(inventory)) {
						inventories.add(inventory);
						if (job.phase() == CellResetJob.Phase.COLLECTING) for (ItemStack item : inventory.getContents())
							if (item != null && !item.getType().isAir()) items.add(item.clone());
					}
				}
				if (!advance()) {
					cancel();
					prepareOrClear();
					return;
				}
			}
		}

		private boolean advance() {
			if (++z <= cell.cuboid().maxZ) return true;
			z = cell.cuboid().minZ;
			if (++x <= cell.cuboid().maxX) return true;
			x = cell.cuboid().minX;
			return ++y <= cell.cuboid().maxY;
		}

		private void prepareOrClear() {
			if (job.phase() == CellResetJob.Phase.PREPARED) {
				clear(job.recoveryLotId());
				return;
			}
			CellLease lease = cells.lease(job.cellId());
			if (lease == null) {
				release();
				return;
			}
			List<ItemStack> recovered = new ArrayList<>(items);
			for (TrackedCellBlock tracked : cells.tracked(lease)) {
				World blockWorld = Bukkit.getWorld(tracked.worldId());
				if (blockWorld == null) continue;
				var block = blockWorld.getBlockAt(tracked.x(), tracked.y(), tracked.z());
				if (block.getBlockData() instanceof Bisected bisected && bisected.getHalf() == Bisected.Half.TOP) continue;
				if (block.getBlockData() instanceof Bed bed && bed.getPart() == Bed.Part.HEAD) continue;
				if (block.getType().isItem()) recovered.add(new ItemStack(block.getType()));
				else recovered.addAll(block.getDrops());
			}
			List<byte[]> serialized = recovered.stream().map(ItemStack::serializeAsBytes).toList();
			cells.storage().createRecoveryLot(lease, cell.wardId(), serialized).whenComplete((lot, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
				if (error != null) {
					plugin.getLogger().severe("Failed to prepare reset for " + cell.id() + ": " + error.getMessage());
					release();
					return;
				}
				cells.markPrepared(job, lot);
				clear(lot);
			}));
		}

		private void clear(long lotId) {
			for (Inventory inventory : inventories) inventory.clear();
			List<TrackedCellBlock> blocks = cells.tracked(cells.lease(job.cellId()));
			new Clear(job, blocks, lotId, chunks).runTaskTimer(plugin, 1, 1);
		}

		private void release() {
			chunks.forEach(chunk -> chunk.removePluginChunkTicket(plugin));
		}
	}

	private final class Clear extends BukkitRunnable {
		private final CellResetJob job;
		private final List<TrackedCellBlock> blocks;
		private final long lotId;
		private final List<Chunk> chunks;
		private int index;

		private Clear(CellResetJob job, List<TrackedCellBlock> blocks, long lotId, List<Chunk> chunks) {
			this.job = job;
			this.blocks = blocks;
			this.lotId = lotId;
			this.chunks = chunks;
		}

		@Override
		public void run() {
			int done = 0;
			while (index < blocks.size() && done++ < BLOCKS_PER_TICK) {
				TrackedCellBlock b = blocks.get(index++);
				World w = Bukkit.getWorld(b.worldId());
				if (w != null) w.getBlockAt(b.x(), b.y(), b.z()).setType(Material.AIR, false);
			}
			if (index >= blocks.size()) {
				cancel();
				cells.completeReset(job, lotId);
				chunks.forEach(chunk -> chunk.removePluginChunkTicket(plugin));
				stateListener.accept(cells.get(job.cellId()));
			}
		}
	}
}
