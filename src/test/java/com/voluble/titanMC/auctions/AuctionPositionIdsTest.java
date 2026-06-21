package com.voluble.titanMC.auctions;

import com.voluble.titanMC.ranks.model.WardId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionPositionIdsTest {
	@Test
	void startsAtOneWithinWard() {
		assertEquals("e-001", AuctionPositionIds.next(WardId.of("e"), List.of()));
	}

	@Test
	void usesFirstAvailableIndexWithinWard() {
		assertEquals(
			"e-002",
			AuctionPositionIds.next(WardId.of("e"), List.of("e-001", "e-003", "d-002"))
		);
	}

	@Test
	void supportsMoreThanThreeDigits() {
		List<String> existing = java.util.stream.IntStream.rangeClosed(1, 999)
			.mapToObj(index -> "d-" + String.format(java.util.Locale.ROOT, "%03d", index))
			.toList();

		assertEquals("d-1000", AuctionPositionIds.next(WardId.of("d"), existing));
	}
}
