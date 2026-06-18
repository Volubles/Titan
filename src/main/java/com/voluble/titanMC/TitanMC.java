package com.voluble.titanMC;

import com.voluble.titanMC.managers.ConfigManager;
import com.voluble.titanMC.donatorTools.DonatorToolsCommandModule;
import com.voluble.titanMC.donatorTools.tools.DonatorToolsConfigManager;
import com.voluble.titanMC.managers.EconomyManager;
import com.voluble.titanMC.managers.RegistrationManager;
import com.voluble.titanMC.mines.MineManager;
import com.voluble.titanMC.mines.reset.MineScheduler;
import com.voluble.titanMC.mines.listeners.MineBlockListener;
import io.voluble.michellelib.commands.CommandKit;
import io.voluble.michellelib.menu.MenuService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import com.voluble.titanMC.mines.command.MineCommandModule;

public final class TitanMC extends JavaPlugin {

	private static TitanMC instance;
	private ConfigManager configManager;
	private DonatorToolsConfigManager donatorToolsConfigManager;
	private EconomyManager economyManager;
	private RegistrationManager registrationManager;
	private MineManager mineManager;
	private MineScheduler mineScheduler;
	private MenuService menuService;

	@Override
	public void onEnable() {
		instance = this;

		// Initialize menu service
		menuService = MenuService.create(this);

		// Initialize general config
		configManager = new ConfigManager(this);
		configManager.initialize();

		// Register component configs
		donatorToolsConfigManager = new DonatorToolsConfigManager(this);
		configManager.registerComponent(donatorToolsConfigManager);

		// Events
		registrationManager = new RegistrationManager(this);
		registrationManager.registerEvents();

		// Mines
		mineManager = new MineManager(this);
		mineManager.load();
		getLogger().info("Loaded " + mineManager.getAll().size() + " mines");
		mineScheduler = new MineScheduler(this, mineManager);
		mineScheduler.start();
		getServer().getPluginManager().registerEvents(new MineBlockListener(this), this);
		getLogger().info("MineBlockListener registered");

		// MichelleLib commands (dtools, mine)
		new CommandKit(this)
			.addModule(new DonatorToolsCommandModule())
			.addModule(new MineCommandModule(this))
			.install();

		getLogger().info("TitanMC has been enabled!");
	}

	@Override
	public void onDisable() {
		if (menuService != null) menuService.shutdown();
		if (mineScheduler != null) mineScheduler.stop();
		if (mineManager != null) mineManager.saveAll();
	}

	public static TitanMC getInstance() {
		return instance;
	}

	// Donator Tools Config Delegates
	public void reloadDonatorToolsConfig() {
		donatorToolsConfigManager.reload();
	}

	public boolean isBlockAllowed(Material material) {
		return donatorToolsConfigManager.isBlockAllowed(material);
	}

	// Economy Manager Delegates
	public Economy getEconomy() {
		return economyManager.getEconomy();
	}

	public MineManager getMineManager() { return mineManager; }
	public MineScheduler getMineScheduler() { return mineScheduler; }
	public MenuService getMenuService() { return menuService; }
}
