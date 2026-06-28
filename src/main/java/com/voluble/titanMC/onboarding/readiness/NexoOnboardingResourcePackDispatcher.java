package com.voluble.titanMC.onboarding.readiness;

import com.nexomc.nexo.NexoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class NexoOnboardingResourcePackDispatcher implements OnboardingResourcePackDispatcher {
	@Override
	public boolean available() {
		return Bukkit.getPluginManager().isPluginEnabled("Nexo");
	}

	@Override
	public void dispatch(Player player) {
		NexoPlugin.instance().packServer().sendPack(player);
	}
}
