package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class BukkitProtectionMapper {

	private BukkitProtectionMapper() {}

	public static ProtectionActor actor(Player player) {
		Objects.requireNonNull(player, "player");
		Set<String> permissions = player.getEffectivePermissions().stream()
			.filter(PermissionAttachmentInfo::getValue)
			.map(PermissionAttachmentInfo::getPermission)
			.collect(Collectors.toUnmodifiableSet());
		return ProtectionActor.player(player.getUniqueId(), permissions);
	}

	public static BlockPosition position(Block block) {
		Objects.requireNonNull(block, "block");
		return new BlockPosition(
			new WorldId(block.getWorld().getUID()), block.getX(), block.getY(), block.getZ()
		);
	}

	public static BlockPosition position(Location location) {
		Objects.requireNonNull(location, "location");
		World world = Objects.requireNonNull(location.getWorld(), "location world");
		return new BlockPosition(
			new WorldId(world.getUID()), location.getBlockX(), location.getBlockY(), location.getBlockZ()
		);
	}

	public static ProtectionRequest request(Player player, ProtectionAction action, Block block) {
		return ProtectionRequest.at(actor(player), Objects.requireNonNull(action, "action"), position(block));
	}
}
