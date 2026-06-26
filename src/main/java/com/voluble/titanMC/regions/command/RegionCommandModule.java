package com.voluble.titanMC.regions.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.voluble.titanMC.TitanMC;
import com.voluble.titanMC.display.notice.MessageDefaults;
import com.voluble.titanMC.regions.admin.RegionAdminService;
import com.voluble.titanMC.regions.admin.RegionProtectionTestService;
import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.model.ConvexPolyhedronGeometry;
import com.voluble.titanMC.regions.model.CuboidGeometry;
import com.voluble.titanMC.regions.model.PolygonPrismGeometry;
import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.RegionGeometry;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.model.RegionTextFlag;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionResolution;
import com.voluble.titanMC.regions.protection.model.RegionSubject;
import com.voluble.titanMC.regions.protection.bukkit.BukkitProtectionMapper;
import com.voluble.titanMC.regions.selection.SelectedRegion;
import com.voluble.titanMC.regions.selection.SelectionException;
import com.voluble.titanMC.regions.selection.WorldEditRegionSelection;
import com.voluble.titanMC.regions.service.RegionMutationResult;
import io.voluble.michellelib.commands.CommandModule;
import io.voluble.michellelib.commands.CommandRegistration;
import io.voluble.michellelib.commands.arguments.Args;
import io.voluble.michellelib.commands.context.MichelleCommandContext;
import io.voluble.michellelib.commands.suggest.Suggest;
import io.voluble.michellelib.commands.tree.CommandTree;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public final class RegionCommandModule implements CommandModule {

	private final TitanMC plugin;
	private final RegionAdminService regions;

	public RegionCommandModule(TitanMC plugin) {
		this.plugin = plugin;
		this.regions = new RegionAdminService(plugin.getRegionEngine());
	}

	@Override
	public void register(CommandRegistration registration) {
		var names = Suggest.fromContext(source -> regions.names());
		var actions = Arrays.stream(ProtectionAction.values())
			.map(action -> action.name().toLowerCase(Locale.ROOT))
			.toList();
		var testActions = Arrays.stream(ProtectionAction.values())
			.filter(action -> action != ProtectionAction.ENTRY)
			.map(action -> action.name().toLowerCase(Locale.ROOT))
			.toList();
		var textFlags = Arrays.stream(RegionTextFlag.values())
			.map(flag -> flag.name().toLowerCase(Locale.ROOT))
			.toList();
		var players = Suggest.fromContext(source -> plugin.getServer().getOnlinePlayers().stream()
			.map(Player::getName)
			.sorted(String.CASE_INSENSITIVE_ORDER)
			.toList());
		var subjects = Suggest.fromContext(source -> {
			List<String> available = new ArrayList<>(List.of(
				"everyone", "owners", "members", "nonowners", "nonmembers"
			));
			available.addAll(plugin.getRegionGroups().groups().stream()
				.map(group -> "group:" + group)
				.toList());
			return available;
		});
		var currentPriority = Suggest.contextual((context, args) -> {
			if (!(context.getSource().getExecutor() instanceof Player player)) return List.of();
			String name = args.opt("name", String.class).orElse(null);
			if (name == null) return List.of();
			try {
				RegionDefinition region = regions.find(world(player), name);
				return region == null ? List.of() : List.of(Integer.toString(region.priority()));
			} catch (IllegalArgumentException exception) {
				return List.of();
			}
		});

		registration.register(
			CommandTree.root("region")
				.description("Manage protection regions")
				.aliases("rg")
				.requiresPermission("titanmc.region.admin")
				.requiresPlayerExecutor()
				.executes(this::handleRoot)
				.literalExec("list", this::handleList)
				.literal("create", create -> create
					.argument("name", Args.word(), name -> name
						.executes(this::handleCreate)
						.argument("priority", Args.integer(), priority -> priority
							.executes(this::handleCreate))))
				.literal("redefine", redefine -> redefine
					.argument("name", Args.word(), name -> name
						.suggests(names)
						.executes(this::handleRedefine)))
				.literal("delete", delete -> delete
					.argument("name", Args.word(), name -> name
						.suggests(names)
						.executes(this::handleDelete)))
				.literal("info", info -> info
					.argument("name", Args.word(), name -> name
						.suggests(names)
						.executes(this::handleInfo)))
				.literal("priority", priority -> priority
					.argument("name", Args.word(), name -> name
						.suggests(names)
						.argument("priority", Args.integer(), value -> value
							.suggests(currentPriority)
							.executes(this::handlePriority))))
				.literal("test", test -> test
					.argument("flag", Args.word(), flag -> flag
						.suggestStrings(testActions)
						.executes(this::handleTest)))
				.literal("message", message -> message
					.argument("name", Args.word(), name -> name
						.suggests(names)
						.argument("type", Args.word(), type -> type
							.suggestStrings(textFlags)
							.argument("value", Args.greedyString(), value -> value
								.suggestStrings(List.of("unset"))
								.executes(this::handleMessage)))))
				.literal("owner", owner -> owner
					.literal("add", add -> add
						.argument("name", Args.word(), name -> name
							.suggests(names)
							.argument("player", Args.word(), player -> player
								.suggests(players)
								.executes(context -> handleAccess(context, true, true)))))
					.literal("remove", remove -> remove
						.argument("name", Args.word(), name -> name
							.suggests(names)
							.argument("player", Args.word(), player -> player
								.suggests(players)
								.executes(context -> handleAccess(context, true, false)))))
					.literal("list", list -> list
						.argument("name", Args.word(), name -> name
							.suggests(names)
							.executes(context -> handleAccessList(context, true)))))
				.literal("member", member -> member
					.literal("add", add -> add
						.argument("name", Args.word(), name -> name
							.suggests(names)
							.argument("player", Args.word(), player -> player
								.suggests(players)
								.executes(context -> handleAccess(context, false, true)))))
					.literal("remove", remove -> remove
						.argument("name", Args.word(), name -> name
							.suggests(names)
							.argument("player", Args.word(), player -> player
								.suggests(players)
								.executes(context -> handleAccess(context, false, false)))))
					.literal("list", list -> list
						.argument("name", Args.word(), name -> name
							.suggests(names)
							.executes(context -> handleAccessList(context, false)))))
				.literal("flag", flag -> flag
					.argument("name", Args.word(), name -> name
						.suggests(names)
						.argument("flag", Args.word(), action -> action
							.suggestStrings(actions)
							.argument("value", Args.word(), value -> value
								.suggestStrings(List.of("allow", "deny", "unset"))
								.executes(this::handleFlag)
								.argument("subject", Args.word(), subject -> subject
									.suggests(subjects)
									.executes(this::handleFlag))))))
				.spec()
		);
	}

	private int handleRoot(MichelleCommandContext context) throws CommandSyntaxException {
		plugin.getMessages().send(context.playerExecutor(), MessageDefaults.REGIONS_USAGE);
		return CommandTree.ok();
	}

	private int handleCreate(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		String name = context.arg("name", String.class);
		int priority = optionalInteger(context, "priority", RegionAdminService.DEFAULT_PRIORITY);
		SelectedRegion selection = selection(player);
		if (selection == null) return CommandTree.ok();
		RegionMutationResult result;
		try {
			result = regions.create(
				name, new WorldId(selection.worldId()), priority, selection.geometry()
			);
		} catch (IllegalArgumentException exception) {
			error(player, exception.getMessage());
			return CommandTree.ok();
		}
		sendResult(player, result, "Created region '" + name + "' as " + geometryName(selection.geometry()) + ".");
		return CommandTree.ok();
	}

	private int handleRedefine(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		String name = context.arg("name", String.class);
		SelectedRegion selection = selection(player);
		if (selection == null) return CommandTree.ok();
		RegionMutationResult result;
		try {
			result = regions.redefine(name, new WorldId(selection.worldId()), selection.geometry());
		} catch (IllegalArgumentException exception) {
			error(player, exception.getMessage());
			return CommandTree.ok();
		}
		sendResult(player, result, "Redefined region '" + name + "' as " + geometryName(selection.geometry()) + ".");
		return CommandTree.ok();
	}

	private int handleDelete(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		String name = context.arg("name", String.class);
		try {
			sendResult(
				player,
				regions.delete(name, world(player)),
				"Deleted region '" + name + "'."
			);
		} catch (IllegalArgumentException exception) {
			error(player, exception.getMessage());
		}
		return CommandTree.ok();
	}

	private int handleList(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		var definitions = regions.list(world(player));
		if (definitions.isEmpty()) {
			plugin.getMessages().send(player, MessageDefaults.REGIONS_EMPTY);
		} else {
			plugin.getMessages().send(player, MessageDefaults.REGIONS_LIST, args -> args.plain(
				"regions",
				definitions.stream().map(region -> region.key().name()).collect(Collectors.joining(", "))
			));
		}
		return CommandTree.ok();
	}

	private int handleInfo(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		RegionDefinition region;
		try {
			region = regions.find(world(player), context.arg("name", String.class));
		} catch (IllegalArgumentException exception) {
			error(player, exception.getMessage());
			return CommandTree.ok();
		}
		if (region == null) {
			plugin.getMessages().send(player, MessageDefaults.REGIONS_UNKNOWN);
			return CommandTree.ok();
		}
		player.sendMessage(region.key() + " | " + geometryName(region.geometry())
			+ " | priority " + region.priority() + " | revision " + region.revision());
		player.sendMessage("Owners: " + identities(region.access().owners()));
		player.sendMessage("Members: " + identities(region.access().members()));
		if (region.flags().explicitRules().isEmpty()) {
			player.sendMessage("Flags: none (namespace/world defaults apply)");
		} else {
			player.sendMessage("Flags: " + region.flags().explicitRules().entrySet().stream()
				.flatMap(action -> action.getValue().entrySet().stream()
					.map(rule -> action.getKey().name().toLowerCase(Locale.ROOT)
						+ "[" + rule.getKey().externalName() + "]="
						+ rule.getValue().name().toLowerCase(Locale.ROOT)))
				.collect(Collectors.joining(", ")));
		}
		if (region.text().explicitValues().isEmpty()) {
			player.sendMessage("Messages: none");
		} else {
			player.sendMessage("Messages:");
			region.text().explicitValues().forEach((flag, value) ->
				player.sendMessage("- " + flag.name().toLowerCase(Locale.ROOT) + " = " + value)
			);
		}
		return CommandTree.ok();
	}

	private int handleFlag(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		String name = context.arg("name", String.class);
		ProtectionAction action;
		ProtectionDecision decision;
		try {
			action = ProtectionAction.valueOf(context.arg("flag", String.class).toUpperCase(Locale.ROOT));
			decision = switch (context.arg("value", String.class).toLowerCase(Locale.ROOT)) {
				case "allow" -> ProtectionDecision.ALLOW;
				case "deny" -> ProtectionDecision.DENY;
				case "unset" -> ProtectionDecision.ABSTAIN;
				default -> throw new IllegalArgumentException("Flag value must be allow, deny, or unset.");
			};
			RegionSubject subject = RegionSubject.parse(
				optionalString(context, "subject", "everyone")
			);
			RegionMutationResult result = regions.setFlag(
				name, world(player), action, subject, decision
			);
			sendResult(
				player,
				result,
				"Set " + action.name().toLowerCase(Locale.ROOT) + " to "
					+ context.arg("value", String.class).toLowerCase(Locale.ROOT)
					+ " for " + subject.externalName() + " in '" + name + "'."
			);
		} catch (IllegalArgumentException exception) {
			error(player, exception.getMessage());
		}
		return CommandTree.ok();
	}

	private int handleAccess(
		MichelleCommandContext context,
		boolean owner,
		boolean add
	) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		String name = context.arg("name", String.class);
		OfflinePlayer target = resolvePlayer(context.arg("player", String.class));
		if (target == null) {
			plugin.getMessages().send(player, MessageDefaults.REGIONS_UNKNOWN_PLAYER);
			return CommandTree.ok();
		}
		RegionMutationResult result;
		try {
			result = owner
				? add
					? regions.addOwner(name, world(player), target.getUniqueId())
					: regions.removeOwner(name, world(player), target.getUniqueId())
				: add
					? regions.addMember(name, world(player), target.getUniqueId())
					: regions.removeMember(name, world(player), target.getUniqueId());
		} catch (IllegalArgumentException exception) {
			error(player, exception.getMessage());
			return CommandTree.ok();
		}
		sendResult(
			player,
			result,
			(add ? "Added " : "Removed ") + displayName(target)
				+ (add ? " as " : " from ") + (owner ? "owner" : "member")
				+ " in '" + name + "'."
		);
		return CommandTree.ok();
	}

	private int handleAccessList(
		MichelleCommandContext context,
		boolean owners
	) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		RegionDefinition region;
		try {
			region = regions.find(world(player), context.arg("name", String.class));
		} catch (IllegalArgumentException exception) {
			error(player, exception.getMessage());
			return CommandTree.ok();
		}
		if (region == null) {
			plugin.getMessages().send(player, MessageDefaults.REGIONS_UNKNOWN);
			return CommandTree.ok();
		}
		plugin.getMessages().send(player, MessageDefaults.REGIONS_ACCESS_LIST, args -> args
			.plain("label", owners ? "Owners" : "Members")
			.plain("players", identities(owners ? region.access().owners() : region.access().members())));
		return CommandTree.ok();
	}

	private int handlePriority(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		String name = context.arg("name", String.class);
		int priority = context.arg("priority", Integer.class);
		try {
			sendResult(
				player,
				regions.setPriority(name, world(player), priority),
				"Set priority to " + priority + " for '" + name + "'."
			);
		} catch (IllegalArgumentException exception) {
			error(player, exception.getMessage());
		}
		return CommandTree.ok();
	}

	private int handleTest(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		ProtectionAction action;
		try {
			action = ProtectionAction.valueOf(
				context.arg("flag", String.class).toUpperCase(Locale.ROOT)
			);
			if (action == ProtectionAction.ENTRY) {
				plugin.getMessages().send(player, MessageDefaults.REGIONS_ENTRY_TEST_UNAVAILABLE);
				return CommandTree.ok();
			}
		} catch (IllegalArgumentException exception) {
			plugin.getMessages().send(player, MessageDefaults.REGIONS_UNKNOWN_FLAG);
			return CommandTree.ok();
		}
		BlockPosition position = BukkitProtectionMapper.position(player.getLocation());
		if (plugin.getProtectionService() == null) {
			plugin.getMessages().send(player, MessageDefaults.REGIONS_PROTECTION_DISABLED);
			return CommandTree.ok();
		}
		RegionProtectionTestService protectionTests = new RegionProtectionTestService(
			plugin.getRegionEngine(),
			plugin.getProtectionService()
		);
		RegionProtectionTestService.Result test = protectionTests.test(
			BukkitProtectionMapper.actor(player),
			action,
			position
		);
		ProtectionResolution resolution = test.resolution();
		plugin.getMessages().send(player, MessageDefaults.REGIONS_TEST_RESULT, args -> args
			.plain("flag", action.name().toLowerCase(Locale.ROOT))
			.plain("x", position.x())
			.plain("y", position.y())
			.plain("z", position.z())
			.plain("decision", resolution.decision().name())
			.plain("reason", reasonName(resolution.reason())));
		if (test.matchingRegions().isEmpty()) {
			plugin.getMessages().send(player, MessageDefaults.REGIONS_MATCHING_NONE);
		} else {
			plugin.getMessages().send(player, MessageDefaults.REGIONS_MATCHING, args -> args.plain(
				"regions",
				test.matchingRegions().stream()
					.map(region -> region.key() + "@" + region.priority())
					.collect(Collectors.joining(", "))
			));
		}
		if (resolution.evaluations().isEmpty()) {
			if (resolution.reason() == ProtectionResolution.Reason.BYPASS) {
				plugin.getMessages().send(player, MessageDefaults.REGIONS_TRACE_BYPASS);
			} else {
				plugin.getMessages().send(player, MessageDefaults.REGIONS_TRACE_DEFAULT);
			}
		} else {
			for (var evaluation : resolution.evaluations()) {
				plugin.getMessages().send(player, MessageDefaults.REGIONS_TRACE_LINE, args -> args
					.plain("region", evaluation.regionKey())
					.plain("priority", evaluation.priority())
					.plain("decision", evaluation.decision().name())
					.plain("policy", evaluation.policyId())
					.plain("error", evaluation.error().map(error -> " (" + error + ")").orElse("")));
			}
		}
		resolution.decidingPriority().ifPresent(priority ->
			plugin.getMessages().send(player, MessageDefaults.REGIONS_WINNING_PRIORITY, args -> args.plain("priority", priority))
		);
		return CommandTree.ok();
	}

	private int handleMessage(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		String name = context.arg("name", String.class);
		try {
			RegionTextFlag flag = RegionTextFlag.valueOf(
				context.arg("type", String.class).toUpperCase(Locale.ROOT)
			);
			String input = context.arg("value", String.class);
			String value = input.equalsIgnoreCase("unset") ? null : input;
			sendResult(
				player,
				regions.setText(name, world(player), flag, value),
				value == null
					? "Unset " + flag.name().toLowerCase(Locale.ROOT) + " for '" + name + "'."
					: "Set " + flag.name().toLowerCase(Locale.ROOT) + " for '" + name + "'."
			);
		} catch (IllegalArgumentException exception) {
			error(player, exception.getMessage());
		}
		return CommandTree.ok();
	}

	private static String reasonName(ProtectionResolution.Reason reason) {
		return reason.name().toLowerCase(Locale.ROOT).replace('_', ' ');
	}

	private SelectedRegion selection(Player player) {
		try {
			return WorldEditRegionSelection.read(player);
		} catch (SelectionException exception) {
			error(player, exception.getMessage());
			return null;
		}
	}

	private static WorldId world(Player player) {
		return new WorldId(player.getWorld().getUID());
	}

	private static int optionalInteger(MichelleCommandContext context, String name, int fallback) {
		try {
			return context.arg(name, Integer.class);
		} catch (IllegalArgumentException exception) {
			return fallback;
		}
	}

	private static String optionalString(
		MichelleCommandContext context,
		String name,
		String fallback
	) {
		try {
			return context.arg(name, String.class);
		} catch (IllegalArgumentException exception) {
			return fallback;
		}
	}

	private OfflinePlayer resolvePlayer(String input) {
		try {
			return plugin.getServer().getOfflinePlayer(UUID.fromString(input));
		} catch (IllegalArgumentException ignored) {
			return plugin.getServer().getOfflinePlayerIfCached(input);
		}
	}

	private String identities(java.util.Set<UUID> playerIds) {
		if (playerIds.isEmpty()) return "none";
		return playerIds.stream()
			.map(plugin.getServer()::getOfflinePlayer)
			.map(RegionCommandModule::displayName)
			.sorted(String.CASE_INSENSITIVE_ORDER)
			.collect(Collectors.joining(", "));
	}

	private static String displayName(OfflinePlayer player) {
		return player.getName() == null
			? player.getUniqueId().toString()
			: player.getName() + " (" + player.getUniqueId() + ")";
	}

	private static String geometryName(RegionGeometry geometry) {
		if (geometry instanceof CuboidGeometry) return "cuboid";
		if (geometry instanceof PolygonPrismGeometry) return "polygon";
		if (geometry instanceof ConvexPolyhedronGeometry) return "convex polyhedron";
		return geometry.getClass().getSimpleName();
	}

	private void sendResult(Player player, RegionMutationResult result, String success) {
		if (result instanceof RegionMutationResult.Success) {
			plugin.getMessages().send(player, MessageDefaults.REGIONS_SUCCESS, args -> args.plain("message", success));
		}
		else if (result instanceof RegionMutationResult.Failure failure) {
			plugin.getMessages().send(player, MessageDefaults.REGIONS_OPERATION_FAILED, args -> args.plain("reason", failure.message()));
		}
	}

	private void error(Player player, String reason) {
		plugin.getMessages().send(player, MessageDefaults.COMMAND_RUNTIME_ERROR, args -> args.plain("reason", reason));
	}
}
