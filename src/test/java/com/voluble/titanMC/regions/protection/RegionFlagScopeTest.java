package com.voluble.titanMC.regions.protection;

import com.voluble.titanMC.regions.model.RegionAccessSet;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.RegionFlagSet;
import com.voluble.titanMC.regions.protection.model.RegionSubject;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionFlagScopeTest {

	@Test
	void moreSpecificSubjectsOverrideEveryoneAndDenyWinsBetweenGroups() {
		UUID owner = UUID.randomUUID();
		RegionAccessSet access = RegionAccessSet.of(Set.of(owner), Set.of());
		RegionFlagSet flags = RegionFlagSet.empty()
			.with(ProtectionAction.BLOCK_BREAK, RegionSubject.EVERYONE, ProtectionDecision.DENY)
			.with(ProtectionAction.BLOCK_BREAK, RegionSubject.MEMBERS, ProtectionDecision.ALLOW)
			.with(ProtectionAction.BLOCK_BREAK, RegionSubject.group("vip"), ProtectionDecision.ALLOW)
			.with(ProtectionAction.BLOCK_BREAK, RegionSubject.group("restricted"), ProtectionDecision.DENY);

		assertEquals(
			ProtectionDecision.ALLOW,
			flags.resolve(ProtectionAction.BLOCK_BREAK, access, owner, ignored -> false)
				.orElseThrow().decision()
		);
		assertEquals(
			ProtectionDecision.DENY,
			flags.resolve(
				ProtectionAction.BLOCK_BREAK,
				RegionAccessSet.empty(),
				UUID.randomUUID(),
				group -> group.equals("vip") || group.equals("restricted")
			).orElseThrow().decision()
		);
	}

	@Test
	void nonPlayerActorsOnlyMatchEveryoneRules() {
		RegionFlagSet flags = RegionFlagSet.empty()
			.with(ProtectionAction.BLOCK_AUTOMATION, RegionSubject.NONMEMBERS, ProtectionDecision.DENY);

		assertEquals(
			java.util.Optional.empty(),
			flags.resolve(
				ProtectionAction.BLOCK_AUTOMATION,
				RegionAccessSet.empty(),
				null,
				ignored -> false
			)
		);
	}
}
