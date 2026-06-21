package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ManagedBlockAccessRegistry implements ManagedBlockAccess {
	private final Logger logger;
	private final CopyOnWriteArrayList<ManagedBlockAccess> entries = new CopyOnWriteArrayList<>();

	public ManagedBlockAccessRegistry(Logger logger) {
		this.logger = Objects.requireNonNull(logger, "logger");
	}

	public void register(ManagedBlockAccess access) {
		entries.add(Objects.requireNonNull(access, "access"));
	}

	@Override
	public boolean allows(Player player, ProtectionAction action, Block block) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(action, "action");
		Objects.requireNonNull(block, "block");
		for (ManagedBlockAccess entry : entries) {
			try {
				if (entry.allows(player, action, block)) return true;
			} catch (RuntimeException failure) {
				logger.log(
					Level.SEVERE,
					"Managed block access failed for " + action + " at "
						+ block.getWorld().getName() + " " + block.getX() + "," + block.getY() + "," + block.getZ(),
					failure
				);
			}
		}
		return false;
	}
}
