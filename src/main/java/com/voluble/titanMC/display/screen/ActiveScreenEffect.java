package com.voluble.titanMC.display.screen;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

record ActiveScreenEffect(BukkitTask task, boolean hudHidden, GameMode gameMode) {
	ActiveScreenEffect {
		java.util.Objects.requireNonNull(task, "task");
		java.util.Objects.requireNonNull(gameMode, "gameMode");
	}

	void cancel(Player player, ScreenEffectHudController hud) {
		cancel(player, hud, true);
	}

	void cancel(Player player, ScreenEffectHudController hud, boolean restoreHud) {
		task.cancel();
		restore(player, hud, restoreHud);
	}

	void restore(Player player, ScreenEffectHudController hud, boolean restoreHud) {
		if (player != null && player.isOnline()) {
			player.clearTitle();
			if (restoreHud && hudHidden) hud.restore(player, gameMode);
		}
	}
}
