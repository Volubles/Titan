package com.voluble.titanMC.onboarding.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.voluble.titanMC.display.notice.MessageDefaults;
import com.voluble.titanMC.display.notice.PluginMessageService;
import com.voluble.titanMC.onboarding.OnboardingService;
import com.voluble.titanMC.onboarding.config.OnboardingPreviewPoint;
import io.voluble.michellelib.commands.CommandModule;
import io.voluble.michellelib.commands.CommandRegistration;
import io.voluble.michellelib.commands.arguments.Args;
import io.voluble.michellelib.commands.context.MichelleCommandContext;
import io.voluble.michellelib.commands.suggest.Suggest;
import io.voluble.michellelib.commands.tree.CommandTree;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public final class OnboardingCommandModule implements CommandModule {
	private static final String ADMIN_PERMISSION = "titanmc.onboarding.admin";

	private final OnboardingService onboarding;
	private final PluginMessageService messages;

	public OnboardingCommandModule(OnboardingService onboarding, PluginMessageService messages) {
		this.onboarding = Objects.requireNonNull(onboarding, "onboarding");
		this.messages = Objects.requireNonNull(messages, "messages");
	}

	@Override
	public void register(CommandRegistration registration) {
		var onlinePlayers = Suggest.fromContext(source -> Bukkit.getOnlinePlayers().stream()
			.map(Player::getName)
			.sorted(String.CASE_INSENSITIVE_ORDER)
			.toList());
		var previewPoints = Suggest.fromContext(source -> Arrays.stream(OnboardingPreviewPoint.values())
			.map(OnboardingPreviewPoint::key)
			.toList());
		registration.register(CommandTree.root("onboarding")
			.aliases("ob")
			.description("Manage onboarding")
			.requiresPermission(ADMIN_PERMISSION)
			.literal("start", node -> node
				.executesPlayer((player, context) -> start(player))
				.argument("player", Args.word(), player -> player.suggests(onlinePlayers).executes(this::startOther)))
			.literalExec("stop", this::stop)
			.literalExec("reload", this::reload)
			.literal("reset", node -> node
				.argument("player", Args.word(), player -> player.executes(this::reset)))
			.literal("preview", preview -> preview
				.literal("set", set -> set
					.argument("point", Args.word(), point -> point
						.suggests(previewPoints)
						.executesPlayer(this::setPreviewPoint))))
			.literal("readiness", readiness -> readiness
				.literal("waiting-room", waitingRoom -> waitingRoom
					.literal("set", set -> set.executesPlayer(this::setWaitingRoom))))
			.spec());
	}

	private int start(Player player) {
		onboarding.start(player);
		return CommandTree.ok();
	}

	private int startOther(MichelleCommandContext context) throws CommandSyntaxException {
		Player target = Bukkit.getPlayerExact(context.arg("player", String.class));
		if (target == null) {
			messages.send(context.sender(), MessageDefaults.COMMAND_UNKNOWN_PLAYER);
			return CommandTree.ok();
		}
		onboarding.start(target);
		messages.send(context.sender(), MessageDefaults.ONBOARDING_STARTED_OTHER, args -> args.plain("player", target.getName()));
		return CommandTree.ok();
	}

	private int stop(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		onboarding.stop(player.getUniqueId(), true);
		messages.send(player, MessageDefaults.ONBOARDING_STOPPED);
		return CommandTree.ok();
	}

	private int reload(MichelleCommandContext context) {
		onboarding.reload();
		messages.send(context.sender(), MessageDefaults.ONBOARDING_RELOADED);
		return CommandTree.ok();
	}

	private int reset(MichelleCommandContext context) throws CommandSyntaxException {
		OfflinePlayer target = resolvePlayer(context.arg("player", String.class));
		if (target == null) {
			messages.send(context.sender(), MessageDefaults.COMMAND_UNKNOWN_PLAYER);
			return CommandTree.ok();
		}
		try {
			onboarding.reset(target.getUniqueId());
		} catch (SQLException exception) {
			messages.send(context.sender(), MessageDefaults.COMMAND_RUNTIME_ERROR, args -> args.plain("reason", exception.getMessage()));
			return CommandTree.ok();
		}
		messages.send(context.sender(), MessageDefaults.ONBOARDING_RESET, args -> args.plain("player", displayName(target)));
		return CommandTree.ok();
	}

	private int setPreviewPoint(Player player, MichelleCommandContext context) throws CommandSyntaxException {
		OnboardingPreviewPoint point;
		try {
			point = OnboardingPreviewPoint.parse(context.arg("point", String.class));
		} catch (IllegalArgumentException exception) {
			messages.send(player, MessageDefaults.COMMAND_RUNTIME_ERROR, args -> args.plain("reason", exception.getMessage()));
			return CommandTree.ok();
		}
		onboarding.capturePreviewPoint(player, point);
		messages.send(player, MessageDefaults.ONBOARDING_PREVIEW_POINT_SET, args -> args.plain("point", point.key()));
		return CommandTree.ok();
	}

	private int setWaitingRoom(Player player, MichelleCommandContext context) {
		onboarding.captureWaitingRoom(player);
		messages.send(player, MessageDefaults.ONBOARDING_WAITING_ROOM_SET);
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
