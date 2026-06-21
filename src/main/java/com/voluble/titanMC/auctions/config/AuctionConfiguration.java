package com.voluble.titanMC.auctions.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public record AuctionConfiguration(
	long minimumPrice,
	long maximumPrice,
	long saleDurationMillis,
	long claimDurationMillis,
	Material chestMaterial,
	Material signMaterial,
	List<String> forSaleSign,
	List<String> claimedSign,
	List<String> publicSign
) {
	public AuctionConfiguration {
		if (minimumPrice < 0 || maximumPrice < minimumPrice || maximumPrice == Long.MAX_VALUE) {
			throw new IllegalArgumentException("invalid auction price range");
		}
		if (saleDurationMillis < 1000 || claimDurationMillis < 1000) throw new IllegalArgumentException("auction durations must be positive");
		if (!(chestMaterial.createBlockData() instanceof org.bukkit.block.data.type.Chest)) {
			throw new IllegalArgumentException("blocks.chest must be a chest block");
		}
		if (!(signMaterial.createBlockData() instanceof org.bukkit.block.data.type.WallSign)) {
			throw new IllegalArgumentException("blocks.sign must be a wall sign block");
		}
		forSaleSign = lines(forSaleSign, "for-sale");
		claimedSign = lines(claimedSign, "claimed");
		publicSign = lines(publicSign, "public");
	}

	public static AuctionConfiguration load(FileConfiguration yaml) {
		Material chest = material(yaml.getString("blocks.chest", "CHEST"));
		Material sign = material(yaml.getString("blocks.sign", "OAK_WALL_SIGN"));
		return new AuctionConfiguration(
			yaml.getLong("price.minimum", 500),
			yaml.getLong("price.maximum", 5000),
			yaml.getLong("timers.sale-seconds", 172800) * 1000L,
			yaml.getLong("timers.claim-seconds", 300) * 1000L,
			chest,
			sign,
			yaml.getStringList("signs.for-sale"),
			yaml.getStringList("signs.claimed"),
			yaml.getStringList("signs.public")
		);
	}

	private static Material material(String value) {
		Material material = Material.matchMaterial(value);
		if (material == null || !material.isBlock()) throw new IllegalArgumentException("invalid auction block: " + value);
		return material;
	}

	private static List<String> lines(List<String> value, String key) {
		if (value.size() != 4) throw new IllegalArgumentException("signs." + key + " must contain four lines");
		return List.copyOf(value);
	}
}
