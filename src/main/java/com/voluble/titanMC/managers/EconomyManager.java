package com.voluble.titanMC.managers;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyManager {

	private final JavaPlugin plugin;
	private Economy economy;

	public EconomyManager(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	public boolean initialize() {
		RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		economy = rsp.getProvider();
		return economy != null;
	}

	public Economy getEconomy() {
		return economy;
	}

	public boolean isEnabled() {
		return economy != null;
	}
}

