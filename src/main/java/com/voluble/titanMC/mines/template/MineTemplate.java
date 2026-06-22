package com.voluble.titanMC.mines.template;

import com.voluble.titanMC.mines.MineResetDefinition;

import java.util.List;
import java.util.Objects;

public final class MineTemplate {
	public static final int MAX_BLOCKS = 20_000_000;
	public static final int MAX_PALETTE_SIZE = 4_096;
	private final String id;
	private final int sizeX;
	private final int sizeY;
	private final int sizeZ;
	private final List<String> blockPalette;
	private final int[] blocks;

	public MineTemplate(String id, int sizeX, int sizeY, int sizeZ, List<String> blockPalette, int[] blocks) {
		this(id, sizeX, sizeY, sizeZ, blockPalette, blocks, false);
	}

	static MineTemplate takeOwnership(String id, int sizeX, int sizeY, int sizeZ, List<String> blockPalette, int[] blocks) {
		return new MineTemplate(id, sizeX, sizeY, sizeZ, blockPalette, blocks, true);
	}

	private MineTemplate(
		String id, int sizeX, int sizeY, int sizeZ, List<String> blockPalette, int[] blocks, boolean takeOwnership
	) {
		this.id = MineResetDefinition.normalizeTemplateId(id);
		if (sizeX < 1 || sizeY < 1 || sizeZ < 1) throw new IllegalArgumentException("Template dimensions must be positive");
		long volume = (long) sizeX * sizeY * sizeZ;
		if (volume > MAX_BLOCKS) throw new IllegalArgumentException("Template exceeds " + MAX_BLOCKS + " blocks");
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		this.blockPalette = List.copyOf(Objects.requireNonNull(blockPalette, "blockPalette"));
		if (this.blockPalette.isEmpty()) throw new IllegalArgumentException("Template palette must not be empty");
		if (this.blockPalette.size() > MAX_PALETTE_SIZE) throw new IllegalArgumentException("Template palette is too large");
		if (this.blockPalette.stream().anyMatch(value -> value == null || value.isBlank())) {
			throw new IllegalArgumentException("Template palette entries must not be blank");
		}
		Objects.requireNonNull(blocks, "blocks");
		if (blocks.length != volume) throw new IllegalArgumentException("Template block count does not match its dimensions");
		for (int block : blocks) {
			if (block < 0 || block >= this.blockPalette.size()) throw new IllegalArgumentException("Template contains an invalid palette index");
		}
		this.blocks = takeOwnership ? blocks : blocks.clone();
	}

	public String id() { return id; }
	public int sizeX() { return sizeX; }
	public int sizeY() { return sizeY; }
	public int sizeZ() { return sizeZ; }
	public List<String> blockPalette() { return blockPalette; }
	public int[] blocks() { return blocks.clone(); }
	int[] blocksView() { return blocks; }

	public int paletteIndex(int relativeX, int relativeY, int relativeZ) {
		if (relativeX < 0 || relativeX >= sizeX || relativeY < 0 || relativeY >= sizeY || relativeZ < 0 || relativeZ >= sizeZ) {
			throw new IndexOutOfBoundsException("Relative template position is outside its dimensions");
		}
		return blocks[(relativeY * sizeZ + relativeZ) * sizeX + relativeX];
	}
}
