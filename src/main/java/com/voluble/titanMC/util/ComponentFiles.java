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
			migrateLegacyFiles(dataFolder.resolve(fileName), target);
			return target;
		} catch (IOException exception) {
			throw new IllegalStateException("Could not prepare " + component + "/" + fileName, exception);
		}
	}

	private static void migrateLegacyFiles(Path legacy, Path target) throws IOException {
		if (Files.exists(target)) return;
		for (String suffix : SQLITE_SUFFIXES) {
			Path sourceFile = Path.of(legacy + suffix);
			Path targetFile = Path.of(target + suffix);
			if (Files.exists(sourceFile) && Files.notExists(targetFile)) {
				Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}
}
