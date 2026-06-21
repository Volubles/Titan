package com.voluble.titanMC.mines;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedPaletteTest {

	@Test
	void compiledPalettePreservesWeightedSelectionBoundaries() {
		WeightedPalette palette = new WeightedPalette();
		palette.addOrUpdate(Material.STONE, 2);
		palette.addOrUpdate(Material.DIAMOND_ORE, 1);

		Random random = new Random() {
			private int value;
			@Override public int nextInt(int bound) {
				return value++ % bound;
			}
		};

		assertEquals(Material.STONE, palette.pickRandom(random));
		assertEquals(Material.STONE, palette.pickRandom(random));
		assertEquals(Material.DIAMOND_ORE, palette.pickRandom(random));
	}

	@Test
	void updatesRecompileThePalette() {
		WeightedPalette palette = new WeightedPalette();
		palette.addOrUpdate(Material.STONE, 1);
		palette.addOrUpdate(Material.DIAMOND_ORE, 1);
		palette.remove(Material.STONE);

		for (int index = 0; index < 20; index++) {
			assertEquals(Material.DIAMOND_ORE, palette.pickRandom(new Random(index)));
		}
		assertTrue(palette.getEntriesView().containsKey(Material.DIAMOND_ORE));
	}
}
