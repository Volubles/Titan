package com.voluble.titanMC.ranks.config;

import java.util.Objects;
import java.util.Optional;

public record RankNotificationEvent(
	boolean enabled,
	RankNotificationMessage playerMessage,
	RankNotificationMessage broadcastMessage,
	Optional<String> sound,
	Optional<String> broadcastSound
) {
	public RankNotificationEvent {
		Objects.requireNonNull(playerMessage, "playerMessage");
		Objects.requireNonNull(broadcastMessage, "broadcastMessage");
		Objects.requireNonNull(sound, "sound");
		Objects.requireNonNull(broadcastSound, "broadcastSound");
	}

	public boolean broadcasts() {
		return enabled && broadcastMessage.enabled() && broadcastMessage.hasLines();
	}

	public boolean sendsPlayerMessage() {
		return enabled && playerMessage.enabled() && playerMessage.hasLines();
	}
}
