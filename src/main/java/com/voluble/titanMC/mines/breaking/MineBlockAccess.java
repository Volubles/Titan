package com.voluble.titanMC.mines.breaking;

import com.voluble.titanMC.mines.Mine;
import com.voluble.titanMC.mines.MineManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class MineBlockAccess {
	public static final String BYPASS_PERMISSION = "titanmc.mine.break-bypass";
	private final MineManager mines;

	public MineBlockAccess(MineManager mines) {
		this.mines = Objects.requireNonNull(mines, "mines");
	}

	public MineBreakDecision evaluate(Player player, Block block) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(block, "block");
		Mine mine = mines.getFirstAt(block.getLocation());
		if (mine == null) return MineBreakDecision.OUTSIDE_MINE;
		if (player.hasPermission(BYPASS_PERMISSION)) return MineBreakDecision.BYPASS;
		return mine.getBreakProfile().allows(block.getType())
			? MineBreakDecision.ALLOWED
			: MineBreakDecision.MATERIAL_DENIED;
	}

	public boolean canBreak(Player player, Block block) {
		return evaluate(player, block).allowed();
	}
}
