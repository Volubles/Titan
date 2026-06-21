package com.voluble.titanMC.auctions;

import com.voluble.titanMC.ranks.model.WardId;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionWardModelTest {
	@Test
	void stateTransitionsPreserveWard() {
		AuctionLot queued = new AuctionLot(
			1, 2, 0, WardId.of("d"), null, 500, AuctionState.QUEUED,
			null, null, 0, 0, List.of(new byte[]{1})
		);

		AuctionLot forSale = queued.atPosition("d-001", 1000);
		AuctionLot claimed = forSale.claimed(UUID.randomUUID(), "Buyer", 2000);

		assertEquals(WardId.of("d"), forSale.wardId());
		assertEquals(WardId.of("d"), claimed.wardId());
		assertEquals(WardId.of("d"), claimed.publicAccess().wardId());
	}

	@Test
	void positionsRequireWard() {
		assertThrows(NullPointerException.class, () -> new AuctionPosition(
			"slot", null, UUID.randomUUID(), 0, 64, 0, BlockFace.NORTH
		));
	}
}
