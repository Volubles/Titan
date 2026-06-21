package com.voluble.titanMC.auctions;

import com.voluble.titanMC.cells.model.CellRecoveryLot;
import com.voluble.titanMC.ranks.model.WardId;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionStorageTest {
	@TempDir
	Path directory;

	@Test
	void positionsAndQueuedBatchesSurviveRestart() throws Exception {
		Path database = directory.resolve("auctions.db");
		AuctionPosition position = new AuctionPosition("north_1", UUID.randomUUID(), 1, 64, 2, BlockFace.SOUTH);
		List<byte[]> items = new ArrayList<>();
		for (int index = 0; index < 30; index++) items.add(new byte[]{(byte) index});

		try (AuctionStorage storage = new AuctionStorage(database)) {
			storage.savePosition(position);
			assertTrue(storage.ingest(new CellRecoveryLot(42, UUID.randomUUID(), WardId.of("e"), items), () -> 1200));
			assertFalse(storage.ingest(new CellRecoveryLot(42, UUID.randomUUID(), WardId.of("e"), items), () -> 9999));
		}

		try (AuctionStorage storage = new AuctionStorage(database)) {
			assertEquals(position, storage.loadPositions().get("north_1"));
			List<AuctionLot> auctions = storage.loadAuctions();
			assertEquals(2, auctions.size());
			assertEquals(27, auctions.get(0).items().size());
			assertEquals(3, auctions.get(1).items().size());
			assertEquals(1200, auctions.get(0).price());
			assertEquals(AuctionState.QUEUED, auctions.get(0).state());
		}
	}

	@Test
	void stateAndRemainingItemsAreDurable() throws Exception {
		Path database = directory.resolve("state.db");
		AuctionPosition position = new AuctionPosition("slot", UUID.randomUUID(), 3, 70, 4, BlockFace.NORTH);
		try (AuctionStorage storage = new AuctionStorage(database)) {
			storage.savePosition(position);
			storage.ingest(new CellRecoveryLot(7, UUID.randomUUID(), WardId.of("e"), List.of(new byte[]{1}, new byte[]{2})), () -> 500);
			AuctionLot queued = storage.loadAuctions().getFirst();
			AuctionLot forSale = queued.atPosition(position.id(), 12345);
			storage.saveAuction(forSale);
			storage.replaceItems(forSale.id(), List.of(new byte[]{2}));
		}

		try (AuctionStorage storage = new AuctionStorage(database)) {
			AuctionLot loaded = storage.loadAuctions().getFirst();
			assertEquals(AuctionState.FOR_SALE, loaded.state());
			assertEquals("slot", loaded.positionId());
			assertEquals(12345, loaded.saleExpiresAt());
			assertEquals(1, loaded.items().size());
		}
	}
}
