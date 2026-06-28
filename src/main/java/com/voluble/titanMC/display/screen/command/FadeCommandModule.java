package com.voluble.titanMC.display.screen.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.voluble.titanMC.display.notice.MessageDefaults;
import com.voluble.titanMC.display.notice.PluginMessageService;
import com.voluble.titanMC.display.screen.ScreenEffectConfigurationManager;
import com.voluble.titanMC.display.screen.ScreenEffectId;
import com.voluble.titanMC.display.screen.ScreenEffectRequest;
import com.voluble.titanMC.display.screen.ScreenEffectService;
import com.voluble.titanMC.display.screen.ScreenEffectTiming;
import io.voluble.michellelib.commands.CommandModule;
import io.voluble.michellelib.commands.CommandRegistration;
import io.voluble.michellelib.commands.arguments.Args;
import io.voluble.michellelib.commands.context.MichelleCommandContext;
import io.voluble.michellelib.commands.suggest.Suggest;
import io.voluble.michellelib.commands.tree.CommandTree;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class FadeCommandModule implements CommandModule {
	private static final String PERMISSION = "titanmc.fade.admin";
	private static final ScreenEffectId DEFAULT_SCREEN = ScreenEffectId.of("fullscreen_black");

	private final ScreenEffectConfigurationManager configuration;
	private final ScreenEffectService screens;
	private final PluginMessageService messages;

	public FadeCommandModule(
		ScreenEffectConfigurationManager configuration,
		ScreenEffectService screens,
		PluginMessageService messages
	) {
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.screens = Objects.requireNonNull(screens, "screens");
		this.messages = Objects.requireNonNull(messages, "messages");
	}

	@Override
	public void register(CommandRegistration registration) {
		var screenIds = Suggest.fromContext(source -> configuration.current().screens().keySet().stream()
			.map(ScreenEffectId::value)
			.sorted()
			.toList());
		var targets = Suggest.fromContext(source -> {
			java.util.ArrayList<String> values = Bukkit.getOnlinePlayers().stream()
				.map(Player::getName)
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
			values.add("all");
			return values;
		});
		registration.register(CommandTree.root("fade")
			.description("Play a screen fade effect")
			.requiresPermission(PERMISSION)
			.executesPlayer((player, context) -> show(context, List.of(player), DEFAULT_SCREEN, null))
			.literalExec("reload", this::reload)
			.literal("stop", node -> node.executesPlayer((player, context) -> stop(player)))
			.argument("screen", Args.word(), screen -> screen
				.suggests(screenIds)
				.executesPlayer((player, context) -> show(context, List.of(player), screen(context), null))
				.argument("target", Args.word(), target -> target
					.suggests(targets)
					.executes(context -> show(context, resolveTargets(context), screen(context), null))
					.argument("fadeIn", Args.longArg(), fadeIn -> fadeIn
						.argument("hold", Args.longArg(), hold -> hold
							.argument("fadeOut", Args.longArg(), fadeOut -> fadeOut
								.executes(context -> show(context, resolveTargets(context), screen(context), timing(context))))))))
			.spec());
	}

	private int reload(MichelleCommandContext context) {
		screens.reload();
		messages.send(context.sender(), MessageDefaults.FADE_RELOADED);
		return CommandTree.ok();
	}

	private int stop(Player player) {
		screens.stop(player.getUniqueId());
		messages.send(player, MessageDefaults.FADE_STOPPED);
		return CommandTree.ok();
	}

	private int show(MichelleCommandContext context, Collection<Player> targets, ScreenEffectId screen, ScreenEffectTiming timing) {
		if (targets.isEmpty()) return CommandTree.ok();
		ScreenEffectRequest request = ScreenEffectRequest.of(screen);
		if (timing != null) request = request.withTiming(timing);
		boolean shown = false;
		for (Player target : targets) {
			shown |= screens.show(target, request);
		}
		if (!shown && !targets.isEmpty()) {
			messages.send(context.sender(), MessageDefaults.FADE_UNKNOWN, args -> args.plain("screen", screen.value()));
		}
		return CommandTree.ok();
	}

	private ScreenEffectId screen(MichelleCommandContext context) throws CommandSyntaxException {
		try {
			return ScreenEffectId.of(context.arg("screen", String.class));
		} catch (IllegalArgumentException exception) {
			throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create(exception.getMessage());
		}
	}

	private ScreenEffectTiming timing(MichelleCommandContext context) {
		return new ScreenEffectTiming(
			context.arg("fadeIn", Long.class),
			context.arg("hold", Long.class),
			context.arg("fadeOut", Long.class)
		);
	}

	private Collection<Player> resolveTargets(MichelleCommandContext context) {
		String target = context.arg("target", String.class);
		if (target.equalsIgnoreCase("all")) return List.copyOf(Bukkit.getOnlinePlayers());
		Player player = Bukkit.getPlayerExact(target);
		if (player == null) {
			messages.send(context.sender(), MessageDefaults.COMMAND_UNKNOWN_PLAYER);
			return List.of();
		}
		return List.of(player);
	}
}
