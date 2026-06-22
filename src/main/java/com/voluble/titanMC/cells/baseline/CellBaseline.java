package com.voluble.titanMC.cells.baseline;

import java.util.List;
import java.util.Objects;

public final class CellBaseline {
	public static final int MAX_BLOCKS = 20_000_000;
	public static final int MAX_PALETTE_SIZE = 4_096;

	private final int sizeX;
	private final int sizeY;
	private final int sizeZ;
	private final List<String> blockPalette;
	private final int[] blocks;

	public CellBaseline(int sizeX, int sizeY, int sizeZ, List<String> blockPalette, int[] blocks) {
		this(sizeX, sizeY, sizeZ, blockPalette, blocks, false);
	}

	static CellBaseline takeOwnership(int sizeX, int sizeY, int sizeZ, List<String> blockPalette, int[] blocks) {
		return new CellBaseline(sizeX, sizeY, sizeZ, blockPalette, blocks, true);
	}

	private CellBaseline(
		int sizeX,
		int sizeY,
		int sizeZ,
		List<String> blockPalette,
		int[] blocks,
		boolean takeOwnership
	) {
		if (sizeX < 1 || sizeY < 1 || sizeZ < 1) {
			throw new IllegalArgumentException("Cell baseline dimensions must be positive");
		}
		long volume = (long) sizeX * sizeY * sizeZ;
		if (volume > MAX_BLOCKS) {
			throw new IllegalArgumentException("Cell baseline exceeds " + MAX_BLOCKS + " blocks");
		}
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		this.blockPalette = List.copyOf(Objects.requireNonNull(blockPalette, "blockPalette"));
		if (this.blockPalette.isEmpty()) throw new IllegalArgumentException("Cell baseline palette must not be empty");
		if (this.blockPalette.size() > MAX_PALETTE_SIZE) {
			throw new IllegalArgumentException("Cell baseline palette is too large");
		}
		if (this.blockPalette.stream().anyMatch(value -> value == null || value.isBlank())) {
			throw new IllegalArgumentException("Cell baseline palette entries must not be blank");
		}
		Objects.requireNonNull(blocks, "blocks");
		if (blocks.length != volume) {
			throw new IllegalArgumentException("Cell baseline block count does not match its dimensions");
		}
		for (int block : blocks) {
			if (block < 0 || block >= this.blockPalette.size()) {
				throw new IllegalArgumentException("Cell baseline contains an invalid palette index");
			}
		}
		this.blocks = takeOwnership ? blocks : blocks.clone();
	}

	public int sizeX() {
		return sizeX;
	}

	public int sizeY() {
		return sizeY;
	}

	public int sizeZ() {
		return sizeZ;
	}

	public List<String> blockPalette() {
		return blockPalette;
	}

	public int[] blocks() {
		return blocks.clone();
	}

	int[] blocksView() {
		return blocks;
	}

	public int paletteIndex(int relativeX, int relativeY, int relativeZ) {
		if (relativeX < 0 || relativeX >= sizeX
			|| relativeY < 0 || relativeY >= sizeY
			|| relativeZ < 0 || relativeZ >= sizeZ) {
			throw new IndexOutOfBoundsException("Relative position is outside the cell baseline");
		}
		return blocks[(relativeY * sizeZ + relativeZ) * sizeX + relativeX];
	}
}
