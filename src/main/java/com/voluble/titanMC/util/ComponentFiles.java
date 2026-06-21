package com.voluble.titanMC.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ComponentFiles {
	private static final String[] SQLITE_SUFFIXES = {"", "-wal", "-shm"};

	private ComponentFiles() {
	}

	public static Path resolve(Path dataFolder, String component, String fileName) {
		Path componentFolder = dataFolder.resolve(component);
		Path target = componentFolder.resolve(fileName);
		try {
			Files.createDirectories(componentFolder);
			migrateLegacyFiles(target, dataFolder.resolve(fileName));
			return target;
		} catch (IOException exception) {
			throw new IllegalStateException("Could not prepare " + component + "/" + fileName, exception);
		}
	}

	public static Path resolveData(Path dataFolder, String component, String fileName) {
		Path componentFolder = dataFolder.resolve(component);
		Path targetFolder = componentFolder.resolve("data");
		Path target = targetFolder.resolve(fileName);
		try {
			Files.createDirectories(targetFolder);
			migrateLegacyFiles(target, componentFolder.resolve(fileName), dataFolder.resolve(fileName));
			return target;
		} catch (IOException exception) {
			throw new IllegalStateException("Could not prepare " + component + "/data/" + fileName, exception);
		}
	}

	private static void migrateLegacyFiles(Path target, Path... legacyCandidates) throws IOException {
		if (Files.exists(target)) return;
		for (Path legacy : legacyCandidates) {
			if (!Files.exists(legacy)) continue;
			moveFileGroup(legacy, target);
			return;
		}
	}

	private static void moveFileGroup(Path legacy, Path target) throws IOException {
		for (String suffix : SQLITE_SUFFIXES) {
			Path sourceFile = Path.of(legacy + suffix);
			Path targetFile = Path.of(target + suffix);
			if (Files.exists(sourceFile) && Files.notExists(targetFile)) {
				Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}
}
