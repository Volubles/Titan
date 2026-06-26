package com.voluble.titanMC.cells;

import com.voluble.titanMC.cells.config.CellsConfigurationManager;
import com.voluble.titanMC.cells.model.CellDefinition;
import com.voluble.titanMC.cells.model.CellLease;
import com.voluble.titanMC.display.notice.MessageDefaults;
import com.voluble.titanMC.display.notice.PluginMessageService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class CellManagementService {
	private final Plugin plugin;
	private final CellManager cells;
	private final CellResetService resets;
	private final CellSignRenderer signs;
	private final CellsConfigurationManager configuration;
	private final Economy economy;
	private final PluginMessageService messages;
	private final Set<String> busy = new HashSet<>();

	public CellManagementService(Plugin plugin, CellManager cells, CellResetService resets, CellSignRenderer signs, CellsConfigurationManager configuration, Economy economy, PluginMessageService messages) {
		this.plugin = plugin;
		this.cells = cells;
		this.resets = resets;
		this.signs = signs;
		this.configuration = configuration;
		this.economy = economy;
		this.messages = messages;
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
			messages.send(player, MessageDefaults.CELLS_ECONOMY_UNAVAILABLE);
			callback.accept(false);
			return;
		}
		if (!busy.add(cellId)) {
			messages.send(player, MessageDefaults.CELLS_OPERATION_BUSY);
			callback.accept(false);
			return;
		}
		long base = Math.max(System.currentTimeMillis(), lease.expiresAtEpochMillis());
		long maximumExpiry = System.currentTimeMillis() + cell.maxRentDurationSeconds() * 1000L;
		long requestedExpiry = base + cell.rentDurationSeconds() * 1000L;
		if (requestedExpiry > maximumExpiry) {
			busy.remove(cellId);
			messages.send(player, MessageDefaults.CELLS_MAX_DURATION);
			callback.accept(false);
			return;
		}
		if (!economy.has(player, cell.rentPrice())) {
			busy.remove(cellId);
			messages.send(player, MessageDefaults.CELLS_NOT_ENOUGH_MONEY);
			callback.accept(false);
			return;
		}
		var response = economy.withdrawPlayer(player, cell.rentPrice());
		if (!response.transactionSuccess()) {
			busy.remove(cellId);
			messages.send(player, MessageDefaults.CELLS_PAYMENT_FAILED);
			callback.accept(false);
			return;
		}
		CellLease updated = new CellLease(cellId, lease.ownerId(), lease.generation(), lease.startedAtEpochMillis(), requestedExpiry);
		cells.persistLease(updated).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
			try {
				if (error != null) throw new IllegalStateException(error);
				cells.replaceLease(updated);
				signs.refresh(cell);
				messages.send(player, MessageDefaults.CELLS_RENT_EXTENDED);
				callback.accept(true);
			} catch (RuntimeException failure) {
				economy.depositPlayer(player, cell.rentPrice());
				messages.send(player, MessageDefaults.CELLS_EXTENSION_REFUNDED);
				callback.accept(false);
			} finally {
				busy.remove(cellId);
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
			messages.send(player, MessageDefaults.CELLS_RETURN_STARTED);
		} catch (RuntimeException e) {
			messages.send(player, MessageDefaults.COMMAND_RUNTIME_ERROR, args -> args.plain("reason", e.getMessage()));
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

	private CellLease requiredOwner(Player player, String cellId) {
		CellLease lease = cells.lease(cellId);
		if (lease == null || !lease.ownerId().equals(player.getUniqueId())) {
			messages.send(player, MessageDefaults.CELLS_OWNER_ONLY);
			return null;
		}
		return lease;
	}
}
