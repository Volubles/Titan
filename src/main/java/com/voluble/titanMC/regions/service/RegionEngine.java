package com.voluble.titanMC.regions.service;

import com.voluble.titanMC.regions.index.RegionIndex;
import com.voluble.titanMC.regions.index.RegionIndexBuildException;
import com.voluble.titanMC.regions.index.RegionIndexOptions;
import com.voluble.titanMC.regions.index.RegionIndexSnapshot;
import com.voluble.titanMC.regions.model.BlockBox;
import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.RegionId;
import com.voluble.titanMC.regions.model.RegionKey;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.persistence.RegionRepository;
import com.voluble.titanMC.regions.persistence.RegionStorageException;
import com.voluble.titanMC.regions.persistence.SqliteRegionRepository;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RegionEngine implements AutoCloseable {

	private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(10);
	private final RegionRepository repository;
	private final RegionIndexOptions options;
	private final RegionIndex index;
	private final ExecutorService writer;
	private final AtomicBoolean closed = new AtomicBoolean();

	private RegionEngine(RegionRepository repository, RegionIndexOptions options, RegionIndex index) {
		this.repository = repository;
		this.options = options;
		this.index = index;
		this.writer = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("titan-region-writer").factory());
	}

	public static RegionEngine open(Path databasePath) throws RegionStorageException {
		return open(new SqliteRegionRepository(databasePath), RegionIndexOptions.defaults());
	}

	public static RegionEngine open(RegionRepository repository, RegionIndexOptions options) throws RegionStorageException {
		Objects.requireNonNull(repository, "repository");
		Objects.requireNonNull(options, "options");
		try {
			repository.initialize();
			List<RegionDefinition> definitions = repository.loadAll();
			RegionIndex index = new RegionIndex();
			RegionIndexSnapshot initial = RegionIndexSnapshot.build(1L, definitions, options);
			if (!index.publish(index.snapshot(), initial)) {
				throw new IllegalStateException("Failed to publish initial region snapshot");
			}
			return new RegionEngine(repository, options, index);
		} catch (SQLException | RegionIndexBuildException | RuntimeException exception) {
			try {
				repository.close();
			} catch (Exception suppressed) {
				exception.addSuppressed(suppressed);
			}
			throw new RegionStorageException("Failed to initialize Titan Region Engine", exception);
		}
	}

	public RegionIndexSnapshot snapshot() {
		return index.snapshot();
	}

	public RegionDefinition find(RegionId id) {
		return index.find(id);
	}

	public RegionDefinition find(WorldId worldId, RegionKey key) {
		return index.find(worldId, key);
	}

	public List<RegionDefinition> findAll(WorldId worldId, int x, int y, int z) {
		return index.findAll(worldId, x, y, z);
	}

	public List<RegionDefinition> findIntersecting(WorldId worldId, BlockBox box) {
		return index.findIntersecting(worldId, box);
	}

	public CompletableFuture<RegionMutationResult> create(
		RegionKey key,
		WorldId worldId,
		int priority,
		Collection<BlockBox> boxes
	) {
		RegionDefinition definition;
		try {
			definition = RegionDefinition.create(key, worldId, priority, List.copyOf(boxes));
		} catch (RuntimeException exception) {
			return CompletableFuture.completedFuture(new RegionMutationResult.Failure(
				RegionMutationResult.Reason.INVALID_GEOMETRY, exception.getMessage()
			));
		}
		return submit(() -> saveMutation(definition, true));
	}

	public CompletableFuture<RegionMutationResult> update(
		RegionId id,
		RegionKey key,
		WorldId worldId,
		int priority,
		Collection<BlockBox> boxes
	) {
		return submit(() -> {
			RegionDefinition existing = index.find(id);
			if (existing == null) return failure(RegionMutationResult.Reason.NOT_FOUND, "Region does not exist: " + id);
			RegionDefinition updated;
			try {
				updated = new RegionDefinition(id, key, worldId, priority, List.copyOf(boxes), existing.createdAt(), Instant.now());
			} catch (RuntimeException exception) {
				return failure(RegionMutationResult.Reason.INVALID_GEOMETRY, exception.getMessage());
			}
			return saveMutation(updated, false);
		});
	}

	public CompletableFuture<RegionMutationResult> delete(RegionId id) {
		return submit(() -> {
			RegionIndexSnapshot active = index.snapshot();
			RegionDefinition existing = active.find(id);
			if (existing == null) return failure(RegionMutationResult.Reason.NOT_FOUND, "Region does not exist: " + id);
			Map<RegionId, RegionDefinition> definitions = mutableDefinitions(active);
			definitions.remove(id);
			RegionIndexSnapshot replacement;
			try {
				replacement = RegionIndexSnapshot.build(active.version() + 1L, definitions.values(), options);
			} catch (RegionIndexBuildException exception) {
				return failure(RegionMutationResult.Reason.INVALID_GEOMETRY, exception.getMessage());
			}
			try {
				repository.delete(id);
			} catch (SQLException exception) {
				return failure(RegionMutationResult.Reason.STORAGE_FAILURE, exception.getMessage());
			}
			if (!index.publish(active, replacement)) {
				return failure(RegionMutationResult.Reason.INTERNAL_CONFLICT, "Snapshot changed outside the mutation writer");
			}
			return new RegionMutationResult.Success(existing);
		});
	}

	private RegionMutationResult saveMutation(RegionDefinition definition, boolean createOnly) {
		RegionIndexSnapshot active = index.snapshot();
		RegionDefinition sameKey = active.find(definition.worldId(), definition.key());
		if (sameKey != null && (!sameKey.id().equals(definition.id()) || createOnly)) {
			return failure(RegionMutationResult.Reason.DUPLICATE_KEY, "Region key already exists: " + definition.key());
		}
		Map<RegionId, RegionDefinition> definitions = mutableDefinitions(active);
		definitions.put(definition.id(), definition);
		RegionIndexSnapshot replacement;
		try {
			replacement = RegionIndexSnapshot.build(active.version() + 1L, definitions.values(), options);
		} catch (RegionIndexBuildException exception) {
			return failure(RegionMutationResult.Reason.INVALID_GEOMETRY, exception.getMessage());
		}
		try {
			repository.save(definition);
		} catch (SQLException exception) {
			return failure(RegionMutationResult.Reason.STORAGE_FAILURE, exception.getMessage());
		}
		if (!index.publish(active, replacement)) {
			return failure(RegionMutationResult.Reason.INTERNAL_CONFLICT, "Snapshot changed outside the mutation writer");
		}
		return new RegionMutationResult.Success(definition);
	}

	private CompletableFuture<RegionMutationResult> submit(Mutation mutation) {
		if (closed.get()) return CompletableFuture.completedFuture(failure(RegionMutationResult.Reason.ENGINE_CLOSED, "Region engine is closed"));
		try {
			return CompletableFuture.supplyAsync(() -> {
				try {
					return mutation.run();
				} catch (RuntimeException exception) {
					return failure(RegionMutationResult.Reason.INTERNAL_CONFLICT, exception.getMessage());
				}
			}, writer);
		} catch (RejectedExecutionException exception) {
			return CompletableFuture.completedFuture(failure(RegionMutationResult.Reason.ENGINE_CLOSED, "Region engine is closed"));
		}
	}

	private static Map<RegionId, RegionDefinition> mutableDefinitions(RegionIndexSnapshot snapshot) {
		Map<RegionId, RegionDefinition> definitions = new LinkedHashMap<>();
		for (RegionDefinition definition : snapshot.definitions()) definitions.put(definition.id(), definition);
		return definitions;
	}

	private static RegionMutationResult.Failure failure(RegionMutationResult.Reason reason, String message) {
		return new RegionMutationResult.Failure(reason, message == null ? reason.name() : message);
	}

	@Override
	public void close() throws RegionStorageException {
		if (!closed.compareAndSet(false, true)) return;
		writer.shutdown();
		try {
			if (!writer.awaitTermination(CLOSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
				writer.shutdownNow();
				if (!writer.awaitTermination(CLOSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
					throw new RegionStorageException("Region writer did not terminate", null);
				}
			}
			repository.close();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			writer.shutdownNow();
			throw new RegionStorageException("Interrupted while closing region engine", exception);
		} catch (SQLException exception) {
			throw new RegionStorageException("Failed to close region repository", exception);
		}
	}

	@FunctionalInterface
	private interface Mutation {
		RegionMutationResult run();
	}
}
