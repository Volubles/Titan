package com.voluble.titanMC.regions.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.voluble.titanMC.TitanMC;
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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
				.literal("flag", flag -> flag
					.argument("name", Args.word(), name -> name
						.suggests(names)
						.argument("flag", Args.word(), action -> action
							.suggestStrings(actions)
							.argument("value", Args.word(), value -> value
								.suggestStrings(List.of("allow", "deny", "unset"))
								.executes(this::handleFlag)))))
				.spec()
		);
	}

	private int handleRoot(MichelleCommandContext context) throws CommandSyntaxException {
		context.playerExecutor().sendMessage(
			"Usage: /region <create|redefine|delete|list|info|priority|test|flag|message>"
		);
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
			player.sendMessage(exception.getMessage());
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
			player.sendMessage(exception.getMessage());
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
			player.sendMessage(exception.getMessage());
		}
		return CommandTree.ok();
	}

	private int handleList(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		var definitions = regions.list(world(player));
		if (definitions.isEmpty()) {
			player.sendMessage("No custom regions exist in this world.");
		} else {
			player.sendMessage("Regions: " + definitions.stream()
				.map(region -> region.key().name())
				.collect(Collectors.joining(", ")));
		}
		return CommandTree.ok();
	}

	private int handleInfo(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		RegionDefinition region;
		try {
			region = regions.find(world(player), context.arg("name", String.class));
		} catch (IllegalArgumentException exception) {
			player.sendMessage(exception.getMessage());
			return CommandTree.ok();
		}
		if (region == null) {
			player.sendMessage("Unknown region in this world.");
			return CommandTree.ok();
		}
		player.sendMessage(region.key() + " | " + geometryName(region.geometry())
			+ " | priority " + region.priority() + " | revision " + region.revision());
		if (region.flags().explicitDecisions().isEmpty()) {
			player.sendMessage("Flags: none (namespace/world defaults apply)");
		} else {
			player.sendMessage("Flags: " + region.flags().explicitDecisions().entrySet().stream()
				.map(entry -> entry.getKey().name().toLowerCase(Locale.ROOT) + "="
					+ entry.getValue().name().toLowerCase(Locale.ROOT))
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
			RegionMutationResult result = regions.setFlag(name, world(player), action, decision);
			sendResult(
				player,
				result,
				"Set " + action.name().toLowerCase(Locale.ROOT) + " to "
					+ context.arg("value", String.class).toLowerCase(Locale.ROOT) + " for '" + name + "'."
			);
		} catch (IllegalArgumentException exception) {
			player.sendMessage(exception.getMessage());
		}
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
			player.sendMessage(exception.getMessage());
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
				player.sendMessage("Entry is transition-based and cannot be tested at a single position.");
				return CommandTree.ok();
			}
		} catch (IllegalArgumentException exception) {
			player.sendMessage("Unknown region flag.");
			return CommandTree.ok();
		}
		BlockPosition position = BukkitProtectionMapper.position(player.getLocation());
		if (plugin.getProtectionService() == null) {
			player.sendMessage("Titan region protection is disabled.");
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
		player.sendMessage(
			"Test " + action.name().toLowerCase(Locale.ROOT)
				+ " at " + position.x() + ", " + position.y() + ", " + position.z()
				+ ": " + resolution.decision().name()
				+ " (" + reasonName(resolution.reason()) + ")"
		);
		if (test.matchingRegions().isEmpty()) {
			player.sendMessage("Matching regions: none");
		} else {
			player.sendMessage("Matching regions: " + test.matchingRegions().stream()
				.map(region -> region.key() + "@" + region.priority())
				.collect(Collectors.joining(", ")));
		}
		if (resolution.evaluations().isEmpty()) {
			if (resolution.reason() == ProtectionResolution.Reason.BYPASS) {
				player.sendMessage("Trace: protection bypassed by your permission.");
			} else {
				player.sendMessage("Trace: no region rule decided; world default applied.");
			}
		} else {
			for (var evaluation : resolution.evaluations()) {
				player.sendMessage(
					"Trace: " + evaluation.regionKey() + "@" + evaluation.priority()
						+ " -> " + evaluation.decision().name()
						+ " via " + evaluation.policyId()
						+ evaluation.error().map(error -> " (" + error + ")").orElse("")
				);
			}
		}
		resolution.decidingPriority().ifPresent(priority ->
			player.sendMessage("Winning priority: " + priority)
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
			player.sendMessage(exception.getMessage());
		}
		return CommandTree.ok();
	}

	private static String reasonName(ProtectionResolution.Reason reason) {
		return reason.name().toLowerCase(Locale.ROOT).replace('_', ' ');
	}

	private static SelectedRegion selection(Player player) {
		try {
			return WorldEditRegionSelection.read(player);
		} catch (SelectionException exception) {
			player.sendMessage(exception.getMessage());
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

	private static String geometryName(RegionGeometry geometry) {
		if (geometry instanceof CuboidGeometry) return "cuboid";
		if (geometry instanceof PolygonPrismGeometry) return "polygon";
		if (geometry instanceof ConvexPolyhedronGeometry) return "convex polyhedron";
		return geometry.getClass().getSimpleName();
	}

	private static void sendResult(Player player, RegionMutationResult result, String success) {
		if (result instanceof RegionMutationResult.Success) player.sendMessage(success);
		else if (result instanceof RegionMutationResult.Failure failure) {
			player.sendMessage("Region operation failed: " + failure.message());
		}
	}
}
