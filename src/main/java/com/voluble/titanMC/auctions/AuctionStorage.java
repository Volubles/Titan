package com.voluble.titanMC.auctions;

import com.voluble.titanMC.cells.model.CellRecoveryLot;
import com.voluble.titanMC.ranks.model.WardId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.LongSupplier;

public final class AuctionStorage implements AutoCloseable {
	private static final int SCHEMA_VERSION = 2;
	private static final int ITEMS_PER_CHEST = 27;
	private final Connection connection;
	private final ExecutorService writer;

	public AuctionStorage(Path path) throws SQLException {
		try {
			Files.createDirectories(path.toAbsolutePath().getParent());
			Class.forName("org.sqlite.JDBC");
		} catch (Exception exception) {
			throw new SQLException("Could not prepare auction database", exception);
		}
		connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
		try {
			initializeSchema();
		} catch (SQLException failure) {
			try {
				connection.close();
			} catch (SQLException closeFailure) {
				failure.addSuppressed(closeFailure);
			}
			throw failure;
		}
		writer = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("titan-auction-writer").factory());
	}

	private void initializeSchema() throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute("PRAGMA foreign_keys=ON");
			statement.execute("PRAGMA journal_mode=WAL");
			statement.execute("PRAGMA synchronous=FULL");
			try (ResultSet result = statement.executeQuery("PRAGMA user_version")) {
				int version = result.next() ? result.getInt(1) : 0;
				if (version != 0 && version != SCHEMA_VERSION) {
					throw new SQLException(
						"Unsupported Auctions database schema " + version + "; recreate the development database"
					);
				}
			}
			statement.execute("""
				CREATE TABLE IF NOT EXISTS auction_positions (
				 id TEXT PRIMARY KEY, ward_id TEXT NOT NULL, world_id TEXT NOT NULL,
				 x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL,
				 facing TEXT NOT NULL,
				 UNIQUE(world_id,x,y,z)
				)
				""");
			statement.execute("""
				CREATE TABLE IF NOT EXISTS auctions (
				 id INTEGER PRIMARY KEY AUTOINCREMENT,
				 source_lot_id INTEGER NOT NULL, batch_index INTEGER NOT NULL,
				 ward_id TEXT NOT NULL,
				 position_id TEXT, price INTEGER NOT NULL, state TEXT NOT NULL,
				 buyer_id TEXT, buyer_name TEXT,
				 sale_expires_at INTEGER NOT NULL DEFAULT 0,
				 claim_expires_at INTEGER NOT NULL DEFAULT 0,
				 UNIQUE(source_lot_id,batch_index),
				 FOREIGN KEY(position_id) REFERENCES auction_positions(id)
				)
				""");
			if (!hasColumn("auction_positions", "ward_id")) {
				statement.executeUpdate("ALTER TABLE auction_positions ADD COLUMN ward_id TEXT NOT NULL DEFAULT 'e'");
			}
			if (!hasColumn("auctions", "ward_id")) {
				statement.executeUpdate("ALTER TABLE auctions ADD COLUMN ward_id TEXT NOT NULL DEFAULT 'e'");
			}
			statement.execute("""
				CREATE TABLE IF NOT EXISTS auction_items (
				 id INTEGER PRIMARY KEY AUTOINCREMENT,
				 auction_id INTEGER NOT NULL, slot INTEGER NOT NULL, item_data BLOB NOT NULL,
				 UNIQUE(auction_id,slot),
				 FOREIGN KEY(auction_id) REFERENCES auctions(id) ON DELETE CASCADE
				)
				""");
			statement.execute("""
				CREATE TABLE IF NOT EXISTS auction_deliveries (
				 id INTEGER PRIMARY KEY AUTOINCREMENT,
				 auction_id INTEGER NOT NULL,
				 item_id INTEGER NOT NULL UNIQUE,
				 player_id TEXT NOT NULL,
				 item_data BLOB NOT NULL,
				 state TEXT NOT NULL,
				 created_at INTEGER NOT NULL,
				 delivered_at INTEGER
				)
				""");
			statement.execute("PRAGMA user_version=" + SCHEMA_VERSION);
		}
	}

	private boolean hasColumn(String table, String column) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
			"SELECT COUNT(*) FROM pragma_table_info(?) WHERE name=?"
		)) {
			statement.setString(1, table);
			statement.setString(2, column);
			try (ResultSet result = statement.executeQuery()) {
				return result.next() && result.getInt(1) > 0;
			}
		}
	}

	public synchronized Map<String, AuctionPosition> loadPositions() throws SQLException {
		Map<String, AuctionPosition> positions = new LinkedHashMap<>();
		try (Statement statement = connection.createStatement();
			 ResultSet result = statement.executeQuery("SELECT * FROM auction_positions ORDER BY id")) {
			while (result.next()) {
				AuctionPosition position = new AuctionPosition(
					result.getString("id"), WardId.of(result.getString("ward_id")), UUID.fromString(result.getString("world_id")),
					result.getInt("x"), result.getInt("y"), result.getInt("z"),
					org.bukkit.block.BlockFace.valueOf(result.getString("facing"))
				);
				positions.put(position.id(), position);
			}
		}
		return positions;
	}

	public synchronized List<AuctionLot> loadAuctions() throws SQLException {
		Map<Long, AuctionBuilder> builders = new LinkedHashMap<>();
		try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
			SELECT auction.*, item.id AS item_id, item.slot, item.item_data
			FROM auctions auction
			LEFT JOIN auction_items item ON item.auction_id=auction.id
			ORDER BY auction.id,item.slot
			""")) {
			while (result.next()) {
				long id = result.getLong("id");
				AuctionBuilder builder = builders.get(id);
				if (builder == null) {
					builder = new AuctionBuilder(new AuctionLot(
						id, result.getLong("source_lot_id"), result.getInt("batch_index"), WardId.of(result.getString("ward_id")),
						result.getString("position_id"), result.getLong("price"),
						AuctionState.valueOf(result.getString("state")),
						uuid(result.getString("buyer_id")), result.getString("buyer_name"),
						result.getLong("sale_expires_at"), result.getLong("claim_expires_at"), List.of()
					));
					builders.put(id, builder);
				}
				byte[] item = result.getBytes("item_data");
				if (item != null) {
					builder.items.add(new AuctionItem(result.getLong("item_id"), result.getInt("slot"), item));
				}
			}
		}
		return builders.values().stream().map(AuctionBuilder::build).toList();
	}

	public CompletableFuture<List<AuctionLot>> loadAuctionsAsync() {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return loadAuctions();
			} catch (SQLException exception) {
				throw new IllegalStateException("Could not load auctions", exception);
			}
		}, writer);
	}

	public synchronized void savePosition(AuctionPosition position) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
			"INSERT INTO auction_positions(id,ward_id,world_id,x,y,z,facing) VALUES(?,?,?,?,?,?,?)"
		)) {
			statement.setString(1, position.id());
			statement.setString(2, position.wardId().value());
			statement.setString(3, position.worldId().toString());
			statement.setInt(4, position.x());
			statement.setInt(5, position.y());
			statement.setInt(6, position.z());
			statement.setString(7, position.facing().name());
			statement.executeUpdate();
		}
	}

	public synchronized void deletePosition(String id) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM auction_positions WHERE id=?")) {
			statement.setString(1, id);
			statement.executeUpdate();
		}
	}

	public synchronized boolean ingest(CellRecoveryLot source, LongSupplier prices) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM auctions WHERE source_lot_id=?")) {
			statement.setLong(1, source.id());
			try (ResultSet result = statement.executeQuery()) {
				if (result.next()) return false;
			}
		}
		boolean oldAutoCommit = connection.getAutoCommit();
		connection.setAutoCommit(false);
		try {
			List<byte[]> items = source.items();
			int batches = (items.size() + ITEMS_PER_CHEST - 1) / ITEMS_PER_CHEST;
			for (int batch = 0; batch < batches; batch++) {
				long auctionId;
				try (PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO auctions(source_lot_id,batch_index,ward_id,price,state) VALUES(?,?,?,?,'QUEUED')",
					Statement.RETURN_GENERATED_KEYS
				)) {
					statement.setLong(1, source.id());
					statement.setInt(2, batch);
					statement.setString(3, source.wardId().value());
					statement.setLong(4, prices.getAsLong());
					statement.executeUpdate();
					try (ResultSet keys = statement.getGeneratedKeys()) {
						if (!keys.next()) throw new SQLException("No auction id returned");
						auctionId = keys.getLong(1);
					}
				}
				int from = batch * ITEMS_PER_CHEST;
				int to = Math.min(items.size(), from + ITEMS_PER_CHEST);
				insertItems(auctionId, items.subList(from, to));
			}
			connection.commit();
			return true;
		} catch (SQLException exception) {
			connection.rollback();
			throw exception;
		} finally {
			connection.setAutoCommit(oldAutoCommit);
		}
	}

	public CompletableFuture<Boolean> ingestAsync(CellRecoveryLot source, LongSupplier prices) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return ingest(source, prices);
			} catch (SQLException exception) {
				throw new IllegalStateException("Could not ingest cell recovery lot", exception);
			}
		}, writer);
	}

	public synchronized void saveAuction(AuctionLot lot) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
			UPDATE auctions SET position_id=?,price=?,state=?,buyer_id=?,buyer_name=?,sale_expires_at=?,claim_expires_at=?
			WHERE id=?
			""")) {
			statement.setString(1, lot.positionId());
			statement.setLong(2, lot.price());
			statement.setString(3, lot.state().name());
			statement.setString(4, lot.buyerId() == null ? null : lot.buyerId().toString());
			statement.setString(5, lot.buyerName());
			statement.setLong(6, lot.saleExpiresAt());
			statement.setLong(7, lot.claimExpiresAt());
			statement.setLong(8, lot.id());
			statement.executeUpdate();
		}
	}

	public CompletableFuture<Void> saveAuctionAsync(AuctionLot lot) {
		return CompletableFuture.runAsync(() -> {
			try {
				saveAuction(lot);
			} catch (SQLException exception) {
				throw new IllegalStateException("Could not save auction", exception);
			}
		}, writer);
	}

	private void insertItems(long auctionId, List<byte[]> items) throws SQLException {
		try (PreparedStatement insert = connection.prepareStatement(
			"INSERT INTO auction_items(auction_id,slot,item_data) VALUES(?,?,?)"
		)) {
			for (int slot = 0; slot < items.size(); slot++) {
				insert.setLong(1, auctionId);
				insert.setInt(2, slot);
				insert.setBytes(3, items.get(slot));
				insert.addBatch();
			}
			insert.executeBatch();
		}
	}

	public CompletableFuture<AuctionDelivery> reserveItem(long auctionId, long itemId, UUID playerId) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return reserveItemTransaction(auctionId, itemId, playerId);
			} catch (SQLException exception) {
				throw new IllegalStateException("Could not reserve auction item", exception);
			}
		}, writer);
	}

	private synchronized AuctionDelivery reserveItemTransaction(long auctionId, long itemId, UUID playerId) throws SQLException {
		boolean oldAutoCommit = connection.getAutoCommit();
		connection.setAutoCommit(false);
		try {
			byte[] itemData;
			try (PreparedStatement statement = connection.prepareStatement("""
				SELECT item.item_data
				FROM auction_items item
				JOIN auctions auction ON auction.id=item.auction_id
				WHERE item.id=? AND item.auction_id=?
				  AND (auction.state='PUBLIC' OR (auction.state='CLAIMED' AND auction.buyer_id=?))
				""")) {
				statement.setLong(1, itemId);
				statement.setLong(2, auctionId);
				statement.setString(3, playerId.toString());
				try (ResultSet result = statement.executeQuery()) {
					if (!result.next()) throw new SQLException("Auction item is unavailable");
					itemData = result.getBytes("item_data");
				}
			}
			try (PreparedStatement statement = connection.prepareStatement(
				"DELETE FROM auction_items WHERE id=? AND auction_id=?"
			)) {
				statement.setLong(1, itemId);
				statement.setLong(2, auctionId);
				if (statement.executeUpdate() != 1) throw new SQLException("Auction item was already reserved");
			}
			long deliveryId;
			try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO auction_deliveries(auction_id,item_id,player_id,item_data,state,created_at)
				VALUES(?,?,?,?,'PENDING',?)
				""", Statement.RETURN_GENERATED_KEYS)) {
				statement.setLong(1, auctionId);
				statement.setLong(2, itemId);
				statement.setString(3, playerId.toString());
				statement.setBytes(4, itemData);
				statement.setLong(5, System.currentTimeMillis());
				statement.executeUpdate();
				try (ResultSet keys = statement.getGeneratedKeys()) {
					if (!keys.next()) throw new SQLException("No auction delivery id returned");
					deliveryId = keys.getLong(1);
				}
			}
			connection.commit();
			return new AuctionDelivery(
				deliveryId, auctionId, itemId, playerId, itemData, AuctionDelivery.State.PENDING
			);
		} catch (SQLException exception) {
			connection.rollback();
			throw exception;
		} finally {
			connection.setAutoCommit(oldAutoCommit);
		}
	}

	public CompletableFuture<Void> completeDelivery(long deliveryId, UUID playerId) {
		return CompletableFuture.runAsync(() -> {
			try {
				completeDeliveryWrite(deliveryId, playerId);
			} catch (SQLException exception) {
				throw new IllegalStateException("Could not complete auction delivery", exception);
			}
		}, writer);
	}

	private synchronized void completeDeliveryWrite(long deliveryId, UUID playerId) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
			UPDATE auction_deliveries SET state='DELIVERED',delivered_at=?
			WHERE id=? AND player_id=? AND state='PENDING'
			""")) {
			statement.setLong(1, System.currentTimeMillis());
			statement.setLong(2, deliveryId);
			statement.setString(3, playerId.toString());
			statement.executeUpdate();
		}
	}

	public CompletableFuture<List<AuctionDelivery>> loadDeliveries(UUID playerId) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return readDeliveries(playerId);
			} catch (SQLException exception) {
				throw new IllegalStateException("Could not load auction deliveries", exception);
			}
		}, writer);
	}

	private synchronized List<AuctionDelivery> readDeliveries(UUID playerId) throws SQLException {
		List<AuctionDelivery> deliveries = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement("""
			SELECT id,auction_id,item_id,item_data,state
			FROM auction_deliveries WHERE player_id=? ORDER BY id
			""")) {
			statement.setString(1, playerId.toString());
			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					deliveries.add(new AuctionDelivery(
						result.getLong("id"), result.getLong("auction_id"), result.getLong("item_id"),
						playerId, result.getBytes("item_data"),
						AuctionDelivery.State.valueOf(result.getString("state"))
					));
				}
			}
		}
		return deliveries;
	}

	public synchronized void deleteAuction(long id) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM auctions WHERE id=?")) {
			statement.setLong(1, id);
			statement.executeUpdate();
		}
	}

	public CompletableFuture<Void> deleteAuctionAsync(long id) {
		return CompletableFuture.runAsync(() -> {
			try {
				deleteAuction(id);
			} catch (SQLException exception) {
				throw new IllegalStateException("Could not delete auction", exception);
			}
		}, writer);
	}

	@Override
	public void close() throws SQLException {
		writer.shutdown();
		try {
			if (!writer.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) writer.shutdownNow();
		} catch (InterruptedException exception) {
			writer.shutdownNow();
			Thread.currentThread().interrupt();
		}
		connection.close();
	}

	private static UUID uuid(String value) {
		return value == null ? null : UUID.fromString(value);
	}

	private static final class AuctionBuilder {
		private final AuctionLot lot;
		private final List<AuctionItem> items = new ArrayList<>();

		private AuctionBuilder(AuctionLot lot) {
			this.lot = lot;
		}

		private AuctionLot build() {
			return new AuctionLot(
				lot.id(), lot.sourceLotId(), lot.batchIndex(), lot.wardId(), lot.positionId(), lot.price(), lot.state(),
				lot.buyerId(), lot.buyerName(), lot.saleExpiresAt(), lot.claimExpiresAt(), items
			);
		}
	}
}
