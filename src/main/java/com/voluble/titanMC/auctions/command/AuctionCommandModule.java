package com.voluble.titanMC.auctions.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.voluble.titanMC.auctions.AuctionPosition;
import com.voluble.titanMC.auctions.AuctionService;
import com.voluble.titanMC.ranks.model.WardId;
import com.voluble.titanMC.ranks.service.RankCatalog;
import io.voluble.michellelib.commands.CommandModule;
import io.voluble.michellelib.commands.CommandRegistration;
import io.voluble.michellelib.commands.arguments.Args;
import io.voluble.michellelib.commands.context.MichelleCommandContext;
import io.voluble.michellelib.commands.suggest.Suggest;
import io.voluble.michellelib.commands.tree.CommandTree;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class AuctionCommandModule implements CommandModule {
	private final AuctionService auctions;
	private final RankCatalog ranks;

	public AuctionCommandModule(AuctionService auctions, RankCatalog ranks) {
		this.auctions = auctions;
		this.ranks = ranks;
	}

	@Override
	public void register(CommandRegistration registration) {
		var names = Suggest.fromContext(source -> auctions.positions().stream().map(AuctionPosition::id).toList());
		var wards = Suggest.fromContext(source -> ranks.wards().stream().map(ward -> ward.id().value()).toList());
		registration.register(CommandTree.root("auction")
			.aliases("ah")
			.description("Manage thrift auction positions")
			.requiresPermission("titanmc.auction.admin")
			.requiresPlayerExecutor()
			.executes(this::root)
			.literal("position", position -> position
				.literal("add", add -> add.argument("ward", Args.word(), ward -> ward.suggests(wards).executes(this::add)))
				.literal("remove", remove -> remove.argument("name", Args.word(), name -> name.suggests(names).executes(this::remove)))
				.literalExec("list", this::list)
				.literal("teleport", teleport -> teleport.argument("name", Args.word(), name -> name.suggests(names).executes(this::teleport))))
			.spec());
	}

	private int root(MichelleCommandContext context) throws CommandSyntaxException {
		context.playerExecutor().sendMessage("Usage: /auction position <add|remove|list|teleport>");
		return CommandTree.ok();
	}

	private int add(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		var block = player.getTargetBlockExact(8);
		if (block == null) {
			player.sendMessage("Look at the block where the auction chest should spawn.");
			return CommandTree.ok();
		}
		try {
			BlockFace facing = player.getFacing().getOppositeFace();
			WardId wardId = WardId.of(context.arg("ward", String.class));
			ranks.requireWard(wardId);
			AuctionPosition position = auctions.addPosition(wardId, block.getLocation(), facing);
			player.sendMessage(
				"Added auction position " + position.id() + " in " + wardId.value()
					+ " ward. The chest and sign will face you."
			);
		} catch (RuntimeException exception) {
			player.sendMessage(exception.getMessage());
		}
		return CommandTree.ok();
	}

	private int remove(MichelleCommandContext context) throws CommandSyntaxException {
		try {
			auctions.removePosition(context.arg("name", String.class));
			context.playerExecutor().sendMessage("Auction position removed.");
		} catch (RuntimeException exception) {
			context.playerExecutor().sendMessage(exception.getMessage());
		}
		return CommandTree.ok();
	}

	private int list(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		if (auctions.positions().isEmpty()) {
			player.sendMessage("No auction positions configured.");
			return CommandTree.ok();
		}
		player.sendMessage("Auction positions:");
		for (AuctionPosition position : auctions.positions()) {
			String coordinates = position.x() + ", " + position.y() + ", " + position.z();
			var lot = auctions.atPosition(position.id());
			String status = lot == null ? "free" : lot.state().name().toLowerCase(java.util.Locale.ROOT);
			player.sendMessage(Component.text("- " + position.id() + " [" + position.wardId().value() + "] (" + coordinates + ") [" + status + "]")
				.clickEvent(ClickEvent.runCommand("/auction position teleport " + position.id()))
				.hoverEvent(HoverEvent.showText(Component.text("Click to teleport"))));
		}
		return CommandTree.ok();
	}

	private int teleport(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		try {
			AuctionPosition position = auctions.requiredPosition(context.arg("name", String.class));
			var world = Bukkit.getWorld(position.worldId());
			if (world == null) throw new IllegalStateException("The position's world is unavailable");
			Location chest = new Location(world, position.x() + 0.5, position.y() + 0.5, position.z() + 0.5);
			Location destination = chest.clone().add(
				position.facing().getModX() * 2.0,
				0.5,
				position.facing().getModZ() * 2.0
			);
			Vector direction = chest.toVector().subtract(destination.toVector());
			destination.setDirection(direction);
			player.teleportAsync(destination);
			player.sendMessage("Teleported to auction position " + position.id() + ".");
		} catch (RuntimeException exception) {
			player.sendMessage(exception.getMessage());
		}
		return CommandTree.ok();
	}
}
