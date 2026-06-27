package com.voluble.titanMC.cinematics.editor;

import java.util.Locale;

final class CinematicTimeFormat {
	private CinematicTimeFormat() {
	}

	static String tickTime(int ticks) {
		return ticks + " ticks (" + seconds(ticks) + "s)";
	}

	static String seconds(int ticks) {
		double seconds = ticks / 20.0;
		if (Math.abs(seconds - Math.rint(seconds)) < 0.0001) {
			return Integer.toString((int) Math.rint(seconds));
		}
		return String.format(Locale.US, "%.2f", seconds);
	}
}
