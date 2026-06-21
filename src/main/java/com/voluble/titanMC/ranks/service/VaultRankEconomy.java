package com.voluble.titanMC.ranks.service;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Server;

import java.util.Objects;
import java.util.UUID;

public final class VaultRankEconomy implements RankEconomy {
	private final Economy economy;
	private final Server server;

	public VaultRankEconomy(Economy economy, Server server) {
		this.economy = Objects.requireNonNull(economy, "economy");
		this.server = Objects.requireNonNull(server, "server");
	}

	@Override
	public boolean available() {
		return true;
	}

	@Override
	public boolean has(UUID playerId, long amount) {
		return economy.has(server.getOfflinePlayer(playerId), amount);
	}

	@Override
	public double balance(UUID playerId) {
		return economy.getBalance(server.getOfflinePlayer(playerId));
	}

	@Override
	public boolean withdraw(UUID playerId, long amount) {
		EconomyResponse response = economy.withdrawPlayer(server.getOfflinePlayer(playerId), amount);
		return response.transactionSuccess();
	}

	@Override
	public boolean deposit(UUID playerId, long amount) {
		EconomyResponse response = economy.depositPlayer(server.getOfflinePlayer(playerId), amount);
		return response.transactionSuccess();
	}
}
