package com.voluble.titanMC.cinematics.runtime;

import java.util.Objects;

public record CinematicPlaybackOptions(
	CinematicEndMode endMode,
	Runnable holdCallback
) {
	private static final Runnable NO_OP = () -> {
	};

	public CinematicPlaybackOptions {
		Objects.requireNonNull(endMode, "endMode");
		holdCallback = Objects.requireNonNullElse(holdCallback, NO_OP);
	}

	public static CinematicPlaybackOptions defaults() {
		return new CinematicPlaybackOptions(CinematicEndMode.STOP_AND_RESTORE, NO_OP);
	}

	public static CinematicPlaybackOptions holdLastFrame() {
		return new CinematicPlaybackOptions(CinematicEndMode.HOLD_LAST_FRAME, NO_OP);
	}

	public CinematicPlaybackOptions withHoldCallback(Runnable callback) {
		return new CinematicPlaybackOptions(endMode, callback);
	}

	void notifyHeld() {
		holdCallback.run();
	}
}
