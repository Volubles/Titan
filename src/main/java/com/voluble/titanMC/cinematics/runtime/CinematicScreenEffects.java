package com.voluble.titanMC.cinematics.runtime;

import com.voluble.titanMC.display.screen.ScreenEffectRequest;
import org.bukkit.entity.Player;

@FunctionalInterface
interface CinematicScreenEffects {
	boolean show(Player player, ScreenEffectRequest request);
}
