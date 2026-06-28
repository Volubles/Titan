package com.voluble.titanMC.display.screen;

import net.kyori.adventure.key.Key;

public record ScreenEffectOverlay(char character, Key font) {
	public ScreenEffectOverlay {
		java.util.Objects.requireNonNull(font, "font");
	}
}
