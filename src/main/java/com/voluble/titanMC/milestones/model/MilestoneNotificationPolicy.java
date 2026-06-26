package com.voluble.titanMC.milestones.model;

import java.util.Objects;
import java.util.Optional;

public record MilestoneNotificationPolicy(
	Optional<Boolean> enabled,
	Optional<Boolean> playerMessage,
	Optional<Boolean> sound,
	Optional<Boolean> broadcast,
	Optional<Boolean> broadcastSound
) {
	public static final MilestoneNotificationPolicy DEFAULT = new MilestoneNotificationPolicy(
		Optional.empty(),
		Optional.empty(),
		Optional.empty(),
		Optional.empty(),
		Optional.empty()
	);

	public MilestoneNotificationPolicy {
		Objects.requireNonNull(enabled, "enabled");
		Objects.requireNonNull(playerMessage, "playerMessage");
		Objects.requireNonNull(sound, "sound");
		Objects.requireNonNull(broadcast, "broadcast");
		Objects.requireNonNull(broadcastSound, "broadcastSound");
	}

	public MilestoneNotificationPolicy merge(MilestoneNotificationPolicy override) {
		Objects.requireNonNull(override, "override");
		return new MilestoneNotificationPolicy(
			override.enabled.or(() -> enabled),
			override.playerMessage.or(() -> playerMessage),
			override.sound.or(() -> sound),
			override.broadcast.or(() -> broadcast),
			override.broadcastSound.or(() -> broadcastSound)
		);
	}

	public boolean enabled(boolean fallback) {
		return enabled.orElse(fallback);
	}

	public boolean playerMessage(boolean fallback) {
		return playerMessage.orElse(fallback);
	}

	public boolean sound(boolean fallback) {
		return sound.orElse(fallback);
	}

	public boolean broadcast(boolean fallback) {
		return broadcast.orElse(fallback);
	}

	public boolean broadcastSound(boolean fallback) {
		return broadcastSound.orElse(fallback);
	}
}
