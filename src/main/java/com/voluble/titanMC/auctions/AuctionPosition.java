package com.voluble.titanMC.auctions;

import com.voluble.titanMC.ranks.model.WardId;
import org.bukkit.block.BlockFace;

import java.util.Objects;
import java.util.UUID;

public record AuctionPosition(String id, WardId wardId, UUID worldId, int x, int y, int z, BlockFace facing) {
	public AuctionPosition {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(wardId, "wardId");
		Objects.requireNonNull(worldId, "worldId");
		Objects.requireNonNull(facing, "facing");
	}
}
