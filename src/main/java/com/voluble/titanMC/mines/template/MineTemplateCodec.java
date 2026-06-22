package com.voluble.titanMC.mines.template;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class MineTemplateCodec {
	private static final int MAGIC = 0x544d5400;
	private static final int VERSION = 1;

	void write(MineTemplate template, OutputStream target) throws IOException {
		try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(target))) {
			output.writeInt(MAGIC);
			output.writeShort(VERSION);
			output.writeUTF(template.id());
			output.writeInt(template.sizeX());
			output.writeInt(template.sizeY());
			output.writeInt(template.sizeZ());
			output.writeInt(template.blockPalette().size());
			for (String blockData : template.blockPalette()) output.writeUTF(blockData);
			writeRuns(output, template.blocksView());
		}
	}

	MineTemplate read(InputStream source) throws IOException {
		try (DataInputStream input = new DataInputStream(new GZIPInputStream(source))) {
			if (input.readInt() != MAGIC) throw new IOException("Not a Titan mine template");
			int version = input.readUnsignedShort();
			if (version != VERSION) throw new IOException("Unsupported mine template version " + version);
			String id = input.readUTF();
			int sizeX = input.readInt();
			int sizeY = input.readInt();
			int sizeZ = input.readInt();
			long volume = (long) sizeX * sizeY * sizeZ;
			if (sizeX < 1 || sizeY < 1 || sizeZ < 1 || volume > MineTemplate.MAX_BLOCKS) {
				throw new IOException("Invalid mine template dimensions");
			}
			int paletteSize = input.readInt();
			if (paletteSize < 1 || paletteSize > MineTemplate.MAX_PALETTE_SIZE) throw new IOException("Invalid template palette size");
			List<String> palette = new ArrayList<>(paletteSize);
			for (int index = 0; index < paletteSize; index++) palette.add(input.readUTF());
			int[] blocks = readRuns(input, Math.toIntExact(volume), paletteSize);
			return new MineTemplate(id, sizeX, sizeY, sizeZ, palette, blocks);
		} catch (IllegalArgumentException exception) {
			throw new IOException("Invalid mine template data", exception);
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
				throw new IOException("Invalid template block run");
			}
			java.util.Arrays.fill(blocks, offset, offset + length, paletteIndex);
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
		throw new IOException("Template varint is too long");
	}
}
