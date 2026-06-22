package com.voluble.titanMC.mines.template;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MineTemplateFormatBenchmark {
	private MineTemplateFormatBenchmark() {
	}

	public static void main(String[] args) throws Exception {
		benchmark("sparse woodfarm", sparseWoodfarm());
		benchmark("mixed mine", mixedMine());
	}

	private static void benchmark(String name, MineTemplate template) throws Exception {
		MineTemplateCodec codec = new MineTemplateCodec();
		long writeStarted = System.nanoTime();
		ByteArrayOutputStream encoded = new ByteArrayOutputStream();
		codec.write(template, encoded);
		long writeNanos = System.nanoTime() - writeStarted;
		long readStarted = System.nanoTime();
		MineTemplate decoded = codec.read(new ByteArrayInputStream(encoded.toByteArray()));
		long readNanos = System.nanoTime() - readStarted;
		System.out.printf(
			"%s: %,d blocks, %,d bytes, write %.2f ms, read %.2f ms%n",
			name, decoded.blocksView().length, encoded.size(), writeNanos / 1_000_000.0, readNanos / 1_000_000.0
		);
	}

	private static MineTemplate sparseWoodfarm() {
		int sizeX = 160;
		int sizeY = 40;
		int sizeZ = 160;
		int[] blocks = new int[sizeX * sizeY * sizeZ];
		for (int house = 0; house < 25; house++) {
			int originX = 5 + (house % 5) * 30;
			int originZ = 5 + (house / 5) * 30;
			for (int y = 0; y < 10; y++) for (int z = 0; z < 12; z++) for (int x = 0; x < 12; x++) {
				boolean shell = y == 0 || y == 9 || x == 0 || x == 11 || z == 0 || z == 11;
				if (shell) blocks[(y * sizeZ + originZ + z) * sizeX + originX + x] = 1;
			}
		}
		return MineTemplate.takeOwnership("woodfarm_benchmark", sizeX, sizeY, sizeZ,
			List.of("minecraft:air", "minecraft:oak_planks"), blocks);
	}

	private static MineTemplate mixedMine() {
		int sizeX = 100;
		int sizeY = 50;
		int sizeZ = 100;
		int[] blocks = new int[sizeX * sizeY * sizeZ];
		for (int index = 0; index < blocks.length; index++) blocks[index] = ThreadLocalRandom.current().nextInt(4);
		return MineTemplate.takeOwnership("mixed_benchmark", sizeX, sizeY, sizeZ,
			List.of("minecraft:stone", "minecraft:coal_ore", "minecraft:iron_ore", "minecraft:air"), blocks);
	}
}
