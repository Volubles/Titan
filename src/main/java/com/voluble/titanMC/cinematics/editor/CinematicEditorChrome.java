package com.voluble.titanMC.cinematics.editor;

import io.voluble.michellelib.menu.item.ClickContext;
import io.voluble.michellelib.menu.item.MenuItem;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

final class CinematicEditorChrome {
	private CinematicEditorChrome() {
	}

	static MenuItem button(CinematicEditorItemFactory items, Material material, String name, List<String> lore, Consumer<ClickContext> click) {
		return item(items.item(material, name, lore), click);
	}

	static MenuItem item(ItemStack stack, Consumer<ClickContext> click) {
		return new MenuItem() {
			@Override
			public ItemStack render(Player viewer) {
				return stack.clone();
			}

			@Override
			public boolean onClick(ClickContext context) {
				playClickSound(context.player());
				click.accept(context);
				return true;
			}
		};
	}

	static MenuItem display(ItemStack stack) {
		return new MenuItem() {
			@Override
			public ItemStack render(Player viewer) {
				return stack.clone();
			}

			@Override
			public boolean onClick(ClickContext context) {
				return true;
			}
		};
	}

	private static void playClickSound(Player player) {
		player.playSound(player.getLocation(), "minecraft:ui.button.click", SoundCategory.MASTER, 0.35F, 1.35F);
	}
}
