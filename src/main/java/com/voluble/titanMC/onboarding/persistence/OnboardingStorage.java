package com.voluble.titanMC.onboarding.persistence;

import com.voluble.titanMC.onboarding.OnboardingOutfitSelection;
import com.voluble.titanMC.outfits.model.OutfitId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class OnboardingStorage implements AutoCloseable {
	private static final int SCHEMA_VERSION = 1;

	private final Connection connection;

	public OnboardingStorage(Path databasePath) throws SQLException {
		Objects.requireNonNull(databasePath, "databasePath");
		try {
			Path parent = databasePath.toAbsolutePath().getParent();
			if (parent != null) Files.createDirectories(parent);
			Class.forName("org.sqlite.JDBC");
		} catch (Exception exception) {
			throw new SQLException("Failed to prepare Onboarding database", exception);
		}
		connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
		configure();
		initializeSchema();
	}

	private void configure() throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute("PRAGMA foreign_keys = ON");
			statement.execute("PRAGMA journal_mode = WAL");
			statement.execute("PRAGMA synchronous = FULL");
			statement.execute("PRAGMA busy_timeout = 5000");
		}
	}

	private void initializeSchema() throws SQLException {
		try (Statement statement = connection.createStatement()) {
			int version;
			try (ResultSet result = statement.executeQuery("PRAGMA user_version")) {
				version = result.next() ? result.getInt(1) : 0;
				if (version > SCHEMA_VERSION) throw new SQLException("Unsupported Onboarding database schema " + version);
			}
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS onboarding_players (
				    player_uuid TEXT PRIMARY KEY,
				    completed INTEGER NOT NULL,
				    selected_outfit TEXT,
				    completed_at INTEGER
				)
				""");
			statement.execute("PRAGMA user_version = " + SCHEMA_VERSION);
		}
	}

	public synchronized boolean completed(UUID playerId) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
			SELECT completed FROM onboarding_players WHERE player_uuid = ?
			""")) {
			statement.setString(1, playerId.toString());
			try (ResultSet result = statement.executeQuery()) {
				return result.next() && result.getInt("completed") == 1;
			}
		}
	}

	public synchronized void complete(UUID playerId, OutfitId outfit, long completedAt) throws SQLException {
		complete(playerId, outfit.value(), completedAt);
	}

	public synchronized void complete(UUID playerId, String selection, long completedAt) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
			INSERT INTO onboarding_players(player_uuid, completed, selected_outfit, completed_at)
			VALUES(?,?,?,?) ON CONFLICT(player_uuid) DO UPDATE SET
			completed = excluded.completed,
			selected_outfit = excluded.selected_outfit,
			completed_at = excluded.completed_at
			""")) {
			statement.setString(1, playerId.toString());
			statement.setInt(2, 1);
			statement.setString(3, selection);
			statement.setLong(4, completedAt);
			statement.executeUpdate();
		}
	}

	public synchronized void reset(UUID playerId) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM onboarding_players WHERE player_uuid = ?")) {
			statement.setString(1, playerId.toString());
			statement.executeUpdate();
		}
	}

	public synchronized Optional<OnboardingOutfitSelection> selectedOutfit(UUID playerId) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
			SELECT selected_outfit FROM onboarding_players WHERE player_uuid = ? AND completed = 1
			""")) {
			statement.setString(1, playerId.toString());
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) return Optional.empty();
				String outfit = result.getString("selected_outfit");
				return outfit == null || outfit.isBlank()
					? Optional.empty()
					: Optional.of(OnboardingOutfitSelection.parse(outfit));
			}
		}
	}

	@Override
	public synchronized void close() throws SQLException {
		connection.close();
	}
}
