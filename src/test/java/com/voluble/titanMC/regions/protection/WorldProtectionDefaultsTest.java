package com.voluble.titanMC.regions.protection;

import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import com.voluble.titanMC.regions.protection.policy.WorldProtectionDefaults;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorldProtectionDefaultsTest {

	@Test
	void actionOverrideWinsOverWorldDefault() {
		WorldId world = new WorldId(UUID.randomUUID());
		WorldProtectionDefaults defaults = WorldProtectionDefaults.builder()
			.fallback(ProtectionDecision.ALLOW)
			.worldDefault(world, ProtectionDecision.DENY)
			.action(world, ProtectionAction.BLOCK_INTERACT, ProtectionDecision.ALLOW)
			.build();
		ProtectionActor actor = ProtectionActor.system("test", Set.of());

		assertEquals(ProtectionDecision.DENY, defaults.decide(request(actor, world, ProtectionAction.BLOCK_BREAK)));
		assertEquals(ProtectionDecision.ALLOW, defaults.decide(request(actor, world, ProtectionAction.BLOCK_INTERACT)));
	}

	@Test
	void finalDefaultsCannotAbstain() {
		WorldId world = new WorldId(UUID.randomUUID());
		assertThrows(IllegalArgumentException.class,
			() -> WorldProtectionDefaults.builder().fallback(ProtectionDecision.ABSTAIN));
		assertThrows(IllegalArgumentException.class,
			() -> WorldProtectionDefaults.builder().worldDefault(world, ProtectionDecision.ABSTAIN));
		assertThrows(IllegalArgumentException.class,
			() -> WorldProtectionDefaults.builder().action(world, ProtectionAction.BLOCK_BREAK, ProtectionDecision.ABSTAIN));
	}

	private static ProtectionRequest request(ProtectionActor actor, WorldId world, ProtectionAction action) {
		return ProtectionRequest.at(actor, action, new BlockPosition(world, 0, 0, 0));
	}
}
