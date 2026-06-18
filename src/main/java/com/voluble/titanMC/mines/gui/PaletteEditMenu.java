package com.voluble.titanMC.mines.gui;

import com.voluble.titanMC.TitanMC;
import com.voluble.titanMC.mines.Mine;
import com.voluble.titanMC.mines.MineManager;
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
import java.util.Map;

public class PaletteEditMenu {

	public static void open(Player player, Mine mine, MineManager manager) {
		MenuService menus = TitanMC.getInstance().getMenuService();

		MenuDefinition.chest(6)
			.title(t -> Component.text("Edit Palette: " + mine.getName()).color(NamedTextColor.GOLD)
					.decoration(TextDecoration.BOLD, true))
			.onOpen(ctx -> {
				int slot = 0;
				Map<Material, Integer> entries = mine.getPalette().getEntriesView();
				for (Map.Entry<Material, Integer> entry : entries.entrySet()) {
					if (slot >= 45) break;
					ctx.setItem(slot++, createPaletteEntryItem(entry.getKey(), entry.getValue(), mine, manager, player));
				}

				int emptySlots = Math.max(0, 45 - entries.size());
				for (int i = 0; i < emptySlots; i++) {
					if (slot >= 45) break;
					ctx.setItem(slot++, new AddBlockSlot(mine, manager, player));
				}

				ctx.setItem(45, createInfoItem(mine));
				ctx.setItem(49, new Items.BackItem(() -> MineEditMenu.open(player, mine, manager)));
				ctx.setItem(53, new Items.CloseItem());
			})
			.build()
			.open(menus, player);
	}

	private static MenuItem createInfoItem(Mine mine) {
		return new MenuItem() {
			@Override
			public ItemStack render(Player viewer) {
				ItemStack item = new ItemStack(Material.PAINTING);
				ItemMeta meta = item.getItemMeta();
				if (meta == null) return item;

				meta.displayName(ChatUtils.formatItem("<aqua><bold>Palette Info"));

				int totalWeight = mine.getPalette().getTotalWeight();
				Map<Material, Integer> entries = mine.getPalette().getEntriesView();
				List<Component> lore = new ArrayList<>();
				lore.add(ChatUtils.formatItem("<yellow>Total Weight: " + totalWeight));
				lore.add(ChatUtils.formatItem("<gray>Blocks: " + entries.size()));
				lore.add(Component.empty());
				lore.add(ChatUtils.formatItem("<green>Place blocks to add"));
				lore.add(ChatUtils.formatItem("<gray>Weight = chance"));

				meta.lore(lore);
				item.setItemMeta(meta);
				return item;
			}

			@Override
			public boolean onClick(ClickContext ctx) {
				return true;
			}
		};
	}

	private static MenuItem createPaletteEntryItem(Material mat, int weight, Mine mine, MineManager manager,
			Player player) {
		return new PaletteEntrySlot(mat, weight, mine, manager, player);
	}

	private static class PaletteEntrySlot implements MenuItem {
		private final Material material;
		private int weight;
		private final Mine mine;
		private final MineManager manager;

		public PaletteEntrySlot(Material material, int weight, Mine mine, MineManager manager, Player player) {
			this.material = material;
			this.weight = weight;
			this.mine = mine;
			this.manager = manager;
		}

		@Override
		public ItemStack render(Player viewer) {
			ItemStack item = new ItemStack(material);
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				meta.displayName(ChatUtils.formatItem("<white>" + formatMaterialName(material.name())));

				List<Component> lore = new ArrayList<>();
				lore.add(ChatUtils.formatItem("<yellow>Weight: " + weight));
				lore.add(Component.empty());
				lore.add(ChatUtils.formatItem("<green>Left-click to increase"));
				lore.add(ChatUtils.formatItem("<red>Right-click to decrease"));
				lore.add(ChatUtils.formatItem("<dark_red>Shift-click to delete"));

				meta.lore(lore);
				item.setItemMeta(meta);
			}
			return item;
		}

		@Override
		public boolean onClick(ClickContext ctx) {
			if (ctx.shiftClick()) {
				manager.paletteRemove(mine.getName(), material);
				ctx.actions().transition(() -> PaletteEditMenu.open(ctx.player(), manager.get(mine.getName()), manager));
				return true;
			}
			if (ctx.click().toString().contains("LEFT")) {
				weight = Math.min(100, weight + 1);
			} else if (ctx.click().toString().contains("RIGHT")) {
				weight = Math.max(1, weight - 1);
			}
			manager.paletteAddOrUpdate(mine.getName(), material, weight);
			ctx.actions().transition(() -> PaletteEditMenu.open(ctx.player(), manager.get(mine.getName()), manager));
			return true;
		}

		@Override
		public boolean isPlaceable() {
			return true;
		}

		@Override
		public boolean canAccept(ItemStack stack, ClickContext ctx) {
			return false;
		}

		@Override
		public boolean allowNumberKeyPlace() {
			return false;
		}

		@Override
		public boolean allowShiftInsert() {
			return false;
		}
	}

	private static class AddBlockSlot implements MenuItem {
		private final Mine mine;
		private final MineManager manager;

		public AddBlockSlot(Mine mine, MineManager manager, Player player) {
			this.mine = mine;
			this.manager = manager;
		}

		@Override
		public ItemStack render(Player viewer) {
			ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				meta.displayName(ChatUtils.formatItem("<green><bold>Add Block"));
				meta.lore(List.of(
						ChatUtils.formatItem("<gray>Place a block here to add"),
						ChatUtils.formatItem("<yellow>Weight: 1")
				));
				item.setItemMeta(meta);
			}
			return item;
		}

		@Override
		public boolean onClick(ClickContext ctx) {
			return true;
		}

		@Override
		public boolean isPlaceable() {
			return true;
		}

		@Override
		public boolean canAccept(ItemStack stack, ClickContext ctx) {
			if (stack == null || stack.getType().isAir()) return false;
			return !mine.getPalette().getEntriesView().containsKey(stack.getType());
		}

		@Override
		public int maxAcceptAmount(ItemStack stack, ClickContext ctx) {
			return 1;
		}

		@Override
		public void onInsert(ItemStack inserted, ClickContext ctx) {
			manager.paletteAddOrUpdate(mine.getName(), inserted.getType(), 1);
			ctx.actions().transition(() -> PaletteEditMenu.open(ctx.player(), manager.get(mine.getName()), manager));
		}

		@Override
		public boolean allowNumberKeyPlace() {
			return false;
		}

		@Override
		public boolean allowShiftInsert() {
			return false;
		}

		@Override
		public boolean returnPlacedItems() {
			return false;
		}
	}

	private static String formatMaterialName(String materialName) {
		return materialName.replace('_', ' ').toLowerCase();
	}
}
