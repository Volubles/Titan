package com.voluble.titanMC.donatortools;

import com.voluble.titanMC.donatortools.config.DonatorToolsConfigurationManager;
import com.voluble.titanMC.donatortools.drop.BlockDropService;
import com.voluble.titanMC.donatortools.drop.VanillaLootService;
import com.voluble.titanMC.donatortools.item.DonatorToolRegistry;
import com.voluble.titanMC.donatortools.protection.DonatorToolProtection;
import com.voluble.titanMC.donatortools.tool.ExplosiveToolListener;
import com.voluble.titanMC.donatortools.tool.SingleBlockToolListener;
import com.voluble.titanMC.donatortools.tracking.MineBreakTracker;
import com.voluble.titanMC.mines.MineManager;
import com.voluble.titanMC.mines.reset.MineScheduler;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class DonatorToolsService {

	private final DonatorToolsConfigurationManager configuration;
	private final DonatorToolRegistry registry;

	public DonatorToolsService(
		Plugin plugin,
		DonatorToolsConfigurationManager configuration,
		MineManager mines,
		MineScheduler mineScheduler,
		ProtectionService protection
	) {
		Objects.requireNonNull(plugin, "plugin");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.registry = new DonatorToolRegistry(plugin);
		plugin.getServer().getPluginManager().registerEvents(
			new SingleBlockToolListener(
				registry,
				configuration,
				new BlockDropService(),
				new VanillaLootService()
			),
			plugin
		);
		plugin.getServer().getPluginManager().registerEvents(
			new ExplosiveToolListener(
				registry,
				configuration,
				new DonatorToolProtection(protection),
				new MineBreakTracker(mines, mineScheduler)
			),
			plugin
		);
	}

	public DonatorToolRegistry registry() {
		return registry;
	}

	public void reload() {
		configuration.reload();
	}
}
