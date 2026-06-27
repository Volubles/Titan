package com.voluble.titanMC.outfits.skin;

import java.util.Locale;

public enum MineSkinVisibility {
	PUBLIC("public"),
	UNLISTED("unlisted"),
	PRIVATE("private");

	private final String apiName;

	MineSkinVisibility(String apiName) {
		this.apiName = apiName;
	}

	public String apiName() {
		return apiName;
	}

	public static MineSkinVisibility parse(String input) {
		if (input == null || input.isBlank()) return UNLISTED;
		return valueOf(input.trim().toUpperCase(Locale.ROOT));
	}
}
