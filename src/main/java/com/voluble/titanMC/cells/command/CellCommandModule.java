package com.voluble.titanMC.cells.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.voluble.titanMC.cells.CellManager;
import com.voluble.titanMC.cells.CellResetService;
import com.voluble.titanMC.cells.CellSignRenderer;
import com.voluble.titanMC.cells.CellSignService;
import com.voluble.titanMC.cells.baseline.CellBaselineCaptureService;
import com.voluble.titanMC.cells.model.CellDefinition;
import com.voluble.titanMC.display.notice.MessageDefaults;
import com.voluble.titanMC.display.notice.PluginMessageService;
import com.voluble.titanMC.regions.model.CuboidGeometry;
import com.voluble.titanMC.regions.selection.SelectionException;
import com.voluble.titanMC.regions.selection.WorldEditRegionSelection;
import com.voluble.titanMC.ranks.model.WardId;
import com.voluble.titanMC.ranks.service.RankCatalog;
import com.voluble.titanMC.util.RegionUtils;
import io.voluble.michellelib.commands.CommandModule;
import io.voluble.michellelib.commands.CommandRegistration;
import io.voluble.michellelib.commands.arguments.Args;
import io.voluble.michellelib.commands.context.MichelleCommandContext;
import io.voluble.michellelib.commands.suggest.Suggest;
import io.voluble.michellelib.commands.tree.CommandTree;
import io.voluble.michellelib.util.TimeParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.UUID;
import java.util.stream.Collectors;

public final class CellCommandModule implements CommandModule {
	private final CellManager cells;
	private final CellResetService resets;
	private final CellSignService signs;
	private final CellSignRenderer renderer;
	private final RankCatalog ranks;
	private final CellBaselineCaptureService baselineCapture;
	private final PluginMessageService messages;

	public CellCommandModule(
		CellManager cells,
		CellResetService resets,
		CellSignService signs,
		CellSignRenderer renderer,
		RankCatalog ranks,
		CellBaselineCaptureService baselineCapture,
		PluginMessageService messages
	) {
		this.cells = cells;
		this.resets = resets;
		this.signs = signs;
		this.renderer = renderer;
		this.ranks = ranks;
		this.baselineCapture = baselineCapture;
		this.messages = messages;
	}

	@Override
	public void register(CommandRegistration registration) {
		var names = Suggest.fromContext(source -> cells.cells().stream().map(CellDefinition::id).toList());
		var wards = Suggest.fromContext(source -> ranks.wards().stream().map(ward -> ward.id().value()).toList());
		registration.register(CommandTree.root("cell")
			.aliases("cells")
			.description("Manage rentable cells")
			.requiresPermission("titanmc.cell.admin")
			.requiresPlayerExecutor()
			.executes(this::root)
			.literalExec("list", this::list)
			.literal("create", node -> node
				.argument("name", Args.word(), name -> name
					.argument("ward", Args.word(), ward -> ward.suggests(wards)
						.argument("price", Args.longArg(), price -> price
							.argument("duration", Args.word(), duration -> duration
								.argument("max_duration", Args.word(), maximum -> maximum.executes(this::create)))))))
			.literal("delete", node -> node.argument("name", Args.word(), name -> name.suggests(names).executes(this::delete)))
			.literal("info", node -> node.argument("name", Args.word(), name -> name.suggests(names).executes(this::info)))
			.literal("displayname", node -> node.argument("name", Args.word(), name -> name.suggests(names)
				.argument("display_name", Args.greedyString(), value -> value.executes(this::displayName))))
			.literal("ward", node -> node.argument("name", Args.word(), name -> name.suggests(names)
				.argument("ward", Args.word(), ward -> ward.suggests(wards).executes(this::setWard))))
			.literal("reset", node -> node.argument("name", Args.word(), name -> name.suggests(names).executes(this::reset)))
			.literal("baseline", node -> node
				.literal("capture", capture -> capture.argument(
					"name", Args.word(), name -> name.suggests(names).executes(this::captureBaseline)
				)))
			.literal("member", node -> node
				.literal("add", add -> add.argument("name", Args.word(), name -> name.suggests(names)
					.argument("player", Args.word(), player -> player.executes(context -> member(context, true)))))
				.literal("remove", remove -> remove.argument("name", Args.word(), name -> name.suggests(names)
					.argument("player", Args.word(), player -> player.executes(context -> member(context, false)))))
				.literal("list", list -> list.argument("name", Args.word(), name -> name.suggests(names).executes(this::members))))
			.literal("sign", node -> node.argument("name", Args.word(), name -> name.suggests(names).executes(this::sign)))
			.spec());
	}

	private int root(MichelleCommandContext context) throws CommandSyntaxException {
		messages.send(context.playerExecutor(), MessageDefaults.CELLS_USAGE);
		return CommandTree.ok();
	}

