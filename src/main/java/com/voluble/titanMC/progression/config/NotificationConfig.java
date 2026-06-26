package com.voluble.titanMC.progression.config;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record NotificationConfig(
		List<String> playerMessages,
		List<String> broadcastMessages,
		int broadcastEvery,
		Optional<String> playerSound,
		Optional<String> broadcastSound,
		Map<Integer, String> soundOverrides
) {
	public NotificationConfig {
		Objects.requireNonNull(playerMessages, "playerMessages");
		Objects.requireNonNull(broadcastMessages, "broadcastMessages");
		Objects.requireNonNull(playerSound, "playerSound");
		Objects.requireNonNull(broadcastSound, "broadcastSound");
		playerMessages = List.copyOf(playerMessages);
		broadcastMessages = List.copyOf(broadcastMessages);
		if (playerMessages.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("playerMessages must not contain null");
		}
		if (broadcastMessages.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("broadcastMessages must not contain null");
		}
		soundOverrides = Map.copyOf(Objects.requireNonNull(soundOverrides, "soundOverrides"));
		if (broadcastEvery < 0) {
			throw new IllegalArgumentException("broadcastEvery must be >= 0 (was " + broadcastEvery + ")");
		}
	}

	public boolean broadcastsEnabled() {
		return broadcastEvery > 0;
	}

	public boolean shouldBroadcast(int level) {
		return broadcastsEnabled() && level % broadcastEvery == 0;
	}

	public Optional<String> soundForLevel(int level) {
		String override = soundOverrides.get(level);
		return override != null ? Optional.of(override) : playerSound;
	}

	public static NotificationConfig defaults() {
		return new NotificationConfig(
			List.of("<green>Level up! You are now level <yellow>{level}</yellow>."),
			List.of("<gold>{player} reached level <yellow>{level}</yellow>!"),
			5,
			Optional.of("entity.player.levelup"),
			Optional.of("entity.experience_orb.pickup"),
			Map.of()
		);
	}
}
