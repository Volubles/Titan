package com.voluble.titanMC.cells.baseline;

import com.voluble.titanMC.cells.model.CellDefinition;
import com.voluble.titanMC.util.RegionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.TileState;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class CellBaselineCaptureService implements AutoCloseable {
	private static final int BLOCKS_PER_TICK = 2_000;
	private static final long MAX_NANOS_PER_TICK = 2_000_000L;

	private final Plugin plugin;
	private final Queue<Request> queued = new ArrayDeque<>();
	private Request loadingRequest;
	private ActiveCapture active;
	private boolean closed;

	public CellBaselineCaptureService(Plugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	public CompletableFuture<CellBaseline> capture(CellDefinition cell) {
		Objects.requireNonNull(cell, "cell");
		CompletableFuture<CellBaseline> result = new CompletableFuture<>();
		if (closed) {
			result.completeExceptionally(new IllegalStateException("Cell baseline capture service is closed"));
			return result;
		}
		queued.add(new Request(cell, result));
		startNext();
		return result;
	}

	private void startNext() {
		if (active != null || loadingRequest != null || closed) return;
		Request request = queued.poll();
		if (request == null) return;
		RegionUtils.Cuboid cuboid = request.cell().cuboid();
		long volume = (long) (cuboid.maxX - cuboid.minX + 1)
			* (cuboid.maxY - cuboid.minY + 1)
			* (cuboid.maxZ - cuboid.minZ + 1);
		if (volume > CellBaseline.MAX_BLOCKS) {
			request.result().completeExceptionally(
				new IllegalArgumentException("Cell exceeds " + CellBaseline.MAX_BLOCKS + " baseline blocks")
			);
			startNext();
			return;
		}
		World world = Bukkit.getWorld(cuboid.worldId);
		if (world == null) {
			request.result().completeExceptionally(new IllegalStateException("Cell world is unavailable"));
			startNext();
			return;
		}
		List<CompletableFuture<Chunk>> loading = new ArrayList<>();
		for (int chunkX = cuboid.minChunkX(); chunkX <= cuboid.maxChunkX(); chunkX++) {
			for (int chunkZ = cuboid.minChunkZ(); chunkZ <= cuboid.maxChunkZ(); chunkZ++) {
				loading.add(world.getChunkAtAsync(chunkX, chunkZ, true));
			}
		}
		loadingRequest = request;
		CompletableFuture.allOf(loading.toArray(CompletableFuture[]::new)).whenComplete((ignored, failure) ->
			Bukkit.getScheduler().runTask(plugin, () -> finishLoading(request, world, loading, failure))
		);
	}

	private void finishLoading(
		Request request,
		World world,
		List<CompletableFuture<Chunk>> loading,
		Throwable failure
	) {
		if (loadingRequest == request) loadingRequest = null;
		if (closed) {
			if (!request.result().isDone()) {
				request.result().completeExceptionally(new IllegalStateException("Cell baseline capture service is closed"));
			}
			return;
		}
		if (failure != null) {
			request.result().completeExceptionally(new IllegalStateException("Could not load cell chunks", failure));
			startNext();
			return;
		}
		List<Chunk> chunks = loading.stream().map(CompletableFuture::join).toList();
		chunks.forEach(chunk -> chunk.addPluginChunkTicket(plugin));
		active = new ActiveCapture(request, world, chunks);
		active.task = Bukkit.getScheduler().runTaskTimer(plugin, this::processActive, 1L, 1L);
	}

	private void processActive() {
		ActiveCapture capture = active;
		if (capture == null) return;
		long deadline = System.nanoTime() + MAX_NANOS_PER_TICK;
		try {
			int processed = 0;
			while (processed < BLOCKS_PER_TICK && System.nanoTime() < deadline) {
				var block = capture.world.getBlockAt(capture.x, capture.y, capture.z);
				if (block.getState() instanceof TileState) {
					throw new IllegalStateException(
						"Cell baseline contains unsupported block state " + block.getType()
							+ " at " + block.getX() + ", " + block.getY() + ", " + block.getZ()
					);
				}
				String blockData = block.getBlockData().getAsString();
				int paletteIndex = capture.palette.computeIfAbsent(blockData, ignored -> capture.palette.size());
				capture.blocks[capture.index++] = paletteIndex;
				processed++;
				if (!capture.advance()) {
					complete(capture);
					return;
				}
			}
		} catch (RuntimeException exception) {
			fail(capture, exception);
		}
	}

	private void complete(ActiveCapture capture) {
		CellBaseline baseline = new CellBaseline(
			capture.sizeX,
			capture.sizeY,
			capture.sizeZ,
			List.copyOf(capture.palette.keySet()),
			capture.blocks
		);
		cleanup(capture);
		capture.request.result().complete(baseline);
		startNext();
	}

	private void fail(ActiveCapture capture, RuntimeException exception) {
		cleanup(capture);
		capture.request.result().completeExceptionally(exception);
		plugin.getLogger().log(Level.WARNING, "Cell baseline capture failed for " + capture.request.cell().id(), exception);
		startNext();
	}

	private void cleanup(ActiveCapture capture) {
		if (capture.task != null) capture.task.cancel();
		capture.chunks.forEach(chunk -> chunk.removePluginChunkTicket(plugin));
		if (active == capture) active = null;
	}

	@Override
	public void close() {
		closed = true;
		if (active != null) {
			ActiveCapture capture = active;
			cleanup(capture);
			capture.request.result().completeExceptionally(new IllegalStateException("Plugin is shutting down"));
		}
		if (loadingRequest != null) {
			loadingRequest.result().completeExceptionally(new IllegalStateException("Plugin is shutting down"));
			loadingRequest = null;
		}
		Request request;
		while ((request = queued.poll()) != null) {
			request.result().completeExceptionally(new IllegalStateException("Plugin is shutting down"));
		}
	}

	private record Request(CellDefinition cell, CompletableFuture<CellBaseline> result) {
	}

	private static final class ActiveCapture {
		private final Request request;
		private final World world;
		private final List<Chunk> chunks;
		private final Map<String, Integer> palette = new LinkedHashMap<>();
		private final int sizeX;
		private final int sizeY;
		private final int sizeZ;
		private final int[] blocks;
		private int x;
		private int y;
		private int z;
		private int index;
		private BukkitTask task;

		private ActiveCapture(Request request, World world, List<Chunk> chunks) {
			this.request = request;
			this.world = world;
			this.chunks = chunks;
			RegionUtils.Cuboid cuboid = request.cell().cuboid();
			sizeX = cuboid.maxX - cuboid.minX + 1;
			sizeY = cuboid.maxY - cuboid.minY + 1;
			sizeZ = cuboid.maxZ - cuboid.minZ + 1;
			long volume = (long) sizeX * sizeY * sizeZ;
			if (volume > CellBaseline.MAX_BLOCKS) {
				throw new IllegalArgumentException("Cell exceeds " + CellBaseline.MAX_BLOCKS + " baseline blocks");
			}
			blocks = new int[Math.toIntExact(volume)];
			x = cuboid.minX;
			y = cuboid.minY;
			z = cuboid.minZ;
		}

		private boolean advance() {
			RegionUtils.Cuboid cuboid = request.cell().cuboid();
			if (++x <= cuboid.maxX) return true;
			x = cuboid.minX;
			if (++z <= cuboid.maxZ) return true;
			z = cuboid.minZ;
			return ++y <= cuboid.maxY;
		}
	}
}
