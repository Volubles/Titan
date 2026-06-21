package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface ManagedBlockAccess {
	boolean allows(Player player, ProtectionAction action, Block block);

	static ManagedBlockAccess none() {
		return (player, action, block) -> false;
	}
}
