package com.voluble.titanMC.donatortools.protection;

import com.voluble.titanMC.regions.protection.bukkit.BukkitProtectionMapper;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.service.ProtectionService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class DonatorToolProtection implements BlockBreakPermission {

	private final ProtectionService protection;

	public DonatorToolProtection(ProtectionService protection) {
		this.protection = protection;
	}

	@Override
	public boolean canBreak(Player player, Block block) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(block, "block");
		return protection == null || protection.allowed(BukkitProtectionMapper.request(
			player, ProtectionAction.BLOCK_BREAK, block
		));
	}
}
