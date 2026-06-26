package com.voluble.titanMC.cells;

import com.voluble.titanMC.cells.model.CellDefinition;
import com.voluble.titanMC.cells.model.CellLease;
import com.voluble.titanMC.display.notice.MessageDefaults;
import com.voluble.titanMC.display.notice.PluginMessageService;
import com.voluble.titanMC.ranks.service.PlayerRankService;
import com.voluble.titanMC.ranks.service.WardRankRequirements;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class CellRentalService {
	private final Plugin plugin;
	private final CellManager cells;
	private final Economy economy;
	private final CellSignRenderer signs;
	private final PlayerRankService playerRanks;
	private final WardRankRequirements eligibility;
	private final PluginMessageService messages;
	private final Set<String> reservations = new HashSet<>();
	private final Set<UUID> playerReservations = new HashSet<>();

	public CellRentalService(
		Plugin plugin,
		CellManager cells,
		Economy economy,
		CellSignRenderer signs,
		PlayerRankService playerRanks,
		WardRankRequirements eligibility,
		PluginMessageService messages
	) {
		this.plugin = plugin;
		this.cells = cells;
		this.economy = economy;
		this.signs = signs;
		this.playerRanks = playerRanks;
		this.eligibility = eligibility;
		this.messages = messages;
	}

	public void rent(Player player, String cellId) {
		CellDefinition cell = cells.get(cellId);
		if (cell == null) {
			messages.send(player, MessageDefaults.CELLS_UNKNOWN);
			return;
		}
		if (economy == null) {
			messages.send(player, MessageDefaults.CELLS_RENTING_UNAVAILABLE);
			return;
		}
		var currentRank = playerRanks.current(player.getUniqueId());
		if (currentRank.isEmpty()) {
			messages.send(player, MessageDefaults.CELLS_RANK_UNAVAILABLE);
			return;
		}
		if (!eligibility.allows(currentRank.get().rankId(), cell.wardId())) {
			messages.send(player, MessageDefaults.CELLS_RANK_REQUIRED, args -> args
				.plain("rank", eligibility.requiredRank(cell.wardId()).value().toUpperCase(java.util.Locale.ROOT))
				.plain("ward", cell.wardId().value().toUpperCase(java.util.Locale.ROOT)));
			return;
		}
		if (!playerReservations.add(player.getUniqueId())) {
			messages.send(player, MessageDefaults.CELLS_RENT_ALREADY_PROCESSING_PLAYER);
			return;
		}
		if (!reservations.add(cell.id())) {
			playerReservations.remove(player.getUniqueId());
			messages.send(player, MessageDefaults.CELLS_RENT_ALREADY_PROCESSING_CELL);
			return;
		}
		CellLease lease;
		try {
			lease = cells.planLease(cell.id(), player.getUniqueId());
		} catch (RuntimeException e) {
			reservations.remove(cell.id());
			playerReservations.remove(player.getUniqueId());
			messages.send(player, MessageDefaults.COMMAND_RUNTIME_ERROR, args -> args.plain("reason", e.getMessage()));
			return;
		}
		if (!economy.has(player, cell.rentPrice())) {
			reservations.remove(cell.id());
			playerReservations.remove(player.getUniqueId());
			messages.send(player, MessageDefaults.CELLS_NOT_ENOUGH_MONEY);
			return;
		}
		var withdrawal = economy.withdrawPlayer(player, cell.rentPrice());
		if (!withdrawal.transactionSuccess()) {
			reservations.remove(cell.id());
			playerReservations.remove(player.getUniqueId());
			messages.send(player, MessageDefaults.CELLS_PAYMENT_FAILED);
			return;
		}
		cells.persistLease(lease).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
			try {
				if (error != null) throw new IllegalStateException(error);
				cells.activateLease(lease);
				signs.refresh(cell);
				messages.send(player, MessageDefaults.CELLS_RENTED, args -> args.plain("cell", cell.displayName()));
			} catch (RuntimeException failure) {
				cells.discardLease(lease);
				economy.depositPlayer(player, cell.rentPrice());
				messages.send(player, MessageDefaults.CELLS_RENT_REFUNDED);
				plugin.getLogger().warning("Failed to activate cell lease: " + failure.getMessage());
			} finally {
				reservations.remove(cell.id());
				playerReservations.remove(player.getUniqueId());
			}
		}));
	}
}
