package com.voluble.titanMC.regions.persistence;

import com.voluble.titanMC.regions.model.BlockBox;
import com.voluble.titanMC.regions.model.BlockPoint2;
import com.voluble.titanMC.regions.model.ConvexPolyhedronGeometry;
import com.voluble.titanMC.regions.model.CuboidGeometry;
import com.voluble.titanMC.regions.model.PolygonPrismGeometry;
import com.voluble.titanMC.regions.model.PolyhedronPlane;
import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.RegionGeometry;
import com.voluble.titanMC.regions.model.RegionId;
import com.voluble.titanMC.regions.model.RegionKey;
import com.voluble.titanMC.regions.model.WorldId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SqliteRegionRepository implements RegionRepository {

	private static final int SCHEMA_VERSION = 4;
	private final Path databasePath;
	private Connection connection;

	public SqliteRegionRepository(Path databasePath) {
		this.databasePath = Objects.requireNonNull(databasePath, "databasePath").toAbsolutePath();
	}

	@Override
	public synchronized void initialize() throws SQLException {
		if (connection != null) return;
		try {
			Path parent = databasePath.getParent();
			if (parent != null) Files.createDirectories(parent);
			Class.forName("org.sqlite.JDBC");
		} catch (Exception exception) {
			throw new SQLException("Failed to prepare SQLite driver or database directory", exception);
		}

		connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
		try {
			configureConnection();
			verifyIntegrity();
			initializeSchema();
		} catch (SQLException exception) {
			try {
				connection.close();
			} catch (SQLException suppressed) {
				exception.addSuppressed(suppressed);
			}
			connection = null;
			throw exception;
		}
	}

	private void configureConnection() throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute("PRAGMA foreign_keys = ON");
			statement.execute("PRAGMA journal_mode = WAL");
			statement.execute("PRAGMA synchronous = FULL");
			statement.execute("PRAGMA busy_timeout = 5000");
		}
	}

	private void verifyIntegrity() throws SQLException {
		try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("PRAGMA integrity_check")) {
			if (!result.next() || !"ok".equalsIgnoreCase(result.getString(1))) {
				throw new SQLException("Region database integrity check failed; refusing to modify it");
			}
	}
	}

	private void initializeSchema() throws SQLException {
		int version;
		try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("PRAGMA user_version")) {
			version = result.next() ? result.getInt(1) : 0;
		}
		if (version != 0 && version != SCHEMA_VERSION) {
			throw new SQLException(
				"Unsupported region database schema " + version + "; expected an empty database or schema " + SCHEMA_VERSION
			);
		}
		if (version == SCHEMA_VERSION) return;

		inTransaction(() -> {
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate("""
					CREATE TABLE IF NOT EXISTS regions (
					    id TEXT PRIMARY KEY NOT NULL,
					    world_id TEXT NOT NULL,
					    namespace TEXT NOT NULL,
					    name TEXT NOT NULL,
					    priority INTEGER NOT NULL,
					    revision INTEGER NOT NULL,
					    created_at INTEGER NOT NULL,
					    updated_at INTEGER NOT NULL,
					    UNIQUE(world_id, namespace, name)
					)
					""");
				statement.executeUpdate("""
					CREATE TABLE IF NOT EXISTS region_geometries (
					    region_id TEXT PRIMARY KEY NOT NULL,
					    geometry_type TEXT NOT NULL,
					    min_x INTEGER NOT NULL,
					    min_y INTEGER NOT NULL,
					    min_z INTEGER NOT NULL,
					    max_x_exclusive INTEGER NOT NULL,
					    max_y_exclusive INTEGER NOT NULL,
					    max_z_exclusive INTEGER NOT NULL,
					    FOREIGN KEY(region_id) REFERENCES regions(id) ON DELETE CASCADE
					)
					""");
				statement.executeUpdate("""
					CREATE TABLE IF NOT EXISTS region_polygon_points (
					    region_id TEXT NOT NULL,
					    point_order INTEGER NOT NULL,
					    x INTEGER NOT NULL,
					    z INTEGER NOT NULL,
					    PRIMARY KEY(region_id, point_order),
					    FOREIGN KEY(region_id) REFERENCES region_geometries(region_id) ON DELETE CASCADE
					)
					""");
				statement.executeUpdate("""
					CREATE TABLE IF NOT EXISTS region_polyhedron_planes (
					    region_id TEXT NOT NULL,
					    plane_order INTEGER NOT NULL,
					    normal_x REAL NOT NULL,
					    normal_y REAL NOT NULL,
					    normal_z REAL NOT NULL,
					    maximum_dot_product REAL NOT NULL,
					    PRIMARY KEY(region_id, plane_order),
					    FOREIGN KEY(region_id) REFERENCES region_geometries(region_id) ON DELETE CASCADE
					)
					""");
				statement.execute("PRAGMA user_version = " + SCHEMA_VERSION);
			}
		});
	}

	@Override
	public synchronized List<RegionDefinition> loadAll() throws SQLException {
		requireInitialized();
		Map<RegionId, List<BlockPoint2>> polygonPoints = loadPolygonPoints();
		Map<RegionId, List<PolyhedronPlane>> polyhedronPlanes = loadPolyhedronPlanes();
		List<RegionDefinition> definitions = new ArrayList<>();
		try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
			SELECT r.id, r.world_id, r.namespace, r.name, r.priority, r.revision, r.created_at, r.updated_at,
			       g.geometry_type, g.min_x, g.min_y, g.min_z,
			       g.max_x_exclusive, g.max_y_exclusive, g.max_z_exclusive
			FROM regions r
			LEFT JOIN region_geometries g ON g.region_id = r.id
			ORDER BY r.world_id, r.namespace, r.name
			""")) {
			while (result.next()) {
				RegionId id = RegionId.parse(result.getString("id"));
				if (result.getObject("min_x") == null) throw new SQLException("Region has no geometry: " + id);
				BlockBox bounds = new BlockBox(
					result.getInt("min_x"), result.getInt("min_y"), result.getInt("min_z"),
					result.getInt("max_x_exclusive"), result.getInt("max_y_exclusive"),
					result.getInt("max_z_exclusive")
				);
				definitions.add(new RegionDefinition(
					id,
					RegionKey.of(result.getString("namespace"), result.getString("name")),
					WorldId.parse(result.getString("world_id")),
					result.getInt("priority"),
					readGeometry(id, result.getString("geometry_type"), bounds, polygonPoints, polyhedronPlanes),
					Instant.ofEpochMilli(result.getLong("created_at")),
					Instant.ofEpochMilli(result.getLong("updated_at")),
					result.getLong("revision")
				));
			}
		}
		return List.copyOf(definitions);
	}

	@Override
	public synchronized void save(RegionDefinition definition) throws SQLException {
		requireInitialized();
		inTransaction(() -> {
			try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO regions(id, world_id, namespace, name, priority, revision, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				ON CONFLICT(id) DO UPDATE SET
				    world_id = excluded.world_id,
				    namespace = excluded.namespace,
				    name = excluded.name,
				    priority = excluded.priority,
				    revision = excluded.revision,
				    updated_at = excluded.updated_at
				""")) {
				statement.setString(1, definition.id().toString());
				statement.setString(2, definition.worldId().toString());
				statement.setString(3, definition.key().namespace());
				statement.setString(4, definition.key().name());
				statement.setInt(5, definition.priority());
				statement.setLong(6, definition.revision());
				statement.setLong(7, definition.createdAt().toEpochMilli());
				statement.setLong(8, definition.updatedAt().toEpochMilli());
				statement.executeUpdate();
			}
			try (PreparedStatement statement = connection.prepareStatement("DELETE FROM region_geometries WHERE region_id = ?")) {
				statement.setString(1, definition.id().toString());
				statement.executeUpdate();
			}
			insertGeometry(definition);
		});
	}

	@Override
	public synchronized void delete(RegionId id) throws SQLException {
		requireInitialized();
		inTransaction(() -> {
			try (PreparedStatement statement = connection.prepareStatement("DELETE FROM regions WHERE id = ?")) {
				statement.setString(1, id.toString());
				statement.executeUpdate();
			}
		});
	}

	@Override
	public synchronized void applyBatch(List<RegionDefinition> saves, List<RegionId> deletes) throws SQLException {
		requireInitialized();
		Objects.requireNonNull(saves, "saves");
		Objects.requireNonNull(deletes, "deletes");
		inTransaction(() -> {
			try (PreparedStatement statement = connection.prepareStatement("DELETE FROM regions WHERE id = ?")) {
				for (RegionId id : deletes) {
					statement.setString(1, Objects.requireNonNull(id, "deletes must not contain null").toString());
					statement.addBatch();
				}
				for (RegionDefinition definition : saves) {
					statement.setString(1, Objects.requireNonNull(definition, "saves must not contain null").id().toString());
					statement.addBatch();
				}
				statement.executeBatch();
			}

			try (PreparedStatement regionStatement = connection.prepareStatement("""
				INSERT INTO regions(id, world_id, namespace, name, priority, revision, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""")) {
				for (RegionDefinition definition : saves) {
					regionStatement.setString(1, definition.id().toString());
					regionStatement.setString(2, definition.worldId().toString());
					regionStatement.setString(3, definition.key().namespace());
					regionStatement.setString(4, definition.key().name());
					regionStatement.setInt(5, definition.priority());
					regionStatement.setLong(6, definition.revision());
					regionStatement.setLong(7, definition.createdAt().toEpochMilli());
					regionStatement.setLong(8, definition.updatedAt().toEpochMilli());
					regionStatement.addBatch();
				}
				regionStatement.executeBatch();
				for (RegionDefinition definition : saves) insertGeometry(definition);
			}
		});
	}

	private Map<RegionId, List<BlockPoint2>> loadPolygonPoints() throws SQLException {
		Map<RegionId, List<BlockPoint2>> points = new LinkedHashMap<>();
		try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
			SELECT region_id, x, z FROM region_polygon_points ORDER BY region_id, point_order
			""")) {
			while (result.next()) {
				RegionId id = RegionId.parse(result.getString("region_id"));
				points.computeIfAbsent(id, ignored -> new ArrayList<>()).add(
					new BlockPoint2(result.getInt("x"), result.getInt("z"))
				);
			}
		}
		return points;
	}

	private Map<RegionId, List<PolyhedronPlane>> loadPolyhedronPlanes() throws SQLException {
		Map<RegionId, List<PolyhedronPlane>> planes = new LinkedHashMap<>();
		try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
			SELECT region_id, normal_x, normal_y, normal_z, maximum_dot_product
			FROM region_polyhedron_planes ORDER BY region_id, plane_order
			""")) {
			while (result.next()) {
				RegionId id = RegionId.parse(result.getString("region_id"));
				planes.computeIfAbsent(id, ignored -> new ArrayList<>()).add(new PolyhedronPlane(
					result.getDouble("normal_x"), result.getDouble("normal_y"),
					result.getDouble("normal_z"), result.getDouble("maximum_dot_product")
				));
			}
		}
		return planes;
	}

	private static RegionGeometry readGeometry(
		RegionId id,
		String typeName,
		BlockBox bounds,
		Map<RegionId, List<BlockPoint2>> polygonPoints,
		Map<RegionId, List<PolyhedronPlane>> polyhedronPlanes
	) throws SQLException {
		GeometryType type;
		try {
			type = GeometryType.valueOf(typeName);
		} catch (IllegalArgumentException exception) {
			throw new SQLException("Unknown region geometry type for " + id + ": " + typeName, exception);
		}
		return switch (type) {
			case CUBOID -> new CuboidGeometry(bounds);
			case POLYGON_PRISM -> new PolygonPrismGeometry(
				requiredParts(id, "polygon points", polygonPoints.get(id)),
				bounds.minY(), bounds.maxYExclusive() - 1
			);
			case CONVEX_POLYHEDRON -> new ConvexPolyhedronGeometry(
				bounds, requiredParts(id, "polyhedron planes", polyhedronPlanes.get(id))
			);
		};
	}

	private static <T> List<T> requiredParts(RegionId id, String description, List<T> parts) throws SQLException {
		if (parts == null || parts.isEmpty()) throw new SQLException("Region has no " + description + ": " + id);
		return parts;
	}

	private void insertGeometry(RegionDefinition definition) throws SQLException {
		RegionGeometry geometry = definition.geometry();
		GeometryType type = geometryType(geometry);
		try (PreparedStatement statement = connection.prepareStatement("""
			INSERT INTO region_geometries(
			    region_id, geometry_type, min_x, min_y, min_z,
			    max_x_exclusive, max_y_exclusive, max_z_exclusive
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
			""")) {
			bindGeometry(statement, definition.id(), type, geometry.bounds());
			statement.executeUpdate();
		}
		if (geometry instanceof PolygonPrismGeometry polygon) insertPolygonPoints(definition.id(), polygon);
		else if (geometry instanceof ConvexPolyhedronGeometry polyhedron) {
			insertPolyhedronPlanes(definition.id(), polyhedron);
		}
	}

	private static GeometryType geometryType(RegionGeometry geometry) throws SQLException {
		if (geometry instanceof CuboidGeometry) return GeometryType.CUBOID;
		if (geometry instanceof PolygonPrismGeometry) return GeometryType.POLYGON_PRISM;
		if (geometry instanceof ConvexPolyhedronGeometry) return GeometryType.CONVEX_POLYHEDRON;
		throw new SQLException("Unsupported region geometry: " + geometry.getClass().getSimpleName());
	}

	private static void bindGeometry(
		PreparedStatement statement,
		RegionId id,
		GeometryType type,
		BlockBox box
	) throws SQLException {
		statement.setString(1, id.toString());
		statement.setString(2, type.name());
		statement.setInt(3, box.minX());
		statement.setInt(4, box.minY());
		statement.setInt(5, box.minZ());
		statement.setInt(6, box.maxXExclusive());
		statement.setInt(7, box.maxYExclusive());
		statement.setInt(8, box.maxZExclusive());
	}

	private void insertPolygonPoints(RegionId id, PolygonPrismGeometry polygon) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
			INSERT INTO region_polygon_points(region_id, point_order, x, z) VALUES (?, ?, ?, ?)
			""")) {
			for (int index = 0; index < polygon.points().size(); index++) {
				BlockPoint2 point = polygon.points().get(index);
				statement.setString(1, id.toString());
				statement.setInt(2, index);
				statement.setInt(3, point.x());
				statement.setInt(4, point.z());
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	private void insertPolyhedronPlanes(RegionId id, ConvexPolyhedronGeometry polyhedron) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
			INSERT INTO region_polyhedron_planes(
			    region_id, plane_order, normal_x, normal_y, normal_z, maximum_dot_product
			) VALUES (?, ?, ?, ?, ?, ?)
			""")) {
			for (int index = 0; index < polyhedron.planes().size(); index++) {
				PolyhedronPlane plane = polyhedron.planes().get(index);
				statement.setString(1, id.toString());
				statement.setInt(2, index);
				statement.setDouble(3, plane.normalX());
				statement.setDouble(4, plane.normalY());
				statement.setDouble(5, plane.normalZ());
				statement.setDouble(6, plane.maximumDotProduct());
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	@Override
	public synchronized void close() throws SQLException {
		if (connection == null) return;
		connection.close();
		connection = null;
	}

	private void requireInitialized() throws SQLException {
		if (connection == null) throw new SQLException("Region repository is not initialized");
	}

	private void inTransaction(SqlAction action) throws SQLException {
		boolean previousAutoCommit = connection.getAutoCommit();
		connection.setAutoCommit(false);
		try {
			action.run();
			connection.commit();
		} catch (SQLException | RuntimeException exception) {
			try {
				connection.rollback();
			} catch (SQLException rollbackFailure) {
				exception.addSuppressed(rollbackFailure);
			}
			throw exception;
		} finally {
			connection.setAutoCommit(previousAutoCommit);
		}
	}

	@FunctionalInterface
	private interface SqlAction {
		void run() throws SQLException;
	}

	private enum GeometryType {
		CUBOID,
		POLYGON_PRISM,
		CONVEX_POLYHEDRON
	}
}
