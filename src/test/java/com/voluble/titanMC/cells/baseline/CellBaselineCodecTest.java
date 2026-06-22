package com.voluble.titanMC.cells.baseline;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CellBaselineCodecTest {
	private final CellBaselineCodec codec = new CellBaselineCodec();

	@Test
	void preservesDimensionsPaletteAndBlocks() throws Exception {
		CellBaseline baseline = new CellBaseline(
			3,
			2,
			2,
			List.of("minecraft:air", "minecraft:stone", "minecraft:oak_stairs[facing=north]"),
			new int[]{0, 0, 1, 1, 1, 2, 2, 2, 0, 0, 0, 1}
		);

		CellBaseline decoded = codec.decode(codec.encode(baseline));

		assertEquals(3, decoded.sizeX());
		assertEquals(2, decoded.sizeY());
		assertEquals(2, decoded.sizeZ());
		assertEquals(baseline.blockPalette(), decoded.blockPalette());
		assertArrayEquals(baseline.blocks(), decoded.blocks());
	}

	@Test
	void rejectsDataFromAnotherFormat() {
		assertThrows(IOException.class, () -> codec.decode(new byte[]{1, 2, 3}));
	}

	@Test
	void rejectsInvalidPaletteIndexes() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new CellBaseline(1, 1, 1, List.of("minecraft:air"), new int[]{1})
		);
	}
}
