package com.voluble.titanMC.regions.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockBoxTest {

	@Test
	void inclusiveFactoryProducesHalfOpenBounds() {
		BlockBox box = BlockBox.inclusive(5, 8, 2, -3, 4, -9);

		assertEquals(new BlockBox(-3, 4, -9, 6, 9, 3), box);
		assertTrue(box.contains(-3, 4, -9));
		assertTrue(box.contains(5, 8, 2));
		assertFalse(box.contains(6, 8, 2));
		assertFalse(box.contains(5, 9, 2));
	}

	@Test
	void touchingHalfOpenBoxesDoNotOverlap() {
		BlockBox first = new BlockBox(0, 0, 0, 10, 10, 10);
		BlockBox touching = new BlockBox(10, 0, 0, 20, 10, 10);
		BlockBox overlapping = new BlockBox(9, 0, 0, 20, 10, 10);

		assertFalse(first.intersects(touching));
		assertTrue(first.intersects(overlapping));
	}

	@Test
	void rejectsEmptyAndOverflowingInclusiveBoxes() {
		assertThrows(IllegalArgumentException.class, () -> new BlockBox(1, 0, 0, 1, 1, 1));
		assertThrows(IllegalArgumentException.class, () -> BlockBox.inclusive(0, 0, 0, Integer.MAX_VALUE, 1, 1));
	}
}
