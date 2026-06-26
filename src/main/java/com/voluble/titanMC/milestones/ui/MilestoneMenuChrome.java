package com.voluble.titanMC.milestones.ui;

import com.voluble.titanMC.util.ChatUtils;
import io.voluble.michellelib.menu.item.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class MilestoneMenuChrome {
	private MilestoneMenuChrome() {
	}

	static ItemStack filler() {
		ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.displayName(ChatUtils.formatItem(" "));
			item.setItemMeta(meta);
		}
		return item;
	}

	static MenuItem pageButton(Material material, String name, Runnable action) {
		ItemStack stack = new ItemStack(material);
		ItemMeta meta = stack.getItemMeta();
		if (meta != null) {
			meta.displayName(ChatUtils.formatItem(name));
			stack.setItemMeta(meta);
		}
		return new MenuItem() {
			@Override
			public ItemStack render(Player viewer) {
				return stack.clone();
			}

			@Override
			public boolean onClick(io.voluble.michellelib.menu.item.ClickContext context) {
				context.actions().transition(action);
				return true;
			}
		};
	}

	static int pages(int size, int pageSize) {
		if (size <= 0) return 1;
		return Math.max(1, (int) Math.ceil((double) size / pageSize));
	}

	static int clampPage(int page, int pages) {
		return Math.max(0, Math.min(Math.max(0, pages - 1), page));
	}
}
