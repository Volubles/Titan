package com.voluble.titanMC.mines.protection;

import com.voluble.titanMC.regions.model.BlockBox;
import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.RegionId;
import com.voluble.titanMC.regions.model.RegionKey;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MineProtectionPolicyTest {

	private static final WorldId WORLD = new WorldId(new UUID(10L, 1L));
	private static final ProtectionActor PLAYER = ProtectionActor.player(new UUID(10L, 2L), Set.of());
	private static final BlockPosition POSITION = new BlockPosition(WORLD, 1, 2, 3);
	private static final RegionDefinition REGION = new RegionDefinition(
		new RegionId(new UUID(10L, 3L)),
		RegionKey.of(MineProtectionPolicy.NAMESPACE, "alpha"),
		WORLD,
		100,
		List.of(new BlockBox(0, 0, 0, 16, 16, 16)),
		Instant.EPOCH,
		Instant.EPOCH
	);

	private final MineProtectionPolicy policy = new MineProtectionPolicy();

	@Test
	void identifiesMineRegions() {
		assertEquals("mine-default", policy.id());
		assertEquals("mine", policy.namespace());
	}

	@Test
	void allowsMiningAndDeniesPlacement() {
		assertEquals(ProtectionDecision.ALLOW, decide(ProtectionAction.BLOCK_BREAK));
		assertEquals(ProtectionDecision.DENY, decide(ProtectionAction.BLOCK_PLACE));
		assertEquals(ProtectionDecision.DENY, decide(ProtectionAction.CONTAINER_OPEN));
		assertEquals(ProtectionDecision.DENY, decide(ProtectionAction.BUCKET_FILL));
		assertEquals(ProtectionDecision.DENY, decide(ProtectionAction.BUCKET_EMPTY));
	}

	@Test
	void abstainsFromActionsWithoutMineSemanticsYet() {
		assertEquals(ProtectionDecision.ABSTAIN, decide(ProtectionAction.BLOCK_INTERACT));
		assertEquals(ProtectionDecision.ABSTAIN, decide(ProtectionAction.EXPLOSION_BLOCK_DAMAGE));
	}

	private ProtectionDecision decide(ProtectionAction action) {
		return policy.decide(ProtectionRequest.at(PLAYER, action, POSITION), REGION);
	}
}
