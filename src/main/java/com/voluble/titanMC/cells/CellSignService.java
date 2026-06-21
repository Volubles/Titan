package com.voluble.titanMC.cells;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class CellSignService implements Listener {
	private final CellManager cells;private final CellRentalService rentals;private final NamespacedKey cellKey;
	public CellSignService(Plugin plugin,CellManager cells,CellRentalService rentals){this.cells=cells;this.rentals=rentals;cellKey=new NamespacedKey(plugin,"cell_rental");}
	public void bind(Sign sign,String cellId){var cell=java.util.Objects.requireNonNull(cells.get(cellId),"Unknown cell: "+cellId);sign.getPersistentDataContainer().set(cellKey,PersistentDataType.STRING,cell.id());var side=sign.getSide(Side.FRONT);side.line(0,Component.text("[Cell]"));side.line(1,Component.text(cell.id()));side.line(2,Component.text("$"+cell.rentPrice()));side.line(3,Component.text(format(cell.rentDurationSeconds())));sign.update(true,false);}
	@EventHandler(ignoreCancelled=true)public void onInteract(PlayerInteractEvent event){if(event.getAction()!=Action.RIGHT_CLICK_BLOCK||event.getClickedBlock()==null||!(event.getClickedBlock().getState() instanceof Sign sign))return;String cellId=sign.getPersistentDataContainer().get(cellKey,PersistentDataType.STRING);if(cellId==null)return;event.setCancelled(true);rentals.rent(event.getPlayer(),cellId);}
	private static String format(long seconds){if(seconds%86400==0)return seconds/86400+" days";if(seconds%3600==0)return seconds/3600+" hours";return seconds+" sec";}
}
