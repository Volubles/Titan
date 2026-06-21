package com.voluble.titanMC.ranks.service;

import com.voluble.titanMC.ranks.model.PlayerRank;
import com.voluble.titanMC.ranks.model.PrisonRank;
import com.voluble.titanMC.ranks.model.RankId;
import com.voluble.titanMC.ranks.model.RankupRequirement;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RankupService {
	private final RankCatalog catalog;
	private final PlayerRankService players;
	private final RankEconomy economy;
	private final LongSupplier clock;
	private final Logger logger;

	public RankupService(RankCatalog catalog, PlayerRankService players, RankEconomy economy, Logger logger) {
		this(catalog, players, economy, System::currentTimeMillis, logger);
	}

	RankupService(
		RankCatalog catalog,
		PlayerRankService players,
		RankEconomy economy,
		LongSupplier clock,
		Logger logger
	) {
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.players = Objects.requireNonNull(players, "players");
		this.economy = Objects.requireNonNull(economy, "economy");
		this.clock = Objects.requireNonNull(clock, "clock");
		this.logger = Objects.requireNonNull(logger, "logger");
	}

	public RankupResult rankup(UUID playerId) {
		Objects.requireNonNull(playerId, "playerId");
		Optional<PlayerRank> currentRank = players.current(playerId);
		if (currentRank.isEmpty()) return new RankupResult.NoCurrentRank(playerId);

		PlayerRank current = currentRank.get();
		Optional<PrisonRank> nextRank = catalog.nextRank(current.rankId());
		if (nextRank.isEmpty()) return new RankupResult.AtMaxRank(current);

		PrisonRank next = nextRank.get();
		RankupRequirement requirement = next.rankup().orElseThrow(
			() -> new IllegalStateException("non-starter rank " + next.id().value() + " is missing a rankup requirement")
		);

		Optional<RankId> required = requirement.requires();
		if (required.isPresent() && !catalog.meetsRequirement(current.rankId(), required.get())) {
			return new RankupResult.MissingRequirement(next, required.get());
		}

		if (!economy.available()) return new RankupResult.EconomyUnavailable();
		if (!economy.has(playerId, requirement.cost())) {
			return new RankupResult.InsufficientFunds(next, requirement.cost(), economy.balance(playerId));
		}
		if (!economy.withdraw(playerId, requirement.cost())) {
			return new RankupResult.InsufficientFunds(next, requirement.cost(), economy.balance(playerId));
		}

		try {
			PlayerRank applied = players.apply(current.withRank(next.id(), clock.getAsLong()));
			return new RankupResult.Success(current, applied, requirement.cost());
		} catch (RuntimeException failure) {
			boolean refunded = refund(playerId, requirement.cost());
			logger.log(
				Level.SEVERE,
				"Failed to persist rankup for " + playerId + " to " + next.id().value()
					+ (refunded ? "; payment refunded" : "; payment refund failed"),
				failure
			);
			return new RankupResult.PersistenceFailure(next, requirement.cost(), refunded);
		}
	}

	private boolean refund(UUID playerId, long amount) {
		try {
			return economy.deposit(playerId, amount);
		} catch (RuntimeException failure) {
			logger.log(Level.SEVERE, "Rankup refund threw an exception for " + playerId, failure);
			return false;
		}
	}
}
