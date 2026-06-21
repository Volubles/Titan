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
import java.util.function.LongSupplier;

public final class AuctionStorage implements AutoCloseable {
	private static final int SCHEMA_VERSION = 1;
	private static final int ITEMS_PER_CHEST = 27;
	private final Connection connection;

	public AuctionStorage(Path path) throws SQLException {
		try {
			Files.createDirectories(path.toAbsolutePath().getParent());
			Class.forName("org.sqlite.JDBC");
		} catch (Exception exception) {
			throw new SQLException("Could not prepare auction database", exception);
		}
		connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
		try (Statement statement = connection.createStatement()) {
			statement.execute("PRAGMA foreign_keys=ON");
			statement.execute("PRAGMA journal_mode=WAL");
			statement.execute("PRAGMA synchronous=FULL");
			try (ResultSet result = statement.executeQuery("PRAGMA user_version")) {
				int version = result.next() ? result.getInt(1) : 0;
				if (version > SCHEMA_VERSION) throw new SQLException("Unsupported Auctions database schema " + version);
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
				 auction_id INTEGER NOT NULL, slot INTEGER NOT NULL, item_data BLOB NOT NULL,
				 PRIMARY KEY(auction_id,slot),
				 FOREIGN KEY(auction_id) REFERENCES auctions(id) ON DELETE CASCADE
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
			SELECT auction.*, item.slot, item.item_data
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
				if (item != null) builder.items.add(item);
			}
		}
		return builders.values().stream().map(AuctionBuilder::build).toList();
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
				replaceItems(auctionId, items.subList(from, to));
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

	public synchronized void replaceItems(long auctionId, List<byte[]> items) throws SQLException {
		boolean manageTransaction = connection.getAutoCommit();
		if (manageTransaction) connection.setAutoCommit(false);
		try {
			replaceItemRows(auctionId, items);
			if (manageTransaction) connection.commit();
		} catch (SQLException exception) {
			if (manageTransaction) connection.rollback();
			throw exception;
		} finally {
			if (manageTransaction) connection.setAutoCommit(true);
		}
	}

	private void replaceItemRows(long auctionId, List<byte[]> items) throws SQLException {
		try (PreparedStatement delete = connection.prepareStatement("DELETE FROM auction_items WHERE auction_id=?")) {
			delete.setLong(1, auctionId);
			delete.executeUpdate();
		}
		try (PreparedStatement insert = connection.prepareStatement("INSERT INTO auction_items VALUES(?,?,?)")) {
			for (int slot = 0; slot < items.size(); slot++) {
				insert.setLong(1, auctionId);
				insert.setInt(2, slot);
				insert.setBytes(3, items.get(slot));
				insert.addBatch();
			}
			insert.executeBatch();
		}
	}

	public synchronized void deleteAuction(long id) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM auctions WHERE id=?")) {
			statement.setLong(1, id);
			statement.executeUpdate();
		}
	}

	@Override
	public synchronized void close() throws SQLException {
		connection.close();
	}

	private static UUID uuid(String value) {
		return value == null ? null : UUID.fromString(value);
	}

	private static final class AuctionBuilder {
		private final AuctionLot lot;
		private final List<byte[]> items = new ArrayList<>();

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
