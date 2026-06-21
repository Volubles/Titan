package com.voluble.titanMC.cells.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.voluble.titanMC.cells.CellManager;
import com.voluble.titanMC.cells.CellResetService;
import com.voluble.titanMC.cells.CellSignService;
import com.voluble.titanMC.cells.model.CellDefinition;
import com.voluble.titanMC.regions.model.CuboidGeometry;
import com.voluble.titanMC.regions.selection.SelectionException;
import com.voluble.titanMC.regions.selection.WorldEditRegionSelection;
import com.voluble.titanMC.util.RegionUtils;
import io.voluble.michellelib.commands.CommandModule;
import io.voluble.michellelib.commands.CommandRegistration;
import io.voluble.michellelib.commands.arguments.Args;
import io.voluble.michellelib.commands.context.MichelleCommandContext;
import io.voluble.michellelib.commands.suggest.Suggest;
import io.voluble.michellelib.commands.tree.CommandTree;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class CellCommandModule implements CommandModule {
	private final CellManager cells;private final CellResetService resets;private final CellSignService signs;
	public CellCommandModule(CellManager cells,CellResetService resets,CellSignService signs){this.cells=cells;this.resets=resets;this.signs=signs;}
	@Override public void register(CommandRegistration registration){var names=Suggest.fromContext(source->cells.cells().stream().map(CellDefinition::id).toList());registration.register(CommandTree.root("cell").aliases("cells").description("Manage rentable cells").requiresPermission("titanmc.cell.admin").requiresPlayerExecutor().executes(this::root)
		.literalExec("list",this::list)
		.literal("create",node->node.argument("name",Args.word(),name->name.argument("price",Args.longArg(),price->price.argument("duration",Args.timeDuration(),duration->duration.executes(this::create)))))
		.literal("delete",node->node.argument("name",Args.word(),name->name.suggests(names).executes(this::delete)))
		.literal("info",node->node.argument("name",Args.word(),name->name.suggests(names).executes(this::info)))
		.literal("reset",node->node.argument("name",Args.word(),name->name.suggests(names).executes(this::reset)))
		.literal("sign",node->node.argument("name",Args.word(),name->name.suggests(names).executes(this::sign))).spec());}
	private int root(MichelleCommandContext c)throws CommandSyntaxException{c.playerExecutor().sendMessage("Usage: /cell <create|delete|list|info|reset|sign>");return CommandTree.ok();}
	private int create(MichelleCommandContext c)throws CommandSyntaxException{Player p=c.playerExecutor();try{var selected=WorldEditRegionSelection.read(p);if(!(selected.geometry() instanceof CuboidGeometry cuboid)){p.sendMessage("Cells must use a cuboid selection.");return CommandTree.ok();}var b=cuboid.bounds();CellDefinition cell=new CellDefinition(c.arg("name",String.class),new RegionUtils.Cuboid(selected.worldId(),b.minX(),b.minY(),b.minZ(),b.maxXExclusive()-1,b.maxYExclusive()-1,b.maxZExclusive()-1),c.arg("price",Long.class),c.arg("duration",Duration.class).toSeconds(),true);cells.create(cell);p.sendMessage("Created cell '"+cell.id()+"'.");}catch(SelectionException|RuntimeException e){p.sendMessage(e.getMessage());}return CommandTree.ok();}
	private int delete(MichelleCommandContext c)throws CommandSyntaxException{try{cells.delete(c.arg("name",String.class));c.playerExecutor().sendMessage("Deleted cell.");}catch(RuntimeException e){c.playerExecutor().sendMessage(e.getMessage());}return CommandTree.ok();}
	private int list(MichelleCommandContext c)throws CommandSyntaxException{String value=cells.cells().isEmpty()?"none":cells.cells().stream().map(CellDefinition::id).collect(java.util.stream.Collectors.joining(", "));c.playerExecutor().sendMessage("Cells: "+value);return CommandTree.ok();}
	private int info(MichelleCommandContext c)throws CommandSyntaxException{CellDefinition cell=cells.get(c.arg("name",String.class));if(cell==null){c.playerExecutor().sendMessage("Unknown cell.");return CommandTree.ok();}var lease=cells.lease(cell.id());c.playerExecutor().sendMessage(cell.id()+" | $"+cell.rentPrice()+" | "+cell.rentDurationSeconds()+"s | "+(lease==null?"available":"rented by "+lease.ownerId()));return CommandTree.ok();}
	private int reset(MichelleCommandContext c)throws CommandSyntaxException{try{resets.reset(c.arg("name",String.class));c.playerExecutor().sendMessage("Cell reset started.");}catch(RuntimeException e){c.playerExecutor().sendMessage(e.getMessage());}return CommandTree.ok();}
	private int sign(MichelleCommandContext c)throws CommandSyntaxException{var block=c.playerExecutor().getTargetBlockExact(6);if(block==null||!(block.getState() instanceof Sign sign)){c.playerExecutor().sendMessage("Look at a sign within 6 blocks.");return CommandTree.ok();}try{signs.bind(sign,c.arg("name",String.class));c.playerExecutor().sendMessage("Rental sign linked.");}catch(RuntimeException e){c.playerExecutor().sendMessage(e.getMessage());}return CommandTree.ok();}
}
