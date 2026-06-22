package com.voluble.titanMC.mines.template;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MineTemplateStorageTest {
	@TempDir Path directory;

	@Test
	void compressedTemplateRoundTripPreservesAirAndBlockData() throws Exception {
		int[] blocks = new int[16 * 8 * 16];
		java.util.Arrays.fill(blocks, 0);
		for (int index = 0; index < blocks.length; index += 23) blocks[index] = 1;
		MineTemplate expected = new MineTemplate(
			"woodfarm_v1", 16, 8, 16,
			List.of("minecraft:air", "minecraft:oak_log[axis=x]"), blocks
		);

		try (MineTemplateStorage storage = new MineTemplateStorage(directory)) {
			storage.save(expected).join();
			MineTemplate loaded = storage.load(expected.id()).join();

			assertEquals(expected.id(), loaded.id());
			assertEquals(expected.blockPalette(), loaded.blockPalette());
			assertArrayEquals(expected.blocks(), loaded.blocks());
			assertTrue(Files.size(directory.resolve("woodfarm_v1.tmt")) < blocks.length * Integer.BYTES);
		}
	}
}
