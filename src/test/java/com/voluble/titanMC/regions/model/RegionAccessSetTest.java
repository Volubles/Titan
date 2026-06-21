package com.voluble.titanMC.regions.model;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionAccessSetTest {

	@Test
	void ownersImplicitlyMatchMembersWithoutDuplicatingStorage() {
		UUID owner = UUID.randomUUID();
		RegionAccessSet access = RegionAccessSet.of(Set.of(owner), Set.of(owner));

		assertTrue(access.isOwner(owner));
		assertTrue(access.isMember(owner));
		assertFalse(access.members().contains(owner));
	}
}
