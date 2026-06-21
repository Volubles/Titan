package com.voluble.titanMC.ranks.service;

import java.util.UUID;

public interface RankEconomy {
	boolean available();

	boolean has(UUID playerId, long amount);

	double balance(UUID playerId);

	boolean withdraw(UUID playerId, long amount);

	static RankEconomy unavailable() {
		return new RankEconomy() {
			@Override public boolean available() { return false; }
			@Override public boolean has(UUID playerId, long amount) { return false; }
			@Override public double balance(UUID playerId) { return 0.0; }
			@Override public boolean withdraw(UUID playerId, long amount) { return false; }
		};
	}
}
