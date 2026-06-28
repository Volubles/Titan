package com.voluble.titanMC.display.screen;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

final class ScreenEffectHudController {
	private final Plugin plugin;

	ScreenEffectHudController(Plugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	void hide(Player player) {
		if (!available()) return;
		send(player, com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR);
	}

	void restore(Player player, GameMode gameMode) {
		if (!available()) return;
		send(player, switch (gameMode) {
			case CREATIVE -> com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE;
			case ADVENTURE -> com.github.retrooper.packetevents.protocol.player.GameMode.ADVENTURE;
			case SPECTATOR -> com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR;
			case SURVIVAL -> com.github.retrooper.packetevents.protocol.player.GameMode.SURVIVAL;
		});
	}

	private boolean available() {
		return plugin.getServer().getPluginManager().isPluginEnabled("packetevents")
			|| plugin.getServer().getPluginManager().isPluginEnabled("PacketEvents");
	}

	private void send(Player player, com.github.retrooper.packetevents.protocol.player.GameMode gameMode) {
		try {
			PacketEvents.getAPI().getPlayerManager().sendPacket(
				player,
				new WrapperPlayServerChangeGameState(
					WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE,
					gameMode.getId()
				)
			);
		} catch (Exception exception) {
			plugin.getLogger().warning("Failed to update screen HUD for " + player.getName() + ": " + exception.getMessage());
		}
	}
}
