package com.voluble.titanMC.onboarding.config;

import com.voluble.titanMC.cinematics.model.CinematicId;
import com.voluble.titanMC.outfits.model.OutfitId;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Objects;

public record OnboardingConfiguration(
	boolean enabled,
	boolean firstJoinEnabled,
	long firstJoinDelayTicks,
	CinematicId cinematic,
	long inputCooldownMillis,
	OnboardingPreviewStage previewStage,
	List<OutfitId> outfits
) {
	public OnboardingConfiguration {
		Objects.requireNonNull(cinematic, "cinematic");
		Objects.requireNonNull(previewStage, "previewStage");
		outfits = List.copyOf(Objects.requireNonNull(outfits, "outfits"));
		if (firstJoinDelayTicks < 0L) throw new IllegalArgumentException("first join delay must not be negative");
		if (inputCooldownMillis < 0L) throw new IllegalArgumentException("input cooldown must not be negative");
		if (outfits.isEmpty()) throw new IllegalArgumentException("onboarding.yml must define at least one outfit");
	}

	public static OnboardingConfiguration load(FileConfiguration yaml) {
		ConfigurationSection firstJoin = yaml.getConfigurationSection("first-join");
		ConfigurationSection input = yaml.getConfigurationSection("input");
		return new OnboardingConfiguration(
			yaml.getBoolean("enabled", true),
			firstJoin == null || firstJoin.getBoolean("enabled", true),
			firstJoin == null ? 40L : firstJoin.getLong("delay-ticks", 40L),
			CinematicId.of(yaml.getString("cinematic", "onboarding_intro")),
			input == null ? 300L : input.getLong("repeat-cooldown-ms", 300L),
			OnboardingPreviewStage.load(yaml),
			yaml.getStringList("outfits").stream().map(OutfitId::of).toList()
		);
	}

	private static ConfigurationSection requireSection(FileConfiguration yaml, String path) {
		ConfigurationSection section = yaml.getConfigurationSection(path);
		if (section == null) throw new IllegalArgumentException("Missing section: " + path);
		return section;
	}

	public record LocationSpec(String world, double x, double y, double z, float yaw, float pitch) {
		public LocationSpec {
			world = Objects.requireNonNull(world, "world").trim();
			if (world.isBlank()) throw new IllegalArgumentException("location world must not be blank");
		}

		public static LocationSpec load(ConfigurationSection section) {
			return new LocationSpec(
				section.getString("world", "world"),
				section.getDouble("x"),
				section.getDouble("y"),
				section.getDouble("z"),
				(float) section.getDouble("yaw"),
				(float) section.getDouble("pitch")
			);
		}

		public static LocationSpec from(Location location) {
			World world = Objects.requireNonNull(location.getWorld(), "location world");
			return new LocationSpec(
				world.getName(),
				location.getX(),
				location.getY(),
				location.getZ(),
				location.getYaw(),
				location.getPitch()
			);
		}

		public Location toLocation() {
			World bukkitWorld = Bukkit.getWorld(world);
			if (bukkitWorld == null) throw new IllegalStateException("Unknown onboarding world: " + world);
			return new Location(bukkitWorld, x, y, z, yaw, pitch);
		}
	}
}
