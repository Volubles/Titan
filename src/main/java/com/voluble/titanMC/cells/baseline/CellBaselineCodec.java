package com.voluble.titanMC.cells.baseline;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class CellBaselineCodec {
	private static final int MAGIC = 0x54434200;
	private static final int VERSION = 1;

	public byte[] encode(CellBaseline baseline) {
		try {
			ByteArrayOutputStream target = new ByteArrayOutputStream();
			try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(target))) {
				output.writeInt(MAGIC);
				output.writeShort(VERSION);
				output.writeInt(baseline.sizeX());
				output.writeInt(baseline.sizeY());
				output.writeInt(baseline.sizeZ());
				output.writeInt(baseline.blockPalette().size());
				for (String blockData : baseline.blockPalette()) output.writeUTF(blockData);
				writeRuns(output, baseline.blocksView());
			}
			return target.toByteArray();
		} catch (IOException exception) {
			throw new IllegalStateException("Could not encode cell baseline", exception);
		}
	}

	public CellBaseline decode(byte[] encoded) throws IOException {
		try (DataInputStream input = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(encoded)))) {
			if (input.readInt() != MAGIC) throw new IOException("Not a Titan cell baseline");
			int version = input.readUnsignedShort();
			if (version != VERSION) throw new IOException("Unsupported cell baseline version " + version);
			int sizeX = input.readInt();
			int sizeY = input.readInt();
			int sizeZ = input.readInt();
			long volume = (long) sizeX * sizeY * sizeZ;
			if (sizeX < 1 || sizeY < 1 || sizeZ < 1 || volume > CellBaseline.MAX_BLOCKS) {
				throw new IOException("Invalid cell baseline dimensions");
			}
			int paletteSize = input.readInt();
			if (paletteSize < 1 || paletteSize > CellBaseline.MAX_PALETTE_SIZE) {
				throw new IOException("Invalid cell baseline palette size");
			}
			List<String> palette = new ArrayList<>(paletteSize);
			for (int index = 0; index < paletteSize; index++) palette.add(input.readUTF());
			int[] blocks = readRuns(input, Math.toIntExact(volume), paletteSize);
			return CellBaseline.takeOwnership(sizeX, sizeY, sizeZ, palette, blocks);
		} catch (IllegalArgumentException exception) {
			throw new IOException("Invalid cell baseline data", exception);
		}
	}

	private static void writeRuns(DataOutputStream output, int[] blocks) throws IOException {
		int index = 0;
		while (index < blocks.length) {
			int paletteIndex = blocks[index];
			int end = index + 1;
			while (end < blocks.length && blocks[end] == paletteIndex) end++;
			writeVarInt(output, paletteIndex);
			writeVarInt(output, end - index);
			index = end;
		}
	}

	private static int[] readRuns(DataInputStream input, int volume, int paletteSize) throws IOException {
		int[] blocks = new int[volume];
		int offset = 0;
		while (offset < volume) {
			int paletteIndex = readVarInt(input);
			int length = readVarInt(input);
			if (paletteIndex < 0 || paletteIndex >= paletteSize || length < 1 || length > volume - offset) {
				throw new IOException("Invalid cell baseline block run");
			}
			Arrays.fill(blocks, offset, offset + length, paletteIndex);
			offset += length;
		}
		return blocks;
	}

	private static void writeVarInt(DataOutputStream output, int value) throws IOException {
		do {
			int part = value & 0x7f;
			value >>>= 7;
			output.writeByte(value == 0 ? part : part | 0x80);
		} while (value != 0);
	}

	private static int readVarInt(DataInputStream input) throws IOException {
		int result = 0;
		for (int shift = 0; shift < 35; shift += 7) {
			int part = input.readUnsignedByte();
			result |= (part & 0x7f) << shift;
			if ((part & 0x80) == 0) return result;
		}
		throw new IOException("Cell baseline varint is too long");
	}
}
