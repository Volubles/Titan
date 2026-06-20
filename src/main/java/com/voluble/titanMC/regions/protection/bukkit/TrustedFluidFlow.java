package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.model.BlockPosition;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

public final class TrustedFluidFlow {

	private final Set<BlockPosition> trusted = new HashSet<>();

	public boolean contains(Block block) {
		return trusted.contains(BukkitProtectionMapper.position(block));
	}

	public void add(Block block) {
		trusted.add(BukkitProtectionMapper.position(block));
	}

	public void remove(Block block) {
		trusted.remove(BukkitProtectionMapper.position(block));
	}
}