	private int create(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		try {
			var selected = WorldEditRegionSelection.read(player);
			if (!(selected.geometry() instanceof CuboidGeometry cuboid)) {
				messages.send(player, MessageDefaults.CELLS_CUBOID_REQUIRED);
				return CommandTree.ok();
			}

			Duration duration = parseDuration(context, "duration");
			Duration maximum = parseDuration(context, "max_duration");
			var bounds = cuboid.bounds();
			CellDefinition cell = new CellDefinition(
				context.arg("name", String.class),
				parseWard(context),
				new RegionUtils.Cuboid(
					selected.worldId(),
					bounds.minX(), bounds.minY(), bounds.minZ(),
					bounds.maxXExclusive() - 1, bounds.maxYExclusive() - 1, bounds.maxZExclusive() - 1
				),
				context.arg("price", Long.class),
				duration.toSeconds(),
				maximum.toSeconds(),
				true
			);
			messages.send(player, MessageDefaults.CELLS_BASELINE_CAPTURING);
			baselineCapture.capture(cell).whenComplete((baseline, failure) -> {
				if (failure != null) {
					messages.send(player, MessageDefaults.CELLS_CREATE_FAILED, args -> args.plain("reason", rootMessage(failure)));
					return;
				}
				try {
					cells.create(cell, baseline);
					messages.send(player, MessageDefaults.CELLS_CREATED, args -> args
						.plain("cell", cell.id())
						.plain("ward", cell.wardId().value()));
				} catch (RuntimeException exception) {
					messages.send(player, MessageDefaults.CELLS_CREATE_FAILED, args -> args.plain("reason", rootMessage(exception)));
				}
			});
		} catch (SelectionException | RuntimeException exception) {
			messages.send(player, MessageDefaults.COMMAND_RUNTIME_ERROR, args -> args.plain("reason", exception.getMessage()));
		}
		return CommandTree.ok();
	}

	private static String rootMessage(Throwable failure) {
		Throwable cause = failure;
		while (cause.getCause() != null) cause = cause.getCause();
		return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
	}

	private WardId parseWard(MichelleCommandContext context) {
		WardId wardId = WardId.of(context.arg("ward", String.class));
		ranks.requireWard(wardId);
		return wardId;
	}

	private Duration parseDuration(MichelleCommandContext context, String argument) {
		return TimeParser.parse(context.arg(argument, String.class));
	}

	private int delete(MichelleCommandContext context) throws CommandSyntaxException {
		try {
			cells.delete(context.arg("name", String.class));
			messages.send(context.playerExecutor(), MessageDefaults.CELLS_DELETED);
		} catch (RuntimeException exception) {
			messages.send(context.playerExecutor(), MessageDefaults.COMMAND_RUNTIME_ERROR, args -> args.plain("reason", exception.getMessage()));
		}
		return CommandTree.ok();
	}

	private int list(MichelleCommandContext context) throws CommandSyntaxException {
		String value = cells.cells().isEmpty()
			? "none"
			: cells.cells().stream().map(CellDefinition::id).collect(Collectors.joining(", "));
		messages.send(context.playerExecutor(), MessageDefaults.CELLS_LIST, args -> args.plain("cells", value));
		return CommandTree.ok();
	}

	private int info(MichelleCommandContext context) throws CommandSyntaxException {
		CellDefinition cell = cells.get(context.arg("name", String.class));
		if (cell == null) {
			messages.send(context.playerExecutor(), MessageDefaults.CELLS_UNKNOWN);
			return CommandTree.ok();
		}
		var lease = cells.lease(cell.id());
		messages.send(context.playerExecutor(), MessageDefaults.CELLS_INFO, args -> args
			.plain("display_name", cell.displayName())
			.plain("cell", cell.id())
			.plain("price", cell.rentPrice())
			.plain("ward", cell.wardId().value())
			.plain("duration", cell.rentDurationSeconds())
			.plain("maximum", cell.maxRentDurationSeconds())
			.plain("state", lease == null ? "available" : "rented by " + lease.ownerId()));
		return CommandTree.ok();
	}

	private int setWard(MichelleCommandContext context) throws CommandSyntaxException {
		try {
			String id = context.arg("name", String.class);
			WardId wardId = parseWard(context);
			cells.setWard(id, wardId);
			renderer.refresh(cells.get(id));
			messages.send(context.playerExecutor(), MessageDefaults.CELLS_WARD_MOVED, args -> args
				.plain("cell", id)
				.plain("ward", wardId.value()));
		} catch (RuntimeException exception) {
			messages.send(context.playerExecutor(), MessageDefaults.COMMAND_RUNTIME_ERROR, args -> args.plain("reason", exception.getMessage()));
		}
		return CommandTree.ok();
	}

