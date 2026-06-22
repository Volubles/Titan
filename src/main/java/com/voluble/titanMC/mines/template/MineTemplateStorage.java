package com.voluble.titanMC.mines.template;

import com.voluble.titanMC.mines.MineResetDefinition;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MineTemplateStorage implements AutoCloseable {
	private final Path directory;
	private final MineTemplateCodec codec = new MineTemplateCodec();
	private final ExecutorService io = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("titan-mine-template-io").factory());

	public MineTemplateStorage(Path directory) {
		this.directory = Objects.requireNonNull(directory, "directory").toAbsolutePath();
		try {
			Files.createDirectories(this.directory);
		} catch (IOException exception) {
			throw new IllegalStateException("Could not create mine template directory", exception);
		}
	}

	public CompletableFuture<MineTemplate> load(String id) {
		Path path = path(id);
		return CompletableFuture.supplyAsync(() -> {
			try (var input = Files.newInputStream(path)) {
				MineTemplate template = codec.read(input);
				if (!template.id().equals(MineResetDefinition.normalizeTemplateId(id))) {
					throw new IOException("Template id does not match its file name");
				}
				return template;
			} catch (IOException exception) {
				throw new IllegalStateException("Could not load mine template " + id, exception);
			}
		}, io);
	}

	public CompletableFuture<Void> save(MineTemplate template) {
		Objects.requireNonNull(template, "template");
		return CompletableFuture.runAsync(() -> write(template), io);
	}

	public boolean exists(String id) {
		return Files.isRegularFile(path(id));
	}

	private void write(MineTemplate template) {
		Path target = path(template.id());
		Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
		try (var output = Files.newOutputStream(temporary)) {
			codec.write(template, output);
		} catch (IOException exception) {
			try {
				Files.deleteIfExists(temporary);
			} catch (IOException cleanupFailure) {
				exception.addSuppressed(cleanupFailure);
			}
			throw new IllegalStateException("Could not write mine template " + template.id(), exception);
		}
		try {
			try {
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException ignored) {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			try {
				Files.deleteIfExists(temporary);
			} catch (IOException cleanupFailure) {
				exception.addSuppressed(cleanupFailure);
			}
			throw new IllegalStateException("Could not install mine template " + template.id(), exception);
		}
	}

	private Path path(String id) {
		return directory.resolve(MineResetDefinition.normalizeTemplateId(id) + ".tmt");
	}

	@Override
	public void close() {
		io.shutdown();
		try {
			if (!io.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) io.shutdownNow();
		} catch (InterruptedException exception) {
			io.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}
