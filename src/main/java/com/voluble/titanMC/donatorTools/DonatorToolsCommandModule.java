package com.voluble.titanMC.donatorTools;

import com.voluble.titanMC.TitanMC;
import com.voluble.titanMC.donatorTools.tools.blockPickaxe.BlockPickaxe;
import com.voluble.titanMC.donatorTools.tools.bountifulPickaxe.BountifulPickaxe;
import com.voluble.titanMC.donatorTools.tools.explosivePickaxe.ExplosivePickaxe;
import com.voluble.titanMC.donatorTools.tools.smeltingPickaxe.SmeltingPickaxe;
import com.voluble.titanMC.util.ChatUtils;
import io.voluble.michellelib.commands.CommandModule;
import io.voluble.michellelib.commands.CommandRegistration;
import io.voluble.michellelib.commands.arguments.Args;
import io.voluble.michellelib.commands.arguments.Resolve;
import io.voluble.michellelib.commands.context.MichelleCommandContext;
import io.voluble.michellelib.commands.errors.CommandErrors;
import io.voluble.michellelib.commands.tree.CommandTree;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class DonatorToolsCommandModule implements CommandModule {

	private static final List<String> TOOL_NAMES = List.of("smelting", "explosive", "bountiful", "block");

	@Override
	public void register(CommandRegistration registration) {
		registration.register(
			CommandTree.root("dtools")
				.description("Give a donator tool")
				.requiresAnyPermission("donatortools.give", "donatortools.reload")
				.executes(this::sendHelp)
				.literalExec("reload", this::reload)
				.argument("tool", Args.word(), toolNode -> toolNode
					.suggestStrings(TOOL_NAMES)
					.executes(this::giveToSelf)
					.argument("player", Args.player(), playerNode -> playerNode
						.executes(this::giveToPlayer)))
				.spec()
		);
	}

	private int sendHelp(MichelleCommandContext ctx) {
		Player player = ctx.sender() instanceof Player p ? p : null;
		List<Component> messages = List.of(
			ChatUtils.formatWithColor(player, "=== Donator Tools ===", ChatUtils.PRIMARY_COLOR),
			ChatUtils.formatWithColor(player, "Usage:", ChatUtils.INFO_COLOR),
			ChatUtils.format(player, "  <color:" + ChatUtils.SECONDARY_COLOR + ">/dtools <tool> [player]</color> <color:" + ChatUtils.MUTED_COLOR + ">- Give a donator tool</color>"),
			ChatUtils.format(player, "  <color:" + ChatUtils.SECONDARY_COLOR + ">/dtools reload</color> <color:" + ChatUtils.MUTED_COLOR + ">- Reload donator tools config</color>"),
			ChatUtils.formatWithColor(player, "Available tools:", ChatUtils.INFO_COLOR),
			ChatUtils.format(player, "  <color:" + ChatUtils.PRIMARY_COLOR + ">• smelting</color> <color:" + ChatUtils.MUTED_COLOR + ">- Automatically smelts ores</color>"),
			ChatUtils.format(player, "  <color:" + ChatUtils.WARNING_COLOR + ">• explosive</color> <color:" + ChatUtils.MUTED_COLOR + ">- Explodes blocks in 3x3x3 area</color>"),
			ChatUtils.format(player, "  <color:" + ChatUtils.SECONDARY_COLOR + ">• bountiful</color> <color:" + ChatUtils.MUTED_COLOR + ">- Drops the best ore from 3x3x3 area</color>"),
			ChatUtils.format(player, "  <color:" + ChatUtils.INFO_COLOR + ">• block</color> <color:" + ChatUtils.MUTED_COLOR + ">- Converts ores to block form</color>")
		);
		for (Component message : messages) {
			sendMessage(ctx.sender(), message);
		}
		return CommandTree.ok();
	}

	private int reload(MichelleCommandContext ctx) {
		if (!ctx.hasPermission("donatortools.reload")) {
			sendMessage(ctx.sender(), ChatUtils.formatWithColor("You do not have permission to reload the config!", ChatUtils.ERROR_COLOR));
			return CommandTree.ok();
		}
		try {
			TitanMC.getInstance().reloadDonatorToolsConfig();
			sendMessage(ctx.sender(), ChatUtils.formatWithColor("Donator tools config reloaded successfully!", ChatUtils.SUCCESS_COLOR));
			return CommandTree.ok();
		} catch (Exception e) {
			Component errorMsg = ChatUtils.formatWithColor("Error reloading config: " + e.getMessage(), ChatUtils.ERROR_COLOR);
			sendMessage(ctx.sender(), errorMsg);
			TitanMC.getInstance().getLogger().severe("Error reloading donator tools config: " + e.getMessage());
			return CommandTree.ok();
		}
	}

	private int giveToSelf(MichelleCommandContext ctx) throws CommandSyntaxException {
		if (!ctx.hasPermission("donatortools.give")) {
			sendMessage(ctx.sender(), ChatUtils.formatWithColor("You do not have permission to use this command!", ChatUtils.ERROR_COLOR));
			return CommandTree.ok();
		}
		if (!(ctx.executor() instanceof Player self)) {
			throw CommandErrors.playerOnly();
		}
		String toolType = ctx.arg("tool", String.class).toLowerCase();
		return giveTool(ctx, toolType, self);
	}

	private int giveToPlayer(MichelleCommandContext ctx) throws CommandSyntaxException {
		if (!ctx.hasPermission("donatortools.give")) {
			sendMessage(ctx.sender(), ChatUtils.formatWithColor("You do not have permission to use this command!", ChatUtils.ERROR_COLOR));
			return CommandTree.ok();
		}
		String toolType = ctx.arg("tool", String.class).toLowerCase();
		Player target = Resolve.player(ctx, "player");
		return giveTool(ctx, toolType, target);
	}

	private int giveTool(MichelleCommandContext ctx, String toolType, Player target) {
		CommandSender sender = ctx.sender();
		ItemStack tool = getToolByName(toolType);
		if (tool == null) {
			sendMessage(sender, ChatUtils.formatWithColor("Unknown tool type: " + toolType, ChatUtils.ERROR_COLOR));
			sendMessage(sender, ChatUtils.formatWithColor("Available tools: smelting, explosive, bountiful, block", ChatUtils.WARNING_COLOR));
			return CommandTree.ok();
		}
		Player senderPlayer = sender instanceof Player p ? p : null;
		Component toolDisplayName = getToolDisplayName(toolType, senderPlayer);
		if (target.getInventory().firstEmpty() == -1) {
			target.getWorld().dropItemNaturally(target.getLocation(), tool);
			Component fullInvPrefix = ChatUtils.formatWithColor(target, "Your inventory is full! The ", ChatUtils.WARNING_COLOR);
			Component fullInvSuffix = ChatUtils.formatWithColor(target, " was dropped on the ground.", ChatUtils.WARNING_COLOR);
			target.sendMessage(fullInvPrefix.append(toolDisplayName).append(fullInvSuffix));
		} else {
			target.getInventory().addItem(tool);
			Component receivedPrefix = ChatUtils.formatWithColor(target, "You have received a ", ChatUtils.PRIMARY_COLOR);
			Component exclamation = ChatUtils.formatWithColor(target, "!", ChatUtils.PRIMARY_COLOR);
			target.sendMessage(receivedPrefix.append(toolDisplayName).append(exclamation));
		}
		if (!target.equals(sender)) {
			Component gavePrefix = ChatUtils.formatWithColor(senderPlayer, "Gave ", ChatUtils.SUCCESS_COLOR);
			Component toSuffix = ChatUtils.formatWithColor(senderPlayer, " to " + target.getName() + "!", ChatUtils.SUCCESS_COLOR);
			sendMessage(sender, gavePrefix.append(getToolDisplayName(toolType, senderPlayer)).append(toSuffix));
		}
		return CommandTree.ok();
	}

	private static void sendMessage(CommandSender sender, Component message) {
		if (sender instanceof Player p) {
			p.sendMessage(message);
		} else {
			sender.sendMessage(ChatUtils.serialize(message));
		}
	}

	private static ItemStack getToolByName(String toolType) {
		return switch (toolType.toLowerCase()) {
			case "smelting", "smeltingpickaxe" -> SmeltingPickaxe.createPickaxe();
			case "explosive", "explosivepickaxe" -> ExplosivePickaxe.createPickaxe();
			case "bountiful", "bountifulpickaxe" -> BountifulPickaxe.createPickaxe();
			case "block", "blockpickaxe" -> BlockPickaxe.createPickaxe();
			default -> null;
		};
	}

	private static Component getToolDisplayName(String toolType, Player player) {
		return switch (toolType.toLowerCase()) {
			case "smelting", "smeltingpickaxe" -> ChatUtils.formatWithColor(player, "Smelting Pickaxe", ChatUtils.PRIMARY_COLOR);
			case "explosive", "explosivepickaxe" -> ChatUtils.formatWithColor(player, "Explosive Pickaxe", ChatUtils.WARNING_COLOR);
			case "bountiful", "bountifulpickaxe" -> ChatUtils.formatWithColor(player, "Bountiful Pickaxe", ChatUtils.SECONDARY_COLOR);
			case "block", "blockpickaxe" -> ChatUtils.formatWithColor(player, "Block Pickaxe", ChatUtils.INFO_COLOR);
			default -> ChatUtils.format(toolType);
		};
	}
}
