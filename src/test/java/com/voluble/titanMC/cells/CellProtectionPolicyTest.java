package com.voluble.titanMC.cells;

import com.voluble.titanMC.cells.region.CellProtectionPolicy;
import com.voluble.titanMC.regions.model.BlockBox;
import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.model.CuboidGeometry;
import com.voluble.titanMC.regions.model.RegionAccessSet;
import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.RegionId;
import com.voluble.titanMC.regions.model.RegionKey;
import com.voluble.titanMC.regions.model.RegionTextSet;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import com.voluble.titanMC.regions.protection.model.RegionFlagSet;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CellProtectionPolicyTest {
	private final UUID memberId = UUID.randomUUID();
	private final WorldId world = new WorldId(UUID.randomUUID());
	private final RegionDefinition region = new RegionDefinition(
		RegionId.random(),
		RegionKey.of("cell", "a1"),
		world,
		200,
		new CuboidGeometry(BlockBox.inclusive(0, 0, 0, 5, 5, 5)),
		RegionAccessSet.of(Set.of(memberId), Set.of()),
		RegionFlagSet.empty(),
		RegionTextSet.empty(),
		Instant.EPOCH,
		Instant.EPOCH,
		1
	);
	private final CellProtectionPolicy policy = new CellProtectionPolicy();
	private final BlockPosition position = new BlockPosition(world, 1, 1, 1);

	@Test
	void membersCanMutateContentRestoredByTheBaseline() {
		ProtectionActor member = ProtectionActor.player(memberId, Set.of());

		assertDecision(member, ProtectionAction.BLOCK_PLACE, ProtectionDecision.ALLOW);
		assertDecision(member, ProtectionAction.BLOCK_BREAK, ProtectionDecision.ALLOW);
		assertDecision(member, ProtectionAction.CONTAINER_OPEN, ProtectionDecision.ALLOW);
		assertDecision(member, ProtectionAction.BUCKET_EMPTY, ProtectionDecision.ALLOW);
		assertDecision(member, ProtectionAction.BUCKET_FILL, ProtectionDecision.ALLOW);
	}

	@Test
	void membersCannotCreateContentWithoutAResetLifecycle() {
		ProtectionActor member = ProtectionActor.player(memberId, Set.of());

		assertDecision(member, ProtectionAction.ENTITY_PLACE, ProtectionDecision.DENY);
		assertDecision(member, ProtectionAction.ENTITY_INTERACT, ProtectionDecision.DENY);
		assertDecision(member, ProtectionAction.ENTITY_DAMAGE, ProtectionDecision.DENY);
		assertDecision(member, ProtectionAction.HANGING_MODIFY, ProtectionDecision.DENY);
		assertDecision(member, ProtectionAction.VEHICLE_PLACE, ProtectionDecision.DENY);
		assertDecision(member, ProtectionAction.VEHICLE_ENTER, ProtectionDecision.DENY);
		assertDecision(member, ProtectionAction.VEHICLE_MODIFY, ProtectionDecision.DENY);
	}

	@Test
	void visitorsCanEnterButCannotBuild() {
		ProtectionActor visitor = ProtectionActor.player(UUID.randomUUID(), Set.of());

		assertDecision(visitor, ProtectionAction.BLOCK_PLACE, ProtectionDecision.DENY);
		assertDecision(visitor, ProtectionAction.ENTRY, ProtectionDecision.ALLOW);
	}

	private void assertDecision(
		ProtectionActor actor,
		ProtectionAction action,
		ProtectionDecision expected
	) {
		assertEquals(expected, policy.decide(ProtectionRequest.at(actor, action, position), region));
	}
}
