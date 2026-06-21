package com.voluble.titanMC.cells;

import com.voluble.titanMC.cells.model.CellDefinition;
import com.voluble.titanMC.cells.model.CellLease;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public final class CellRentalService {
	private final Plugin plugin;private final CellManager cells;private final Economy economy;private final Set<String> reservations=new HashSet<>();
	public CellRentalService(Plugin plugin,CellManager cells,Economy economy){this.plugin=plugin;this.cells=cells;this.economy=economy;}
	public void rent(Player player,String cellId){CellDefinition cell=cells.get(cellId);if(cell==null){player.sendMessage("Unknown cell.");return;}if(economy==null){player.sendMessage("Renting is unavailable because no economy provider is active.");return;}if(!reservations.add(cell.id())){player.sendMessage("This cell is currently being processed.");return;}CellLease lease;try{lease=cells.planLease(cell.id(),player.getUniqueId(),false);}catch(RuntimeException e){reservations.remove(cell.id());player.sendMessage(e.getMessage());return;}if(!economy.has(player,cell.rentPrice())){reservations.remove(cell.id());player.sendMessage("You do not have enough money.");return;}var withdrawal=economy.withdrawPlayer(player,cell.rentPrice());if(!withdrawal.transactionSuccess()){reservations.remove(cell.id());player.sendMessage("The payment failed.");return;}cells.persistLease(lease).whenComplete((ignored,error)->Bukkit.getScheduler().runTask(plugin,()->{try{if(error!=null)throw new IllegalStateException(error);cells.activateLease(lease);player.sendMessage("You rented cell '"+cell.id()+"'.");}catch(RuntimeException failure){cells.discardLease(lease);economy.depositPlayer(player,cell.rentPrice());player.sendMessage("The rental could not be completed; your payment was refunded.");plugin.getLogger().warning("Failed to activate cell lease: "+failure.getMessage());}finally{reservations.remove(cell.id());}}));}
}
