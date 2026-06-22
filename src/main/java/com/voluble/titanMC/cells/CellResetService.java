package com.voluble.titanMC.cells;

import com.voluble.titanMC.cells.baseline.CellBaseline;
import com.voluble.titanMC.cells.model.CellDefinition;
import com.voluble.titanMC.cells.model.CellLease;
import com.voluble.titanMC.cells.model.CellResetJob;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class CellResetService {
	private static final int BLOCKS_PER_TICK = 750;
	private static final int MAX_CONCURRENT_RESETS = 2;
	private static final long MAX_RETRY_DELAY_MILLIS = 300_000L;
	private final Plugin plugin;
	private final CellManager cells;
	private final Set<String> active = new HashSet<>();
	private BukkitTask coordinator;
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
		if (coordinator != null) return;
		coordinator = Bukkit.getScheduler().runTaskTimer(plugin, this::startReadyJobs, 1L, 20L);
	}

	public void reset(String cellId) {
		CellResetJob job = cells.beginReset(cellId);
		if (active.size() < MAX_CONCURRENT_RESETS) start(job);
	}

	private void startReadyJobs() {
		long now = System.currentTimeMillis();
		for (CellResetJob job : cells.resetJobs()) {
			if (active.size() >= MAX_CONCURRENT_RESETS) return;
			if (job.ready(now)) start(job);
		}
	}

	private void start(CellResetJob job) {
		if (!active.add(job.cellId())) return;
		CellDefinition cell = cells.get(job.cellId());
		if (cell == null) {
			fail(job, "cell definition is unavailable", null);
			return;
		}
		cells.storage().loadBaseline(cell.id()).whenComplete((baseline, baselineFailure) ->
			Bukkit.getScheduler().runTask(plugin, () -> prepareChunks(job, cell, baseline, baselineFailure))
		);
	}

	private void prepareChunks(CellResetJob job, CellDefinition cell, CellBaseline baseline, Throwable baselineFailure) {
		if (baselineFailure != null) {
			fail(job, "baseline is unavailable", baselineFailure);
			return;
		}
		World world = Bukkit.getWorld(cell.cuboid().worldId);
		if (world == null) {
			fail(job, "world is unavailable", null);
			return;
		}
		List<BlockData> palette;
		try {
			validateDimensions(cell, baseline);
			palette = baseline.blockPalette().stream().map(Bukkit::createBlockData).toList();
		} catch (RuntimeException exception) {
			fail(job, "baseline is invalid", exception);
			return;
		}
		List<CompletableFuture<Chunk>> loads = new ArrayList<>();
		for (int x = cell.cuboid().minChunkX(); x <= cell.cuboid().maxChunkX(); x++)
			for (int z = cell.cuboid().minChunkZ(); z <= cell.cuboid().maxChunkZ(); z++)
				loads.add(world.getChunkAtAsync(x, z));
		CompletableFuture.allOf(loads.toArray(CompletableFuture[]::new)).whenComplete((ignored, chunkFailure) ->
			Bukkit.getScheduler().runTask(plugin, () -> {
				if (chunkFailure != null) {
					fail(job, "chunks could not be loaded", chunkFailure);
					return;
				}
				List<Chunk> chunks = loads.stream().map(CompletableFuture::join).toList();
				chunks.forEach(chunk -> chunk.addPluginChunkTicket(plugin));
				new Scan(job, cell, world, chunks, baseline, palette).runTaskTimer(plugin, 1, 1);
			})
		);
	}

	private static void validateDimensions(CellDefinition cell, CellBaseline baseline) {
		var cuboid = cell.cuboid();
		if (baseline.sizeX() != cuboid.maxX - cuboid.minX + 1
			|| baseline.sizeY() != cuboid.maxY - cuboid.minY + 1
			|| baseline.sizeZ() != cuboid.maxZ - cuboid.minZ + 1) {
			throw new IllegalStateException("Baseline dimensions do not match the cell region");
		}
	}

	private void fail(CellResetJob job, String reason, Throwable failure) {
		CellResetJob current = cells.resetJob(job.cellId());
		int attempt = current == null ? job.attempts() + 1 : current.attempts() + 1;
		if (failure == null) {
			plugin.getLogger().warning(
				"Cell reset attempt " + attempt + " failed for " + job.cellId() + ": " + reason
			);
		} else {
			plugin.getLogger().log(
				java.util.logging.Level.WARNING,
				"Cell reset attempt " + attempt + " failed for " + job.cellId() + ": " + reason,
				failure
			);
		}
		if (current != null && current.leaseGeneration() == job.leaseGeneration()) {
			long delay = retryDelay(current.attempts() + 1);
			String detail = failure == null || rootMessage(failure).equals(reason)
				? reason
				: reason + ": " + rootMessage(failure);
			try {
				cells.recordResetFailure(current, System.currentTimeMillis() + delay, detail);
			} catch (RuntimeException persistenceFailure) {
				plugin.getLogger().log(
					java.util.logging.Level.SEVERE,
					"Could not persist reset retry for cell " + job.cellId(),
					persistenceFailure
				);
			}
		}
		active.remove(job.cellId());
	}

	private static long retryDelay(int attempt) {
		int shift = Math.min(6, Math.max(0, attempt - 1));
		return Math.min(MAX_RETRY_DELAY_MILLIS, 5_000L << shift);
	}

	private static String rootMessage(Throwable failure) {
		Throwable cause = failure;
		while (cause.getCause() != null) cause = cause.getCause();
		return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
	}

	private final class Scan extends BukkitRunnable {
		private final CellResetJob job;
		private final CellDefinition cell;
		private final World world;
		private final List<Chunk> chunks;
		private final CellBaseline baseline;
		private final List<BlockData> palette;
		private final List<Inventory> inventories = new ArrayList<>();
		private final List<ItemStack> items = new ArrayList<>();
		private int x, y, z;

		private Scan(
			CellResetJob job,
			CellDefinition cell,
			World world,
			List<Chunk> chunks,
			CellBaseline baseline,
			List<BlockData> palette
		) {
			this.job = job;
			this.cell = cell;
			this.world = world;
			this.chunks = chunks;
			this.baseline = baseline;
			this.palette = palette;
			x = cell.cuboid().minX;
			y = cell.cuboid().minY;
			z = cell.cuboid().minZ;
		}

		@Override
		public void run() {
			try {
				int done = 0;
				while (done++ < BLOCKS_PER_TICK) {
					var block = world.getBlockAt(x, y, z);
					if (block.getState() instanceof Container container) {
						Inventory inventory = container instanceof Chest chest
							? chest.getBlockInventory()
							: container.getInventory();
						inventories.add(inventory);
						if (job.phase() == CellResetJob.Phase.COLLECTING) {
							for (ItemStack item : inventory.getContents()) {
								if (item != null && !item.getType().isAir()) items.add(item.clone());
							}
						}
					}
					if (job.phase() == CellResetJob.Phase.COLLECTING && differsFromBaseline(block, x, y, z)) {
						collectBlockDrop(block, items);
					}
					if (!advance()) {
						cancel();
						prepareOrClear();
						return;
					}
				}
			} catch (RuntimeException exception) {
				cancel();
				release();
				fail(job, "cell scan failed", exception);
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
				fail(job, "cell lease is unavailable", null);
				return;
			}
			List<byte[]> serialized = items.stream().map(ItemStack::serializeAsBytes).toList();
			cells.storage().createRecoveryLot(lease, cell.wardId(), serialized).whenComplete((lot, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
				if (error != null) {
					release();
					fail(job, "recovery lot could not be prepared", error);
					return;
				}
				cells.markPrepared(job, lot);
				clear(lot);
			}));
		}

		private void clear(long lotId) {
			try {
				for (Inventory inventory : inventories) inventory.clear();
				new Restore(job, cell, world, baseline, palette, lotId, chunks).runTaskTimer(plugin, 1, 1);
			} catch (RuntimeException exception) {
				release();
				fail(job, "cell contents could not be cleared", exception);
			}
		}

		private boolean differsFromBaseline(org.bukkit.block.Block block, int blockX, int blockY, int blockZ) {
			int paletteIndex = baseline.paletteIndex(
				blockX - cell.cuboid().minX,
				blockY - cell.cuboid().minY,
				blockZ - cell.cuboid().minZ
			);
			return !block.getBlockData().equals(palette.get(paletteIndex));
		}

		private void release() {
			chunks.forEach(chunk -> chunk.removePluginChunkTicket(plugin));
			active.remove(job.cellId());
		}
	}

	private static void collectBlockDrop(org.bukkit.block.Block block, List<ItemStack> items) {
		if (block.getType().isAir() || block.isLiquid()) return;
		if (block.getBlockData() instanceof Bisected bisected && bisected.getHalf() == Bisected.Half.TOP) return;
		if (block.getBlockData() instanceof Bed bed && bed.getPart() == Bed.Part.HEAD) return;
		if (block.getType().isItem()) items.add(new ItemStack(block.getType()));
		else items.addAll(block.getDrops());
	}

	private final class Restore extends BukkitRunnable {
		private final CellResetJob job;
		private final CellDefinition cell;
		private final World world;
		private final CellBaseline baseline;
		private final List<BlockData> palette;
		private final long lotId;
		private final List<Chunk> chunks;
		private int x;
		private int y;
		private int z;

		private Restore(
			CellResetJob job,
			CellDefinition cell,
			World world,
			CellBaseline baseline,
			List<BlockData> palette,
			long lotId,
			List<Chunk> chunks
		) {
			this.job = job;
			this.cell = cell;
			this.world = world;
			this.baseline = baseline;
			this.palette = palette;
			this.lotId = lotId;
			this.chunks = chunks;
			x = cell.cuboid().minX;
			y = cell.cuboid().minY;
			z = cell.cuboid().minZ;
		}

		@Override
		public void run() {
			try {
				int done = 0;
				while (done++ < BLOCKS_PER_TICK) {
					var block = world.getBlockAt(x, y, z);
					BlockData target = palette.get(baseline.paletteIndex(
						x - cell.cuboid().minX,
						y - cell.cuboid().minY,
						z - cell.cuboid().minZ
					));
					if (!block.getBlockData().equals(target)) block.setBlockData(target, false);
					if (!advance()) {
						cancel();
						cells.completeReset(job, lotId);
						chunks.forEach(chunk -> chunk.removePluginChunkTicket(plugin));
						active.remove(job.cellId());
						stateListener.accept(cells.get(job.cellId()));
						return;
					}
				}
			} catch (RuntimeException exception) {
				cancel();
				chunks.forEach(chunk -> chunk.removePluginChunkTicket(plugin));
				fail(job, "cell restoration failed", exception);
			}
		}

		private boolean advance() {
			if (++x <= cell.cuboid().maxX) return true;
			x = cell.cuboid().minX;
			if (++z <= cell.cuboid().maxZ) return true;
			z = cell.cuboid().minZ;
			return ++y <= cell.cuboid().maxY;
		}
	}
}
