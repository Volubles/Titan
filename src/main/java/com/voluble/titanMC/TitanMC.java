package com.voluble.titanMC;

import com.voluble.titanMC.managers.ConfigManager;
import com.voluble.titanMC.donatorTools.DonatorToolsCommandModule;
import com.voluble.titanMC.donatorTools.tools.DonatorToolsConfigManager;
import com.voluble.titanMC.managers.EconomyManager;
import com.voluble.titanMC.managers.RegistrationManager;
import com.voluble.titanMC.mines.MineManager;
import com.voluble.titanMC.mines.reset.MineScheduler;
import com.voluble.titanMC.mines.listeners.MineBlockListener;
import com.voluble.titanMC.regions.persistence.RegionStorageException;
import com.voluble.titanMC.regions.service.RegionEngine;
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
	private RegionEngine regionEngine;

	@Override
	public void onEnable() {
		instance = this;
		try {
			regionEngine = RegionEngine.open(getDataFolder().toPath().resolve("regions.db"));
			getLogger().info("Titan Region Engine loaded " + regionEngine.snapshot().definitions().size() + " regions");
		} catch (RegionStorageException exception) {
			getLogger().severe("Titan Region Engine failed to initialize: " + exception.getMessage());
			exception.printStackTrace();
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

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
		if (regionEngine != null) {
			try {
				regionEngine.close();
			} catch (RegionStorageException exception) {
				getLogger().severe("Failed to close Titan Region Engine cleanly: " + exception.getMessage());
			}
		}
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
	public RegionEngine getRegionEngine() { return regionEngine; }
}
