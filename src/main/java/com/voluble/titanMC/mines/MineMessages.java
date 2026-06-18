package com.voluble.titanMC.mines;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class MineMessages {

	private MineMessages() {}

	public static void sendNoMinesInstructions(Player player) {
		player.sendMessage(Component.text("There are no mines yet.", NamedTextColor.YELLOW));
		sendCreateInstructions(player);
	}

	public static void sendCreateInstructions(Player player) {
		player.sendMessage(Component.text("1. Make a cuboid selection with WorldEdit.", NamedTextColor.GRAY));
		player.sendMessage(Component.text("2. Run ", NamedTextColor.GRAY).append(
			Component.text("/mine create <name>", NamedTextColor.GREEN)
				.clickEvent(ClickEvent.suggestCommand("/mine create "))
				.hoverEvent(HoverEvent.showText(Component.text("Click to prepare the command")))
		));
	}
}
