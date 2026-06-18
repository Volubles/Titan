package com.voluble.titanMC.mines.gui;

import com.voluble.titanMC.TitanMC;
import com.voluble.titanMC.mines.Mine;
import com.voluble.titanMC.mines.MineManager;
import com.voluble.titanMC.mines.MineMessages;
import com.voluble.titanMC.util.ChatUtils;
import io.voluble.michellelib.menu.MenuService;
import io.voluble.michellelib.menu.item.ClickContext;
import io.voluble.michellelib.menu.item.Items;
import io.voluble.michellelib.menu.item.MenuItem;
import io.voluble.michellelib.menu.template.MenuDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MineListMenu {

	public static void open(Player player, MineManager manager) {
		if (manager.getAll().isEmpty()) {
			player.closeInventory();
			MineMessages.sendNoMinesInstructions(player);
			return;
		}
		MenuService menus = TitanMC.getInstance().getMenuService();

		MenuDefinition.chest(6)
			.title(Component.text("Mine Manager").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			.onOpen(ctx -> {
				List<Mine> mines = new ArrayList<>(manager.getAll());
				int slot = 0;
				for (Mine mine : mines) {
					if (slot >= 45) break;
					ctx.setItem(slot++, createMineDisplayItem(mine, manager));
				}

				int emptySlots = Math.max(0, 45 - mines.size());
				for (int i = 0; i < emptySlots; i++) {
					if (slot >= 45) break;
					ctx.setItem(slot++, new Items.DisplayItem(createEmptySlot()));
				}

				ctx.setItem(49, createCreateButton(manager));
				ctx.setItem(53, new Items.CloseItem());
			})
			.build()
			.open(menus, player);
	}

	private static MenuItem createMineDisplayItem(Mine mine, MineManager manager) {
		return new MenuItem() {
			@Override
			public ItemStack render(Player viewer) {
				ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
				ItemMeta meta = item.getItemMeta();
				if (meta == null) return item;

				meta.displayName(ChatUtils.formatItem("<aqua><bold>" + mine.getName()));

				Component statusColor = mine.isEnabled() ? Component.text("ENABLED").color(NamedTextColor.GREEN)
						: Component.text("DISABLED").color(NamedTextColor.RED);
				List<Component> lore = new ArrayList<>();
				lore.add(ChatUtils.formatItem("<gray>Status: ").append(statusColor));
				lore.add(Component.empty());
				lore.add(ChatUtils.formatItem("<yellow>Reset Interval: " + mine.getResetIntervalSeconds() + "s"));
				lore.add(ChatUtils.formatItem("<yellow>Batch Size: " + mine.getBatchSizePerTick()));
				if (mine.getAutoResetBelowPercent() >= 0) {
					lore.add(ChatUtils.formatItem("<gold>Auto Reset: < " + mine.getAutoResetBelowPercent() + "%"));
				}
				lore.add(Component.empty());
				lore.add(ChatUtils.formatItem("<gray>Blocks: " + mine.getTotalBlockCountSafe()));
				lore.add(ChatUtils.formatItem("<gray>Broken: " + mine.getBrokenBlocks()));
				lore.add(ChatUtils.formatItem("<gray>Remaining: " + mine.getRemainingPercent() + "%"));
				lore.add(Component.empty());
				lore.add(ChatUtils.formatItem("<green>Click to edit"));

				meta.lore(lore);
				item.setItemMeta(meta);
				return item;
			}

			@Override
			public boolean onClick(ClickContext ctx) {
				ctx.actions().transition(() -> MineEditMenu.open(ctx.player(), mine, manager));
				return true;
			}
		};
	}

	private static MenuItem createCreateButton(MineManager manager) {
		return new MenuItem() {
			@Override
			public ItemStack render(Player viewer) {
				ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
				ItemMeta meta = item.getItemMeta();
				if (meta == null) return item;

				meta.displayName(ChatUtils.formatItem("<green><bold>Create New Mine"));
				meta.lore(List.of(ChatUtils.formatItem("<green>Click for WorldEdit creation instructions")));
				item.setItemMeta(meta);
				return item;
			}

			@Override
			public boolean onClick(ClickContext ctx) {
				ctx.actions().close();
				MineMessages.sendCreateInstructions(ctx.player());
				return true;
			}
		};
	}

	private static ItemStack createEmptySlot() {
		ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
		}
		return item;
	}
}
