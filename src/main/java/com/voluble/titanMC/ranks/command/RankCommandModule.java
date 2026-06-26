package com.voluble.titanMC.ranks.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.voluble.titanMC.display.notice.MessageDefaults;
import com.voluble.titanMC.display.notice.PluginMessageService;
import com.voluble.titanMC.ranks.model.PlayerRank;
import com.voluble.titanMC.ranks.model.PrisonRank;
import com.voluble.titanMC.ranks.model.RankId;
import com.voluble.titanMC.ranks.model.RankupRequirement;
import com.voluble.titanMC.ranks.model.WardDefinition;
import com.voluble.titanMC.ranks.service.PlayerRankService;
import com.voluble.titanMC.ranks.service.RankCatalog;
import com.voluble.titanMC.ranks.service.RankupResult;
import com.voluble.titanMC.ranks.service.RankupService;
import io.voluble.michellelib.commands.CommandModule;
import io.voluble.michellelib.commands.CommandRegistration;
import io.voluble.michellelib.commands.arguments.Args;
import io.voluble.michellelib.commands.context.MichelleCommandContext;
import io.voluble.michellelib.commands.suggest.Suggest;
import io.voluble.michellelib.commands.tree.CommandTree;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class RankCommandModule implements CommandModule {
	private static final String USE_PERMISSION = "titanmc.rank.use";
	private static final String ADMIN_PERMISSION = "titanmc.rank.admin";

	private final RankCatalog catalog;
	private final PlayerRankService players;
	private final RankupService rankups;
	private final PluginMessageService messages;

	public RankCommandModule(RankCatalog catalog, PlayerRankService players, RankupService rankups, PluginMessageService messages) {
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.players = Objects.requireNonNull(players, "players");
		this.rankups = Objects.requireNonNull(rankups, "rankups");
		this.messages = Objects.requireNonNull(messages, "messages");
	}

	@Override
	public void register(CommandRegistration registration) {
		var rankIds = Suggest.fromContext(source -> catalog.ranks().stream().map(rank -> rank.id().value()).toList());
		registration.register(CommandTree.root("rank")
			.aliases("ranks")
			.description("Show or manage prison ranks")
			.requiresAnyPermission(USE_PERMISSION, ADMIN_PERMISSION)
			.executesPlayer((player, ctx) -> showOwnRank(player))
			.literalExec("list", this::list)
			.literal("info", node -> node
				.requiresPermission(ADMIN_PERMISSION)
				.argument("player", Args.word(), name -> name.executes(this::info)))
			.literal("set", node -> node
				.requiresPermission(ADMIN_PERMISSION)
				.argument("player", Args.word(), name -> name
					.argument("rank", Args.word(), rank -> rank.suggests(rankIds).executes(this::set))))
			.spec());
		registration.register(CommandTree.root("rankup")
			.description("Purchase the next prison rank")
			.requiresPermission(USE_PERMISSION)
			.executesPlayer((player, ctx) -> rankup(player))
			.spec());
	}

	private int showOwnRank(Player player) {
		Optional<PlayerRank> rank = players.current(player.getUniqueId());
		if (rank.isEmpty()) {
			messages.send(player, MessageDefaults.RANK_NO_RANK);
			return CommandTree.ok();
		}
		PrisonRank prisonRank = catalog.requireRank(rank.get().rankId());
		messages.send(player, MessageDefaults.RANK_OWN, args -> args.plain("rank", prisonRank.displayName()));
		catalog.nextRank(prisonRank.id()).ifPresent(next -> {
			RankupRequirement requirement = next.rankup().orElseThrow();
			messages.send(player, MessageDefaults.RANK_NEXT, args -> args
				.plain("rank", next.displayName())
				.plain("cost", requirement.cost()));
		});
		return CommandTree.ok();
	}

	private int list(MichelleCommandContext context) throws CommandSyntaxException {
		CommandSender sender = context.sender();
		for (WardDefinition ward : catalog.wards()) {
			sender.sendMessage(ward.displayName() + " (" + ward.id().value() + ")");
			for (RankId rankId : ward.ranks()) {
				PrisonRank rank = catalog.requireRank(rankId);
				String cost = rank.rankup()
					.map(requirement -> "$" + requirement.cost())
					.orElse("starter");
				sender.sendMessage("  - " + rank.displayName() + " [" + cost + "]");
			}
		}
		return CommandTree.ok();
	}

	private int info(MichelleCommandContext context) throws CommandSyntaxException {
		CommandSender sender = context.sender();
		OfflinePlayer target = resolvePlayer(context.arg("player", String.class));
		if (target == null) {
			messages.send(sender, MessageDefaults.COMMAND_UNKNOWN_PLAYER);
			return CommandTree.ok();
		}
		Optional<PlayerRank> rank = players.current(target.getUniqueId());
		if (rank.isEmpty()) {
			messages.send(sender, MessageDefaults.RANK_PLAYER_NO_RANK, args -> args.plain("player", displayName(target)));
			return CommandTree.ok();
		}
		PrisonRank prisonRank = catalog.requireRank(rank.get().rankId());
		messages.send(sender, MessageDefaults.RANK_PLAYER_INFO, args -> args
			.plain("player", displayName(target))
			.plain("rank", prisonRank.displayName()));
		return CommandTree.ok();
	}

	private int set(MichelleCommandContext context) throws CommandSyntaxException {
		CommandSender sender = context.sender();
		OfflinePlayer target = resolvePlayer(context.arg("player", String.class));
		if (target == null) {
			messages.send(sender, MessageDefaults.COMMAND_UNKNOWN_PLAYER);
			return CommandTree.ok();
		}
		RankId rankId;
		try {
			rankId = RankId.of(context.arg("rank", String.class));
		} catch (IllegalArgumentException exception) {
			messages.send(sender, MessageDefaults.RANK_INVALID_ID, args -> args.plain("reason", exception.getMessage()));
			return CommandTree.ok();
		}
		PrisonRank prisonRank = catalog.findRank(rankId).orElse(null);
		if (prisonRank == null) {
			messages.send(sender, MessageDefaults.RANK_UNKNOWN, args -> args.plain("rank", rankId.value()));
			return CommandTree.ok();
		}
		PlayerRank updated = players.current(target.getUniqueId())
			.map(existing -> existing.withRank(prisonRank.id(), System.currentTimeMillis()))
			.orElseGet(() -> new PlayerRank(target.getUniqueId(), prisonRank.id(), System.currentTimeMillis()));
		players.apply(updated);
		messages.send(sender, MessageDefaults.RANK_SET, args -> args
			.plain("player", displayName(target))
			.plain("rank", prisonRank.displayName()));
		return CommandTree.ok();
	}

	private int rankup(Player player) {
		RankupResult result = rankups.rankup(player.getUniqueId());
		switch (result) {
			case RankupResult.Success success -> messages.send(player, MessageDefaults.RANKUP_SUCCESS, args -> args
				.plain("rank", catalog.requireRank(success.current().rankId()).displayName())
				.plain("cost", success.charged()));
			case RankupResult.AtMaxRank ignored -> messages.send(player, MessageDefaults.RANKUP_MAX);
			case RankupResult.MissingRequirement missing -> messages.send(player, MessageDefaults.RANKUP_MISSING_REQUIREMENT, args -> args
				.plain("required", catalog.requireRank(missing.required()).displayName())
				.plain("next", missing.next().displayName()));
			case RankupResult.InsufficientFunds funds -> messages.send(player, MessageDefaults.RANKUP_INSUFFICIENT_FUNDS, args -> args
				.plain("needed", funds.needed())
				.plain("rank", funds.next().displayName())
				.plain("balance", (long) funds.balance()));
			case RankupResult.EconomyUnavailable ignored -> messages.send(player, MessageDefaults.RANKUP_ECONOMY_UNAVAILABLE);
			case RankupResult.PersistenceFailure failure -> messages.send(
				player,
				failure.refunded() ? MessageDefaults.RANKUP_SAVE_REFUNDED : MessageDefaults.RANKUP_SAVE_REFUND_FAILED
			);
			case RankupResult.NoCurrentRank ignored -> messages.send(player, MessageDefaults.RANKUP_NO_CURRENT_RANK);
		}
		return CommandTree.ok();
	}

	private static OfflinePlayer resolvePlayer(String input) {
		try {
			return Bukkit.getOfflinePlayer(UUID.fromString(input));
		} catch (IllegalArgumentException ignored) {
			return Bukkit.getOfflinePlayerIfCached(input);
		}
	}

	private static String displayName(OfflinePlayer player) {
		return player.getName() == null ? player.getUniqueId().toString() : player.getName();
	}
}
