package com.voluble.titanMC.cells;

import com.voluble.titanMC.cells.model.CellDefinition;
import com.voluble.titanMC.cells.baseline.CellBaseline;
import com.voluble.titanMC.cells.model.CellLease;
import com.voluble.titanMC.cells.model.TrackedCellBlock;
import com.voluble.titanMC.cells.model.CellSign;
import com.voluble.titanMC.cells.model.CellRecoveryLot;
import com.voluble.titanMC.cells.persistence.CellStorage;
import com.voluble.titanMC.util.RegionUtils;
import com.voluble.titanMC.ranks.model.WardId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CellStorageTest {
	private static CellBaseline baseline(CellDefinition cell) {
		int sizeX = cell.cuboid().maxX - cell.cuboid().minX + 1;
		int sizeY = cell.cuboid().maxY - cell.cuboid().minY + 1;
		int sizeZ = cell.cuboid().maxZ - cell.cuboid().minZ + 1;
		return new CellBaseline(sizeX, sizeY, sizeZ, List.of("minecraft:air"), new int[sizeX * sizeY * sizeZ]);
	}

	@Test
	void createsCellAndBaselineAtomically() throws Exception {
		Path database = directory.resolve("baseline.db");
		UUID world = UUID.randomUUID();
		CellDefinition cell = new CellDefinition(
			"a1",
			WardId.of("e"),
			new RegionUtils.Cuboid(world, 0, 0, 0, 2, 1, 1),
			500,
			86400,
			604800,
			true
		);

		try (CellStorage storage = new CellStorage(database)) {
			storage.createCell(cell, baseline(cell)).join();

			CellBaseline loaded = storage.loadBaseline(cell.id()).join();
			assertEquals(3, loaded.sizeX());
			assertEquals(2, loaded.sizeY());
			assertEquals(2, loaded.sizeZ());
			assertEquals(List.of(), storage.cellsWithoutBaselines());
		}
	}

	@Test
	void replacesAnExistingBaseline() throws Exception {
		Path database = directory.resolve("replace-baseline.db");
		CellDefinition cell = new CellDefinition(
			"a1",
			WardId.of("e"),
			new RegionUtils.Cuboid(UUID.randomUUID(), 0, 0, 0, 1, 0, 0),
			500,
			86400,
			604800,
			true
		);
		CellBaseline replacement = new CellBaseline(
			2,
			1,
			1,
			List.of("minecraft:stone"),
			new int[]{0, 0}
		);

		try (CellStorage storage = new CellStorage(database)) {
			storage.createCell(cell, baseline(cell)).join();
			storage.replaceBaseline(cell.id(), replacement).join();

			assertEquals(List.of("minecraft:stone"), storage.loadBaseline(cell.id()).join().blockPalette());
		}
	}

	@Test
	void reportsCellsCreatedWithoutBaselines() throws Exception {
		Path database = directory.resolve("missing-baseline.db");
		CellDefinition cell = new CellDefinition(
			"legacy",
			WardId.of("e"),
			new RegionUtils.Cuboid(UUID.randomUUID(), 0, 0, 0, 1, 1, 1),
			500,
			86400,
			604800,
			true
		);

		try (CellStorage storage = new CellStorage(database)) {
			storage.saveCell(cell).join();

			assertEquals(List.of("legacy"), storage.cellsWithoutBaselines());
			assertThrows(java.util.concurrent.CompletionException.class, () -> storage.loadBaseline("legacy").join());
		}
	}
	@TempDir Path directory;
	@Test void cellsLeasesAndTrackedBlocksSurviveRestart() throws Exception {
		Path database=directory.resolve("cells.db"); UUID world=UUID.randomUUID(); UUID owner=UUID.randomUUID();
		CellDefinition cell=new CellDefinition("a1",WardId.of("d"),new RegionUtils.Cuboid(world,0,0,0,5,5,5),500,86400,604800,true);
		CellLease lease=new CellLease("a1",owner,1,1000,2000);
		TrackedCellBlock block=new TrackedCellBlock("a1",1,world,2,2,2);
		UUID member=UUID.randomUUID();
		CellSign sign=new CellSign("a1",world,10,64,10);
		try(CellStorage storage=new CellStorage(database)){storage.saveCell(cell).join();storage.saveLease(lease).join();storage.addMember("a1",1,member).join();storage.addBlocks(List.of(block)).join();storage.saveSign(sign).join();}
		try(CellStorage storage=new CellStorage(database)){assertEquals(cell,storage.loadCells().get("a1"));var leases=storage.loadLeases();assertEquals(lease,leases.get("a1"));assertEquals(java.util.Set.of(member),storage.loadMembers(leases).get("a1"));assertEquals(List.of(block),storage.loadBlocks());assertEquals(List.of(sign),storage.loadSigns());}
	}
	@Test void resetPreparationIsDurableAndCompletionIsAtomic() throws Exception {
		Path database=directory.resolve("reset.db");UUID world=UUID.randomUUID();UUID owner=UUID.randomUUID();CellDefinition cell=new CellDefinition("a1",WardId.of("e"),new RegionUtils.Cuboid(world,0,0,0,5,5,5),500,86400,604800,true);CellLease lease=new CellLease("a1",owner,3,1000,2000);TrackedCellBlock block=new TrackedCellBlock("a1",3,world,2,2,2);long lot;
		try(CellStorage storage=new CellStorage(database)){storage.saveCell(cell).join();storage.saveLease(lease).join();storage.addBlocks(List.of(block)).join();storage.beginReset(lease).join();lot=storage.createRecoveryLot(lease,cell.wardId(),List.of(new byte[]{1,2,3})).join();assertEquals(com.voluble.titanMC.cells.model.CellResetJob.Phase.PREPARED,storage.loadResetJobs().get("a1").phase());}
		try(CellStorage storage=new CellStorage(database)){storage.completeReset("a1",3,lot).join();assertEquals(0,storage.loadLeases().size());assertEquals(0,storage.loadBlocks().size());assertEquals(0,storage.loadResetJobs().size());}
	}

	@Test
	void resetFailureMetadataSurvivesReopening() throws Exception {
		Path database = directory.resolve("reset-retry.db");
		UUID owner = UUID.randomUUID();
		CellDefinition cell = new CellDefinition(
			"a1",
			WardId.of("e"),
			new RegionUtils.Cuboid(UUID.randomUUID(), 0, 0, 0, 1, 1, 1),
			500,
			86400,
			604800,
			true
		);
		CellLease lease = new CellLease(cell.id(), owner, 1, 1000, 2000);

		try (CellStorage storage = new CellStorage(database)) {
			storage.saveCell(cell).join();
			storage.saveLease(lease).join();
			storage.beginReset(lease).join();
			var failed = storage.loadResetJobs().get(cell.id()).failed(5000, "world is unavailable");
			storage.recordResetFailure(failed).join();
		}

		try (CellStorage storage = new CellStorage(database)) {
			var recovered = storage.loadResetJobs().get(cell.id());
			assertEquals(1, recovered.attempts());
			assertEquals(5000, recovered.nextAttemptAt());
			assertEquals("world is unavailable", recovered.lastError());
		}
	}
	@Test void readyRecoveryLotsCanBeClaimedByAuctions() throws Exception {
		Path database = directory.resolve("recovery.db");
		UUID world = UUID.randomUUID();
		UUID owner = UUID.randomUUID();
		CellDefinition cell = new CellDefinition("a1", WardId.of("e"), new RegionUtils.Cuboid(world, 0, 0, 0, 5, 5, 5), 500, 86400, 604800, true);
		CellLease lease = new CellLease("a1", owner, 1, 1000, 2000);
		try (CellStorage storage = new CellStorage(database)) {
			storage.saveCell(cell).join();
			storage.saveLease(lease).join();
			storage.beginReset(lease).join();
			long lotId = storage.createRecoveryLot(lease, cell.wardId(), List.of(new byte[]{1})).join();
			storage.completeReset("a1", 1, lotId).join();
			var lots = storage.loadReadyRecoveryLots().join();
			assertEquals(1, lots.size());
			assertEquals(owner, lots.getFirst().ownerId());
			assertEquals(WardId.of("e"), lots.getFirst().wardId());
			storage.markRecoveryLotAuctioned(lotId).join();
			assertEquals(0, storage.loadReadyRecoveryLots().join().size());
		}
	}
	@Test void schemaOneCellsReceiveDisplayNames() throws Exception {
		Path database=directory.resolve("v1.db");UUID world=UUID.randomUUID();Class.forName("org.sqlite.JDBC");
		try(var connection=DriverManager.getConnection("jdbc:sqlite:"+database);var statement=connection.createStatement()){statement.executeUpdate("CREATE TABLE cells(id TEXT PRIMARY KEY,world_id TEXT NOT NULL,min_x INTEGER NOT NULL,min_y INTEGER NOT NULL,min_z INTEGER NOT NULL,max_x INTEGER NOT NULL,max_y INTEGER NOT NULL,max_z INTEGER NOT NULL,rent_price INTEGER NOT NULL,rent_duration_seconds INTEGER NOT NULL,enabled INTEGER NOT NULL)");statement.executeUpdate("INSERT INTO cells VALUES('cell_01','"+world+"',0,0,0,5,5,5,500,86400,1)");statement.execute("PRAGMA user_version=1");}
		try(CellStorage storage=new CellStorage(database)){
			CellDefinition cell = storage.loadCells().get("cell_01");
			assertEquals("cell_01", cell.displayName());
			assertEquals(86400L * 30L, cell.maxRentDurationSeconds());
			assertEquals(WardId.of("e"), cell.wardId());
		}
	}

	@Test void schemaFourRecoveryLotsReceiveCellWard() throws Exception {
		Path database = directory.resolve("v4.db");
		UUID world = UUID.randomUUID();
		UUID owner = UUID.randomUUID();
		Class.forName("org.sqlite.JDBC");
		try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
			 var statement = connection.createStatement()) {
			statement.executeUpdate("""
				CREATE TABLE cells (
				 id TEXT PRIMARY KEY, display_name TEXT NOT NULL, ward_id TEXT NOT NULL, world_id TEXT NOT NULL,
				 min_x INTEGER NOT NULL, min_y INTEGER NOT NULL, min_z INTEGER NOT NULL,
				 max_x INTEGER NOT NULL, max_y INTEGER NOT NULL, max_z INTEGER NOT NULL,
				 rent_price INTEGER NOT NULL, rent_duration_seconds INTEGER NOT NULL,
				 max_rent_duration_seconds INTEGER NOT NULL, enabled INTEGER NOT NULL
				)
				""");
			statement.executeUpdate("INSERT INTO cells VALUES('d_cell','D Cell','d','" + world + "',0,0,0,5,5,5,500,86400,604800,1)");
			statement.executeUpdate("""
				CREATE TABLE cell_recovery_lots (
				 id INTEGER PRIMARY KEY AUTOINCREMENT, cell_id TEXT NOT NULL, owner_uuid TEXT NOT NULL,
				 lease_generation INTEGER NOT NULL, created_at INTEGER NOT NULL, status TEXT NOT NULL
				)
				""");
			statement.executeUpdate("INSERT INTO cell_recovery_lots VALUES(1,'d_cell','" + owner + "',1,1000,'READY')");
			statement.execute("PRAGMA user_version=4");
		}

		try (CellStorage storage = new CellStorage(database)) {
			CellRecoveryLot lot = storage.loadReadyRecoveryLots().join().getFirst();
			assertEquals(WardId.of("d"), lot.wardId());
		}
	}

	@Test
	void playerCannotOwnTwoCellLeases() throws Exception {
		Path database = directory.resolve("single-owner.db");
		UUID world = UUID.randomUUID();
		UUID owner = UUID.randomUUID();
		CellDefinition first = new CellDefinition(
			"first", WardId.of("e"), new RegionUtils.Cuboid(world, 0, 0, 0, 4, 4, 4),
			500, 86400, 604800, true
		);
		CellDefinition second = new CellDefinition(
			"second", WardId.of("e"), new RegionUtils.Cuboid(world, 10, 0, 0, 14, 4, 4),
			500, 86400, 604800, true
		);

		try (CellStorage storage = new CellStorage(database)) {
			storage.saveCell(first).join();
			storage.saveCell(second).join();
			storage.saveLease(new CellLease(first.id(), owner, 1, 1000, 2000)).join();

			assertThrows(
				java.util.concurrent.CompletionException.class,
				() -> storage.saveLease(new CellLease(second.id(), owner, 1, 1000, 2000)).join()
			);
			assertEquals(first.id(), storage.loadLeases().get(first.id()).cellId());
		}
	}

	@Test
	void duplicateLeasePreflightIdentifiesOwnerAndCells() throws Exception {
		Path database = directory.resolve("duplicate-owner.db");
		UUID owner = UUID.randomUUID();
		Class.forName("org.sqlite.JDBC");
		try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
			 var statement = connection.createStatement()) {
			statement.executeUpdate("""
				CREATE TABLE cell_leases (
				 cell_id TEXT PRIMARY KEY NOT NULL,
				 owner_uuid TEXT NOT NULL,
				 generation INTEGER NOT NULL,
				 started_at INTEGER NOT NULL,
				 expires_at INTEGER NOT NULL
				)
				""");
			statement.executeUpdate(
				"INSERT INTO cell_leases VALUES('e-01','" + owner + "',1,1000,2000),"
					+ "('d-04','" + owner + "',1,1000,2000)"
			);
		}

		SQLException failure = assertThrows(SQLException.class, () -> new CellStorage(database));

		assertTrue(failure.getMessage().contains(owner.toString()));
		assertTrue(failure.getMessage().contains("e-01"));
		assertTrue(failure.getMessage().contains("d-04"));
	}
}
