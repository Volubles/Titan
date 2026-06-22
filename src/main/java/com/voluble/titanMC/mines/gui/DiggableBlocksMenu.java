package com.voluble.titanMC.mines.gui;

import com.voluble.titanMC.TitanMC;
import com.voluble.titanMC.mines.Mine;
import com.voluble.titanMC.mines.MineManager;
import com.voluble.titanMC.mines.breaking.MineBreakProfile;
import com.voluble.titanMC.util.ChatUtils;
import io.voluble.michellelib.menu.MenuService;
import io.voluble.michellelib.menu.item.ClickContext;
import io.voluble.michellelib.menu.item.Items;
import io.voluble.michellelib.menu.item.MenuItem;
import io.voluble.michellelib.menu.template.MenuDefinition;
import io.voluble.michellelib.menu.template.MenuContextView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Comparator;
import java.util.List;

public final class DiggableBlocksMenu {
	private DiggableBlocksMenu() {
	}

	public static void open(Player player, Mine mine, MineManager manager) {
		MenuService menus = TitanMC.getInstance().getMenuService();
		MenuDefinition.chest(6)
			.title(ignored -> Component.text("Diggable Blocks: " + mine.getName(), NamedTextColor.GOLD)
				.decoration(TextDecoration.BOLD, true))
			.onOpen(ctx -> populate(ctx, mine.getName(), manager))
			.build()
			.open(menus, player);
	}

	private static void populate(MenuContextView ctx, String mineName, MineManager manager) {
		Mine mine = manager.get(mineName);
		if (mine == null) return;
		int slot = 0;
		if (mine.getBreakProfile() instanceof MineBreakProfile.AllowList allowList) {
			List<Material> materials = allowList.materials().stream().sorted(Comparator.comparing(Enum::name)).toList();
			for (Material material : materials) {
				if (slot >= 45) break;
				ctx.setItem(slot++, materialItem(mineName, material, manager));
			}
			while (slot < 45) ctx.setItem(slot++, new AddMaterialSlot(mineName, manager));
		}
		ctx.setItem(45, modeToggle(mineName, manager));
		ctx.setItem(49, new Items.BackItem(() -> MineEditMenu.open(ctx.player(), manager.get(mineName), manager)));
		ctx.setItem(53, new Items.CloseItem());
	}

	private static MenuItem modeToggle(String mineName, MineManager manager) {
		return new MenuItem() {
			@Override
			public ItemStack render(Player viewer) {
				Mine mine = manager.get(mineName);
				boolean restricted = mine != null && mine.getBreakProfile() instanceof MineBreakProfile.AllowList;
				ItemStack item = new ItemStack(restricted ? Material.IRON_BARS : Material.STRUCTURE_VOID);
				ItemMeta meta = item.getItemMeta();
				meta.displayName(ChatUtils.formatItem(restricted ? "<yellow><bold>Allow List" : "<green><bold>Unrestricted"));
				meta.lore(List.of(
					ChatUtils.formatItem(restricted ? "<gray>Only listed blocks can be broken" : "<gray>Every block can be broken"),
					ChatUtils.formatItem("<white>Click to change mode")
				));
				item.setItemMeta(meta);
				return item;
			}

			@Override
			public boolean onClick(ClickContext ctx) {
				Mine mine = manager.get(mineName);
				if (mine == null) return true;
				if (mine.getBreakProfile() instanceof MineBreakProfile.AllowList) manager.useUnrestrictedBreaking(mineName);
				else manager.useDiggableAllowList(mineName);
				ctx.actions().transition(() -> DiggableBlocksMenu.open(ctx.player(), manager.get(mineName), manager));
				return true;
			}
		};
	}

	private static MenuItem materialItem(String mineName, Material material, MineManager manager) {
		return new MenuItem() {
			@Override
			public ItemStack render(Player viewer) {
				ItemStack item = new ItemStack(material);
				ItemMeta meta = item.getItemMeta();
				meta.displayName(ChatUtils.formatItem("<white>" + displayName(material)));
				meta.lore(List.of(ChatUtils.formatItem("<red>Click to remove")));
				item.setItemMeta(meta);
				return item;
			}

			@Override
			public boolean onClick(ClickContext ctx) {
				manager.removeDiggableMaterial(mineName, material);
				ctx.actions().transition(() -> DiggableBlocksMenu.open(ctx.player(), manager.get(mineName), manager));
				return true;
			}
		};
	}

	private static final class AddMaterialSlot implements MenuItem {
		private final String mineName;
		private final MineManager manager;

		private AddMaterialSlot(String mineName, MineManager manager) {
			this.mineName = mineName;
			this.manager = manager;
		}

		@Override
		public ItemStack render(Player viewer) {
			ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
			ItemMeta meta = item.getItemMeta();
			meta.displayName(ChatUtils.formatItem("<green><bold>Add Diggable Block"));
			meta.lore(List.of(ChatUtils.formatItem("<gray>Place a block here to allow it")));
			item.setItemMeta(meta);
			return item;
		}

		@Override public boolean onClick(ClickContext ctx) { return true; }
		@Override public boolean isPlaceable() { return true; }
		@Override public boolean allowNumberKeyPlace() { return false; }
		@Override public boolean allowShiftInsert() { return false; }
		@Override public boolean returnPlacedItems() { return false; }

		@Override
		public boolean canAccept(ItemStack stack, ClickContext ctx) {
			if (stack == null || stack.getType().isAir() || !stack.getType().isBlock()) return false;
			Mine mine = manager.get(mineName);
			return mine != null && mine.getBreakProfile() instanceof MineBreakProfile.AllowList allowList
				&& !allowList.materials().contains(stack.getType());
		}

		@Override public int maxAcceptAmount(ItemStack stack, ClickContext ctx) { return 1; }

		@Override
		public void onInsert(ItemStack inserted, ClickContext ctx) {
			manager.addDiggableMaterial(mineName, inserted.getType());
			ctx.actions().transition(() -> DiggableBlocksMenu.open(ctx.player(), manager.get(mineName), manager));
		}
	}

	private static String displayName(Material material) {
		return material.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
	}
}
