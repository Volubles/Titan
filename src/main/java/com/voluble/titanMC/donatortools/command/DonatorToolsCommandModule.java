package com.voluble.titanMC.donatortools.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.voluble.titanMC.donatortools.DonatorToolsService;
import com.voluble.titanMC.donatortools.item.DonatorToolRegistry;
import com.voluble.titanMC.donatortools.item.DonatorToolType;
import com.voluble.titanMC.util.ChatUtils;
import io.voluble.michellelib.commands.CommandModule;
import io.voluble.michellelib.commands.CommandRegistration;
import io.voluble.michellelib.commands.arguments.Args;
import io.voluble.michellelib.commands.arguments.Resolve;
import io.voluble.michellelib.commands.context.MichelleCommandContext;
import io.voluble.michellelib.commands.errors.CommandErrors;
import io.voluble.michellelib.commands.tree.CommandTree;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public final class DonatorToolsCommandModule implements CommandModule {

	private final DonatorToolsService service;
	private final DonatorToolRegistry tools;

	public DonatorToolsCommandModule(DonatorToolsService service) {
		this.service = Objects.requireNonNull(service, "service");
		this.tools = service.registry();
	}

	@Override
	public void register(CommandRegistration registration) {
		registration.register(
			CommandTree.root("dtools")
				.description("Give a donator tool")
				.requiresAnyPermission("donatortools.give", "donatortools.reload")
				.executes(this::sendHelp)
				.literalExec("reload", this::reload)
				.argument("tool", Args.word(), tool -> tool
					.suggestStrings(tools.ids())
					.executes(this::giveToSelf)
					.argument("player", Args.player(), player -> player
						.executes(this::giveToPlayer)))
				.spec()
		);
	}

	private int sendHelp(MichelleCommandContext context) {
		send(context.sender(), Component.text("Donator Tools", NamedTextColor.GOLD));
		send(context.sender(), Component.text("/dtools <tool> [player]", NamedTextColor.AQUA));
		send(context.sender(), Component.text("/dtools reload", NamedTextColor.AQUA));
		for (DonatorToolType type : DonatorToolType.values()) {
			send(context.sender(), Component.text(
				type.id() + " - " + type.description(),
				type.color()
			));
		}
		return CommandTree.ok();
	}

	private int reload(MichelleCommandContext context) {
		if (!context.hasPermission("donatortools.reload")) {
			send(context.sender(), Component.text("You may not reload donator tools.", NamedTextColor.RED));
			return CommandTree.ok();
		}
		try {
			service.reload();
			send(context.sender(), Component.text("Donator tools reloaded.", NamedTextColor.GREEN));
		} catch (IllegalArgumentException | IllegalStateException exception) {
			send(context.sender(), Component.text(
				"Donator tools reload failed: " + exception.getMessage(),
				NamedTextColor.RED
			));
		}
		return CommandTree.ok();
	}

	private int giveToSelf(MichelleCommandContext context) throws CommandSyntaxException {
		if (!(context.executor() instanceof Player player)) throw CommandErrors.playerOnly();
		return give(context, player);
	}

	private int giveToPlayer(MichelleCommandContext context) throws CommandSyntaxException {
		return give(context, Resolve.player(context, "player"));
	}

	private int give(MichelleCommandContext context, Player target) {
		if (!context.hasPermission("donatortools.give")) {
			send(context.sender(), Component.text("You may not give donator tools.", NamedTextColor.RED));
			return CommandTree.ok();
		}
		String input = context.arg("tool", String.class);
		DonatorToolType type = tools.find(input).orElse(null);
		if (type == null) {
			send(context.sender(), Component.text(
				"Unknown tool. Available: " + String.join(", ", tools.ids()),
				NamedTextColor.RED
			));
			return CommandTree.ok();
		}
		ItemStack item = tools.create(type);
		var remaining = target.getInventory().addItem(item);
		if (!remaining.isEmpty()) {
			target.getWorld().dropItemNaturally(target.getLocation(), item);
			target.sendMessage(Component.text(
				"Your inventory was full, so the " + type.displayName() + " was dropped.",
				NamedTextColor.YELLOW
			));
		} else {
			target.sendMessage(Component.text(
				"You received a " + type.displayName() + ".",
				type.color()
			));
		}
		if (!target.equals(context.sender())) {
			send(context.sender(), Component.text(
				"Gave " + type.displayName() + " to " + target.getName() + ".",
				NamedTextColor.GREEN
			));
		}
		return CommandTree.ok();
	}

	private static void send(CommandSender sender, Component message) {
		if (sender instanceof Player player) player.sendMessage(message);
		else sender.sendMessage(ChatUtils.serialize(message));
	}
}
