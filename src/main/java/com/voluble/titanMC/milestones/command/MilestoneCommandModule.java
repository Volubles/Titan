package com.voluble.titanMC.milestones.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.voluble.titanMC.display.notice.MessageDefaults;
import com.voluble.titanMC.display.notice.PluginMessageService;
import com.voluble.titanMC.milestones.config.MilestoneConfigurationManager;
import com.voluble.titanMC.milestones.model.MilestoneProgress;
import com.voluble.titanMC.milestones.model.MilestoneTrack;
import com.voluble.titanMC.milestones.service.MilestoneService;
import com.voluble.titanMC.milestones.ui.MilestoneMenuService;
import io.voluble.michellelib.commands.CommandModule;
import io.voluble.michellelib.commands.CommandRegistration;
import io.voluble.michellelib.commands.arguments.Args;
import io.voluble.michellelib.commands.context.MichelleCommandContext;
import io.voluble.michellelib.commands.tree.CommandTree;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class MilestoneCommandModule implements CommandModule {
	private static final String USE_PERMISSION = "titanmc.milestones.use";
	private static final String ADMIN_PERMISSION = "titanmc.milestones.admin";

	private final MilestoneMenuService menus;
	private final MilestoneConfigurationManager configuration;
	private final MilestoneService milestones;
	private final PluginMessageService messages;

	public MilestoneCommandModule(
		MilestoneMenuService menus,
		MilestoneConfigurationManager configuration,
		MilestoneService milestones,
		PluginMessageService messages
	) {
		this.menus = Objects.requireNonNull(menus, "menus");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.milestones = Objects.requireNonNull(milestones, "milestones");
		this.messages = Objects.requireNonNull(messages, "messages");
	}

	@Override
	public void register(CommandRegistration registration) {
		registration.register(CommandTree.root("milestones")
			.aliases("milestone")
			.description("View milestone progress")
			.requiresAnyPermission(USE_PERMISSION, ADMIN_PERMISSION)
			.executesPlayer((player, context) -> {
				menus.openOverview(player);
				return CommandTree.ok();
			})
			.literal("admin", node -> node
				.requiresPermission(ADMIN_PERMISSION)
				.literalExec("reload", this::reload)
				.literal("progress", progress -> progress
					.argument("player", Args.word(), player -> player.executes(this::progress)))
				.literal("reset", reset -> reset
					.argument("player", Args.word(), player -> player.executes(this::reset))))
			.spec());
	}

	private int reload(MichelleCommandContext context) {
		configuration.reload();
		messages.send(context.sender(), MessageDefaults.MILESTONES_RELOADED);
		return CommandTree.ok();
	}

	private int progress(MichelleCommandContext context) throws CommandSyntaxException {
		CommandSender sender = context.sender();
		OfflinePlayer target = resolvePlayer(context.arg("player", String.class));
		if (target == null) {
			messages.send(sender, MessageDefaults.COMMAND_UNKNOWN_PLAYER);
			return CommandTree.ok();
		}
		String playerName = displayName(target);
		var records = milestones.progress(target.getUniqueId());
		if (records.isEmpty()) {
			messages.send(sender, MessageDefaults.MILESTONES_PROGRESS_EMPTY, args -> args.plain("player", playerName));
			return CommandTree.ok();
		}
		messages.send(sender, MessageDefaults.MILESTONES_PROGRESS_HEADER, args -> args.plain("player", playerName));
		for (MilestoneProgress record : records) {
			messages.send(sender, MessageDefaults.MILESTONES_PROGRESS_ROW, args -> args
				.plain("track", trackName(record))
				.plain("amount", record.amount()));
		}
		return CommandTree.ok();
	}

	private int reset(MichelleCommandContext context) throws CommandSyntaxException {
		CommandSender sender = context.sender();
		OfflinePlayer target = resolvePlayer(context.arg("player", String.class));
		if (target == null) {
			messages.send(sender, MessageDefaults.COMMAND_UNKNOWN_PLAYER);
			return CommandTree.ok();
		}
		milestones.reset(target.getUniqueId());
		messages.send(sender, MessageDefaults.MILESTONES_RESET, args -> args.plain("player", displayName(target)));
		return CommandTree.ok();
	}

	private String trackName(MilestoneProgress progress) {
		Optional<MilestoneTrack> track = configuration.current().catalog().tracks(progress.key()).stream().findFirst();
		return track.map(MilestoneTrack::name).orElseGet(() -> {
			String metric = progress.key().metric().name().toLowerCase(java.util.Locale.ROOT);
			return progress.key().subject().isBlank() ? metric : metric + ":" + progress.key().subject();
		});
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
