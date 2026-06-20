package com.voluble.titanMC.regions.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionTextSetTest {

	@Test
	void storesTrimsAndRemovesTypedTextFlags() {
		RegionTextSet text = RegionTextSet.empty()
			.with(RegionTextFlag.ENTRY_MESSAGE, "  Welcome!  ");

		assertEquals("Welcome!", text.value(RegionTextFlag.ENTRY_MESSAGE).orElseThrow());
		assertFalse(text.with(RegionTextFlag.ENTRY_MESSAGE, null)
			.value(RegionTextFlag.ENTRY_MESSAGE).isPresent());
	}

	@Test
	void rejectsBlankAndOversizedMessages() {
		assertThrows(
			IllegalArgumentException.class,
			() -> RegionTextSet.of(Map.of(RegionTextFlag.EXIT_MESSAGE, "   "))
		);
		assertThrows(
			IllegalArgumentException.class,
			() -> RegionTextSet.empty().with(RegionTextFlag.EXIT_MESSAGE, "x".repeat(513))
		);
	}
}
