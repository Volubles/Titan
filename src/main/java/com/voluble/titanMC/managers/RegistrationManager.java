package com.voluble.titanMC.managers;

import com.voluble.titanMC.donatorTools.tools.blockPickaxe.BlockPickaxeListener;
import com.voluble.titanMC.donatorTools.tools.bountifulPickaxe.BountifulPickaxeListener;
import com.voluble.titanMC.donatorTools.tools.explosivePickaxe.ExplosivePickaxeListener;
import com.voluble.titanMC.donatorTools.tools.smeltingPickaxe.SmeltingPickaxeListener;
import org.bukkit.plugin.java.JavaPlugin;

public class RegistrationManager {

	private final JavaPlugin plugin;

	public RegistrationManager(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	public void registerEvents() {
		plugin.getServer().getPluginManager().registerEvents(new SmeltingPickaxeListener(), plugin);
		plugin.getServer().getPluginManager().registerEvents(new ExplosivePickaxeListener(), plugin);
		plugin.getServer().getPluginManager().registerEvents(new BountifulPickaxeListener(), plugin);
		plugin.getServer().getPluginManager().registerEvents(new BlockPickaxeListener(), plugin);
	}
}

