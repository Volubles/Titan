package com.voluble.titanMC.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentFilesTest {
	@TempDir
	Path dataFolder;

	@Test
	void movesLegacyFileIntoComponentFolder() throws Exception {
		Path legacy = dataFolder.resolve("cells.yml");
		Files.writeString(legacy, "value: legacy");

		Path resolved = ComponentFiles.resolve(dataFolder, "cells", "cells.yml");

		assertEquals(dataFolder.resolve("cells/cells.yml"), resolved);
		assertEquals("value: legacy", Files.readString(resolved));
		assertFalse(Files.exists(legacy));
	}

	@Test
	void movesSqliteSidecarsWithDatabase() throws Exception {
		Files.writeString(dataFolder.resolve("regions.db"), "database");
		Files.writeString(dataFolder.resolve("regions.db-wal"), "wal");
		Files.writeString(dataFolder.resolve("regions.db-shm"), "shm");

		ComponentFiles.resolve(dataFolder, "regions", "regions.db");

		assertEquals("database", Files.readString(dataFolder.resolve("regions/regions.db")));
		assertEquals("wal", Files.readString(dataFolder.resolve("regions/regions.db-wal")));
		assertEquals("shm", Files.readString(dataFolder.resolve("regions/regions.db-shm")));
	}

	@Test
	void doesNotOverwriteExistingComponentFile() throws Exception {
		Path target = dataFolder.resolve("mines/mines.yml");
		Files.createDirectories(target.getParent());
		Files.writeString(target, "value: current");
		Path legacy = dataFolder.resolve("mines.yml");
		Files.writeString(legacy, "value: legacy");

		ComponentFiles.resolve(dataFolder, "mines", "mines.yml");

		assertEquals("value: current", Files.readString(target));
		assertTrue(Files.exists(legacy));
	}

	@Test
	void doesNotMixLegacySidecarsWithExistingDatabase() throws Exception {
		Path target = dataFolder.resolve("regions/regions.db");
		Files.createDirectories(target.getParent());
		Files.writeString(target, "current database");
		Files.writeString(dataFolder.resolve("regions.db"), "legacy database");
		Files.writeString(dataFolder.resolve("regions.db-wal"), "legacy wal");

		ComponentFiles.resolve(dataFolder, "regions", "regions.db");

		assertEquals("current database", Files.readString(target));
		assertFalse(Files.exists(dataFolder.resolve("regions/regions.db-wal")));
		assertTrue(Files.exists(dataFolder.resolve("regions.db")));
		assertTrue(Files.exists(dataFolder.resolve("regions.db-wal")));
	}
}
