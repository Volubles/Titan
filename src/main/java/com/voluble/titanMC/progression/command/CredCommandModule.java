package com.voluble.titanMC.progression.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.voluble.titanMC.display.notice.MessageDefaults;
import com.voluble.titanMC.display.notice.PluginMessageService;
import com.voluble.titanMC.progression.model.CredAmount;
import com.voluble.titanMC.progression.model.CredSource;
import com.voluble.titanMC.progression.model.PlayerProgression;
import com.voluble.titanMC.progression.bukkit.ProgressionBarService;
import com.voluble.titanMC.progression.service.ProgressionEngine;
import com.voluble.titanMC.progression.service.ProgressionUpdate;
import io.voluble.michellelib.commands.CommandModule;
import io.voluble.michellelib.commands.CommandRegistration;
import io.voluble.michellelib.commands.arguments.Args;
import io.voluble.michellelib.commands.context.MichelleCommandContext;
import io.voluble.michellelib.commands.tree.CommandTree;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class CredCommandModule implements CommandModule {
	private static final String USE_PERMISSION = "titanmc.cred.use";
	private static final String ADMIN_PERMISSION = "titanmc.cred.admin";
	private static final CredSource ADMIN_SOURCE = CredSource.of("admin");

	private final ProgressionEngine engine;
	private final ProgressionBarService bars;
	private final PluginMessageService messages;

	public CredCommandModule(ProgressionEngine engine, ProgressionBarService bars, PluginMessageService messages) {
		this.engine = Objects.requireNonNull(engine, "engine");
		this.bars = Objects.requireNonNull(bars, "bars");
		this.messages = Objects.requireNonNull(messages, "messages");
	}

	@Override
	public void register(CommandRegistration registration) {
		registration.register(CommandTree.root("cred")
			.description("Show or manage cred and player levels")
			.requiresAnyPermission(USE_PERMISSION, ADMIN_PERMISSION)
			.executesPlayer((player, ctx) -> showOwn(player))
			.literal("info", node -> node
				.requiresPermission(ADMIN_PERMISSION)
				.argument("player", Args.word(), name -> name.executes(this::info)))
			.literal("give", node -> node
				.requiresPermission(ADMIN_PERMISSION)
				.argument("player", Args.word(), name -> name
					.argument("amount", Args.longArg(), amount -> amount
						.executes(context -> apply(context, true, null))
						.argument("source", Args.word(), source -> source
							.executes(context -> apply(context, true, context.arg("source", String.class)))))))
			.literal("take", node -> node
				.requiresPermission(ADMIN_PERMISSION)
				.argument("player", Args.word(), name -> name
					.argument("amount", Args.longArg(), amount -> amount.executes(context -> apply(context, false, null)))))
			.spec());
	}

	private int showOwn(Player player) {
		PlayerProgression progression = engine.current(player.getUniqueId());
		for (Component line : formatSelf(progression)) {
			player.sendMessage(line);
		}
		bars.show(player);
		return CommandTree.ok();
	}

	private int info(MichelleCommandContext context) throws CommandSyntaxException {
		CommandSender sender = context.sender();
		OfflinePlayer target = resolvePlayer(context.arg("player", String.class));
		if (target == null) {
			messages.send(sender, MessageDefaults.COMMAND_UNKNOWN_PLAYER);
			return CommandTree.ok();
		}
		PlayerProgression progression = engine.current(target.getUniqueId());
		messages.send(sender, MessageDefaults.CRED_PLAYER_INFO, args -> args
			.plain("player", displayName(target))
			.plain("summary", formatLine(progression)));
		return CommandTree.ok();
	}

	private int apply(MichelleCommandContext context, boolean give, String sourceArg) throws CommandSyntaxException {
		CommandSender sender = context.sender();
		OfflinePlayer target = resolvePlayer(context.arg("player", String.class));
		if (target == null) {
			messages.send(sender, MessageDefaults.COMMAND_UNKNOWN_PLAYER);
			return CommandTree.ok();
		}
		long amount = context.arg("amount", Long.class);
		if (amount <= 0) {
			messages.send(sender, MessageDefaults.CRED_AMOUNT_POSITIVE);
			return CommandTree.ok();
		}
		CredSource source;
		try {
			source = sourceArg == null ? ADMIN_SOURCE : CredSource.of(sourceArg);
		} catch (IllegalArgumentException exception) {
			messages.send(sender, MessageDefaults.CRED_INVALID_SOURCE, args -> args.plain("reason", exception.getMessage()));
			return CommandTree.ok();
		}
		CredAmount credAmount = CredAmount.of(amount);
		ProgressionUpdate update = give
			? engine.give(target.getUniqueId(), credAmount, source)
			: engine.take(target.getUniqueId(), credAmount, source);

		String verb = give ? "Gave" : "Took";
		String name = displayName(target);
		if (update.applied() == 0L) {
			messages.send(sender, MessageDefaults.CRED_UNCHANGED, args -> args.plain("player", name));
			return CommandTree.ok();
		}
		String credChange = (give ? "+" : "") + update.applied();
		String levelChange = update.changedLevel()
			? " (level " + update.previous().level() + " -> " + update.current().level() + ")"
			: "";
		messages.send(sender, MessageDefaults.CRED_CHANGED, args -> args
			.plain("verb", verb)
			.plain("player", name)
			.plain("amount", credChange)
			.plain("level_change", levelChange)
			.plain("source", source.value()));
		return CommandTree.ok();
	}

	private String formatLine(PlayerProgression progression) {
		return progression.totalCred() + " cred, level " + progression.level();
	}

	private java.util.List<Component> formatSelf(PlayerProgression progression) {
		NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.US);
		int level = progression.level();
		long total = progression.totalCred();
		Component header = Component.text("Cred Level " + level, NamedTextColor.GOLD)
			.decoration(TextDecoration.BOLD, true);
		Component totalLine = label("Total: ").append(Component.text(numbers.format(total) + " cred", NamedTextColor.WHITE));
		if (level >= engine.maxLevel()) {
			return java.util.List.of(
				header,
				totalLine,
				Component.text("You have reached the current max level.", NamedTextColor.GREEN)
			);
		}
		long currentLevelStart = engine.curve().credForLevel(level);
		long nextLevelStart = engine.curve().credForLevel(level + 1);
		long span = Math.max(1L, nextLevelStart - currentLevelStart);
		long inLevel = Math.max(0L, total - currentLevelStart);
		long remaining = Math.max(0L, nextLevelStart - total);
		long percent = Math.round((100.0D * inLevel) / span);
		return java.util.List.of(
			header,
			totalLine,
			label("Progress: ").append(Component.text(percent + "% to level " + (level + 1), NamedTextColor.WHITE)),
			label("Remaining: ").append(Component.text(numbers.format(remaining) + " cred", NamedTextColor.WHITE))
		);
	}

	private static Component label(String text) {
		return Component.text(text, NamedTextColor.GRAY);
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
