package com.voluble.titanMC.cells;

import com.voluble.titanMC.cells.config.CellsConfigurationManager;
import com.voluble.titanMC.cells.model.CellDefinition;
import com.voluble.titanMC.cells.model.CellLease;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class CellManagementService {
	private final Plugin plugin;
	private final CellManager cells;
	private final CellResetService resets;
	private final CellSignRenderer signs;
	private final CellsConfigurationManager configuration;
	private final Economy economy;
	private final Set<String> busy = new HashSet<>();

	public CellManagementService(Plugin plugin, CellManager cells, CellResetService resets, CellSignRenderer signs, CellsConfigurationManager configuration, Economy economy) {
		this.plugin = plugin;
		this.cells = cells;
		this.resets = resets;
		this.signs = signs;
		this.configuration = configuration;
		this.economy = economy;
	}

	public boolean isOwner(Player player, String cellId) {
		CellLease lease = cells.lease(cellId);
		return lease != null && lease.ownerId().equals(player.getUniqueId());
	}

	public void extend(Player player, String cellId, Consumer<Boolean> callback) {
		CellDefinition cell = cells.get(cellId);
		CellLease lease = requiredOwner(player, cellId);
		if (cell == null || lease == null) {
			callback.accept(false);
			return;
		}
		if (economy == null) {
			player.sendMessage("Economy is unavailable.");
			callback.accept(false);
			return;
		}
		if (!busy.add(cellId)) {
			player.sendMessage("A cell operation is already running.");
			callback.accept(false);
			return;
		}
		if (!economy.has(player, cell.rentPrice())) {
			busy.remove(cellId);
			player.sendMessage("You do not have enough money.");
			callback.accept(false);
			return;
		}
		var response = economy.withdrawPlayer(player, cell.rentPrice());
		if (!response.transactionSuccess()) {
			busy.remove(cellId);
			player.sendMessage("The payment failed.");
			callback.accept(false);
			return;
		}
		long base = Math.max(System.currentTimeMillis(), lease.expiresAtEpochMillis());
		CellLease updated = new CellLease(cellId, lease.ownerId(), lease.generation(), lease.startedAtEpochMillis(), base + cell.rentDurationSeconds() * 1000L, lease.autoRenew());
		cells.persistLease(updated).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
			try {
				if (error != null) throw new IllegalStateException(error);
				cells.replaceLease(updated);
				signs.refresh(cell);
				player.sendMessage("Rent extended.");
				callback.accept(true);
			} catch (RuntimeException failure) {
				economy.depositPlayer(player, cell.rentPrice());
				player.sendMessage("Extension failed; your payment was refunded.");
				callback.accept(false);
			} finally {
				busy.remove(cellId);
			}
		}));
	}

	public void toggleAutoRenew(Player player, String cellId, Consumer<Boolean> callback) {
		CellLease lease = requiredOwner(player, cellId);
		if (lease == null) {
			callback.accept(false);
			return;
		}
		CellLease updated = new CellLease(cellId, lease.ownerId(), lease.generation(), lease.startedAtEpochMillis(), lease.expiresAtEpochMillis(), !lease.autoRenew());
		cells.persistLease(updated).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
			if (error == null) {
				cells.replaceLease(updated);
				player.sendMessage("Auto-renew " + (updated.autoRenew() ? "enabled." : "disabled."));
				callback.accept(true);
			} else {
				player.sendMessage("Could not update auto-renew.");
				callback.accept(false);
			}
		}));
	}

	public void returnCell(Player player, String cellId) {
		CellDefinition cell = cells.get(cellId);
		CellLease lease = requiredOwner(player, cellId);
		if (cell == null || lease == null) return;
		try {
			resets.reset(cellId);
			int percent = configuration.current().sellbackRefundPercent();
			if (economy != null && percent > 0) economy.depositPlayer(player, cell.rentPrice() * percent / 100.0);
			signs.refresh(cell);
			player.sendMessage("Cell return started.");
		} catch (RuntimeException e) {
			player.sendMessage(e.getMessage());
		}
	}

	public boolean addMember(Player owner, String cellId, UUID member) {
		if (requiredOwner(owner, cellId) == null) return false;
		cells.addMember(cellId, member);
		signs.refresh(cells.get(cellId));
		return true;
	}

	public boolean removeMember(Player owner, String cellId, UUID member) {
		if (requiredOwner(owner, cellId) == null) return false;
		cells.removeMember(cellId, member);
		signs.refresh(cells.get(cellId));
		return true;
	}

	public CompletableFuture<Boolean> renewAutomatically(CellLease lease) {
		CellDefinition cell = cells.get(lease.cellId());
		if (cell == null || economy == null || !busy.add(cell.id())) return CompletableFuture.completedFuture(false);
		OfflinePlayer owner = Bukkit.getOfflinePlayer(lease.ownerId());
		if (!economy.has(owner, cell.rentPrice())) {
			busy.remove(cell.id());
			return CompletableFuture.completedFuture(false);
		}
		var response = economy.withdrawPlayer(owner, cell.rentPrice());
		if (!response.transactionSuccess()) {
			busy.remove(cell.id());
			return CompletableFuture.completedFuture(false);
		}
		CellLease updated = new CellLease(cell.id(), lease.ownerId(), lease.generation(), lease.startedAtEpochMillis(), Math.max(System.currentTimeMillis(), lease.expiresAtEpochMillis()) + cell.rentDurationSeconds() * 1000L, true);
		CompletableFuture<Boolean> result = new CompletableFuture<>();
		cells.persistLease(updated).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
			try {
				if (error != null) throw new IllegalStateException(error);
				cells.replaceLease(updated);
				signs.refresh(cell);
				result.complete(true);
			} catch (RuntimeException failure) {
				economy.depositPlayer(owner, cell.rentPrice());
				result.complete(false);
			} finally {
				busy.remove(cell.id());
			}
		}));
		return result;
	}

	private CellLease requiredOwner(Player player, String cellId) {
		CellLease lease = cells.lease(cellId);
		if (lease == null || !lease.ownerId().equals(player.getUniqueId())) {
			player.sendMessage("Only the cell owner can do that.");
			return null;
		}
		return lease;
	}
}
