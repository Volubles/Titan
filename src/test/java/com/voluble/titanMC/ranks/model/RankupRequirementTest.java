package com.voluble.titanMC.ranks.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankupRequirementTest {
	@Test
	void factoriesProduceExpectedValues() {
		RankupRequirement free = RankupRequirement.free();
		RankupRequirement paid = RankupRequirement.of(2_500L);
		RankupRequirement gated = RankupRequirement.of(1_000L, RankId.of("e1"));

		assertEquals(0L, free.cost());
		assertTrue(free.requires().isEmpty());
		assertEquals(2_500L, paid.cost());
		assertTrue(paid.requires().isEmpty());
		assertEquals(1_000L, gated.cost());
		assertEquals(Optional.of(RankId.of("e1")), gated.requires());
	}

	@Test
	void rejectsNegativeCost() {
		assertThrows(IllegalArgumentException.class, () -> new RankupRequirement(-1L, Optional.empty()));
	}

	@Test
	void rejectsNullRequiresOptional() {
		assertThrows(NullPointerException.class, () -> new RankupRequirement(0L, null));
	}

	@Test
	void rejectsNullRequiredRank() {
		assertThrows(NullPointerException.class, () -> RankupRequirement.of(100L, null));
	}

	@Test
	void valueSemantics() {
		RankupRequirement a = RankupRequirement.of(500L, RankId.of("e2"));
		RankupRequirement b = RankupRequirement.of(500L, RankId.of("e2"));

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertSame(a.requires().get(), a.requires().get());
	}
}