	private int displayName(MichelleCommandContext context) throws CommandSyntaxException {
		try {
			String id = context.arg("name", String.class);
			cells.setDisplayName(id, context.arg("display_name", String.class));
			renderer.refresh(cells.get(id));
			messages.send(context.playerExecutor(), MessageDefaults.CELLS_DISPLAY_NAME_UPDATED);
		} catch (RuntimeException exception) {
			messages.send(context.playerExecutor(), MessageDefaults.COMMAND_RUNTIME_ERROR, args -> args.plain("reason", exception.getMessage()));
		}
		return CommandTree.ok();
	}

	private int reset(MichelleCommandContext context) throws CommandSyntaxException {
		try {
			resets.reset(context.arg("name", String.class));
			messages.send(context.playerExecutor(), MessageDefaults.CELLS_RESET_STARTED);
		} catch (RuntimeException exception) {
			messages.send(context.playerExecutor(), MessageDefaults.COMMAND_RUNTIME_ERROR, args -> args.plain("reason", exception.getMessage()));
		}
		return CommandTree.ok();
	}

	private int captureBaseline(MichelleCommandContext context) throws CommandSyntaxException {
		Player player = context.playerExecutor();
		CellDefinition cell = cells.get(context.arg("name", String.class));
		if (cell == null) {
			messages.send(player, MessageDefaults.CELLS_UNKNOWN);
			return CommandTree.ok();
		}
		if (cells.lease(cell.id()) != null || cells.resetJob(cell.id()) != null) {
			messages.send(player, MessageDefaults.CELLS_BASELINE_NOT_AVAILABLE);
			return CommandTree.ok();
		}
		messages.send(player, MessageDefaults.CELLS_BASELINE_CAPTURING);
		baselineCapture.capture(cell).whenComplete((baseline, failure) -> {
			if (failure != null) {
				messages.send(player, MessageDefaults.CELLS_BASELINE_CAPTURE_FAILED, args -> args.plain("reason", rootMessage(failure)));
				return;
			}
			try {
				cells.replaceBaseline(cell.id(), baseline);
				messages.send(player, MessageDefaults.CELLS_BASELINE_UPDATED, args -> args.plain("cell", cell.id()));
			} catch (RuntimeException exception) {
				messages.send(player, MessageDefaults.CELLS_BASELINE_UPDATE_FAILED, args -> args.plain("reason", rootMessage(exception)));
			}
		});
		return CommandTree.ok();
	}

	private int member(MichelleCommandContext context, boolean add) throws CommandSyntaxException {
		String input = context.arg("player", String.class);
		OfflinePlayer target;
		try {
			target = Bukkit.getOfflinePlayer(UUID.fromString(input));
		} catch (IllegalArgumentException ignored) {
			target = Bukkit.getOfflinePlayerIfCached(input);
		}
		if (target == null) {
			messages.send(context.playerExecutor(), MessageDefaults.COMMAND_UNKNOWN_PLAYER);
			return CommandTree.ok();
		}
		try {
			if (add) cells.addMember(context.arg("name", String.class), target.getUniqueId());
			else cells.removeMember(context.arg("name", String.class), target.getUniqueId());
			String targetName = target.getName() == null ? target.getUniqueId().toString() : target.getName();
			messages.send(context.playerExecutor(), MessageDefaults.CELLS_MEMBER_CHANGED, args -> args
				.plain("action", add ? "Added" : "Removed")
				.plain("player", targetName)
				.plain("direction", add ? "to" : "from"));
		} catch (RuntimeException exception) {
			messages.send(context.playerExecutor(), MessageDefaults.COMMAND_RUNTIME_ERROR, args -> args.plain("reason", exception.getMessage()));
		}
		return CommandTree.ok();
	}

	private int members(MichelleCommandContext context) throws CommandSyntaxException {
		var values = cells.members(context.arg("name", String.class));
		messages.send(context.playerExecutor(), MessageDefaults.CELLS_MEMBERS_LIST, args -> args
			.plain("members", values.isEmpty() ? "none" : values));
		return CommandTree.ok();
	}

	private int sign(MichelleCommandContext context) throws CommandSyntaxException {
		var block = context.playerExecutor().getTargetBlockExact(6);
		if (block == null || !(block.getState() instanceof Sign sign)) {
			messages.send(context.playerExecutor(), MessageDefaults.CELLS_SIGN_LOOK);
			return CommandTree.ok();
		}
		try {
			signs.bind(sign, context.arg("name", String.class));
			messages.send(context.playerExecutor(), MessageDefaults.CELLS_SIGN_LINKED);
		} catch (RuntimeException exception) {
			messages.send(context.playerExecutor(), MessageDefaults.COMMAND_RUNTIME_ERROR, args -> args.plain("reason", exception.getMessage()));
		}
		return CommandTree.ok();
	}
}
