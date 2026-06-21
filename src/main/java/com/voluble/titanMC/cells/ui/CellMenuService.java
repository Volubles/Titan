package com.voluble.titanMC.cells.ui;

import com.voluble.titanMC.cells.CellManagementService;
import com.voluble.titanMC.cells.CellManager;
import com.voluble.titanMC.cells.CellRentalService;
import com.voluble.titanMC.cells.config.CellsConfigurationManager;
import com.voluble.titanMC.cells.model.CellDefinition;
import com.voluble.titanMC.cells.model.CellLease;
import com.voluble.titanMC.util.ChatUtils;
import io.voluble.michellelib.menu.MenuService;
import io.voluble.michellelib.menu.item.ClickContext;
import io.voluble.michellelib.menu.item.Items;
import io.voluble.michellelib.menu.item.MenuItem;
import io.voluble.michellelib.menu.template.MenuDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CellMenuService {
	private final MenuService menus;
	private final CellManager cells;
	private final CellRentalService rentals;
	private final CellManagementService management;
	private final CellsConfigurationManager configuration;

	public CellMenuService(MenuService menus, CellManager cells, CellRentalService rentals, CellManagementService management, CellsConfigurationManager configuration) {
		this.menus = menus;
		this.cells = cells;
		this.rentals = rentals;
		this.management = management;
		this.configuration = configuration;
	}

	private static MenuItem playerButton(OfflinePlayer player, String action, java.util.function.Consumer<ClickContext> click) {
		ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
		if (stack.getItemMeta() instanceof SkullMeta meta) {
			meta.setOwningPlayer(player);
			meta.displayName(ChatUtils.formatItem("<white>" + (player.getName() == null ? player.getUniqueId() : player.getName())));
			meta.lore(List.of(ChatUtils.formatItem(action)));
			stack.setItemMeta(meta);
		}
		return new MenuItem() {
			public ItemStack render(Player viewer) {
				return stack.clone();
			}

			public boolean onClick(ClickContext context) {
				click.accept(context);
				return true;
			}
		};
	}

	private static MenuItem display(Material material, String name, List<String> lore) {
		return new Items.DisplayItem(item(material, name, lore));
	}

	private static MenuItem button(Material material, String name, List<String> lore, java.util.function.Consumer<ClickContext> click) {
		return new MenuItem() {
			public ItemStack render(Player viewer) {
				return item(material, name, lore);
			}

			public boolean onClick(ClickContext context) {
				click.accept(context);
				return true;
			}
		};
	}

	private static ItemStack item(Material material, String name, List<String> lore) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.displayName(ChatUtils.formatItem(name));
			meta.lore(lore.stream().map(ChatUtils::formatItem).toList());
			item.setItemMeta(meta);
		}
		return item;
	}

	private static Component title(String template, CellDefinition cell) {
		return MiniMessage.miniMessage().deserialize(template.replace("{display_name}", "<display_name>"), Placeholder.unparsed("display_name", cell.displayName()));
	}

	private static String duration(long millis) {
		Duration value = Duration.ofMillis(Math.max(0, millis));
		long days = value.toDays();
		if (days > 0) return days + "d " + value.minusDays(days).toHours() + "h";
		long hours = value.toHours();
		if (hours > 0) return hours + "h " + value.minusHours(hours).toMinutes() + "m";
		return value.toMinutes() + "m";
	}

	public void openFor(Player player, String cellId) {
		CellDefinition cell = cells.get(cellId);
		if (cell == null) {
			player.sendMessage("Unknown cell.");
			return;
		}
		CellLease lease = cells.lease(cellId);
		if (lease == null) openRental(player, cell);
		else if (lease.ownerId().equals(player.getUniqueId())) openManagement(player, cell);
		else openStatus(player, cell, lease);
	}

	private void openRental(Player player, CellDefinition cell) {
		MenuDefinition.chest(3).title(title(configuration.current().rentalMenuTitle(), cell)).onOpen(ctx -> {
			ctx.setItem(11, display(Material.CLOCK, "<green><bold>" + cell.displayName(), List.of("<gray>Price: <gold>$" + cell.rentPrice(), "<gray>Duration: <white>" + duration(cell.rentDurationSeconds() * 1000L))));
			ctx.setItem(15, button(Material.LIME_CONCRETE, "<green><bold>Confirm rental", List.of("<gray>Click to pay <gold>$" + cell.rentPrice()), click -> {
				click.actions().close();
				rentals.rent(click.player(), cell.id());
			}));
			ctx.setItem(22, new Items.CloseItem());
		}).build().open(menus, player);
	}

	private void openManagement(Player player, CellDefinition cell) {
		MenuDefinition.chest(3).title(title(configuration.current().managementMenuTitle(), cell)).refreshEveryTicks(20L).onOpen(ctx -> {
			CellLease lease = cells.lease(cell.id());
			if (lease == null) {
				ctx.setItem(13, display(Material.BARRIER, "<red>Lease ended", List.of()));
				return;
			}
			ctx.setItem(4, display(Material.CLOCK, "<green><bold>" + cell.displayName(), List.of("<gray>Time left: <yellow>" + duration(lease.expiresAtEpochMillis() - System.currentTimeMillis()), "<gray>Members: <white>" + cells.members(cell.id()).size())));
			ctx.setItem(10, button(Material.EMERALD, "<green><bold>Extend rent", List.of("<gray>Time left: <yellow>" + duration(lease.expiresAtEpochMillis() - System.currentTimeMillis()), "<gray>Cost: <gold>$" + cell.rentPrice(), "<gray>Adds: <white>" + duration(cell.rentDurationSeconds() * 1000L)), click -> management.extend(click.player(), cell.id(), success -> {
				if (success) openManagement(click.player(), cell);
			})));
			ctx.setItem(14, button(Material.PLAYER_HEAD, "<aqua><bold>Manage members", List.of("<gray>Add or remove access"), click -> click.actions().transition(() -> openMembers(click.player(), cell))));
			ctx.setItem(16, button(Material.RED_CONCRETE, "<red><bold>Sell back cell", List.of("<gray>This starts a full reset"), click -> click.actions().transition(() -> openReturnConfirmation(click.player(), cell))));
			ctx.setItem(22, new Items.CloseItem());
		}).build().open(menus, player);
	}

	private void openStatus(Player player, CellDefinition cell, CellLease lease) {
		OfflinePlayer owner = Bukkit.getOfflinePlayer(lease.ownerId());
		MenuDefinition.chest(3).title(title(configuration.current().managementMenuTitle(), cell)).onOpen(ctx -> {
			ctx.setItem(13, display(Material.OAK_SIGN, "<green><bold>" + cell.displayName(), List.of("<gray>Owner: <white>" + (owner.getName() == null ? owner.getUniqueId() : owner.getName()), "<gray>Time left: <yellow>" + duration(lease.expiresAtEpochMillis() - System.currentTimeMillis()), cells.members(cell.id()).contains(player.getUniqueId()) ? "<green>You are a member" : "<gray>This cell is occupied")));
			ctx.setItem(22, new Items.CloseItem());
		}).build().open(menus, player);
	}

	private void openReturnConfirmation(Player player, CellDefinition cell) {
		MenuDefinition.chest(3).title(Component.text("Confirm sellback")).onOpen(ctx -> {
			ctx.setItem(11, button(Material.LIME_CONCRETE, "<green><bold>Keep cell", List.of(), click -> click.actions().transition(() -> openManagement(click.player(), cell))));
			ctx.setItem(15, button(Material.RED_CONCRETE, "<red><bold>Sell back permanently", List.of("<red>All items in this cell will be removed", "<gray>and sold through the Storage Auction House."), click -> {
				click.actions().close();
				management.returnCell(click.player(), cell.id());
			}));
		}).build().open(menus, player);
	}

	private void openMembers(Player player, CellDefinition cell) {
		MenuDefinition.chest(6).title(title(configuration.current().membersMenuTitle(), cell)).onOpen(ctx -> {
			int slot = 0;
			for (UUID memberId : cells.members(cell.id())) {
				OfflinePlayer member = Bukkit.getOfflinePlayer(memberId);
				ctx.setItem(slot++, playerButton(member, "<red>Click to remove", click -> {
					if (management.removeMember(click.player(), cell.id(), memberId))
						click.actions().transition(() -> openMembers(click.player(), cell));
				}));
			}
			int addSlot = 27;
			for (Player candidate : Bukkit.getOnlinePlayers()) {
				if (candidate.getUniqueId().equals(player.getUniqueId()) || cells.members(cell.id()).contains(candidate.getUniqueId()))
					continue;
				if (addSlot >= 45) break;
				UUID candidateId = candidate.getUniqueId();
				ctx.setItem(addSlot++, playerButton(candidate, "<green>Click to add", click -> {
					if (management.addMember(click.player(), cell.id(), candidateId))
						click.actions().transition(() -> openMembers(click.player(), cell));
				}));
			}
			ctx.setItem(49, new Items.BackItem(() -> openManagement(player, cell)));
			ctx.setItem(53, new Items.CloseItem());
		}).build().open(menus, player);
	}
}
