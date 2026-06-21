package com.voluble.titanMC.auctions;

import org.bukkit.block.BlockFace;

import java.util.UUID;

public record AuctionPosition(String id, UUID worldId, int x, int y, int z, BlockFace facing) {
}
