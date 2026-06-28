package com.voluble.titanMC.display.screen;

public record ScreenEffectTiming(long fadeInTicks, long holdTicks, long fadeOutTicks) {
	public ScreenEffectTiming {
		if (fadeInTicks < 0L) throw new IllegalArgumentException("fade-in ticks must not be negative");
		if (holdTicks < 0L) throw new IllegalArgumentException("hold ticks must not be negative");
		if (fadeOutTicks < 0L) throw new IllegalArgumentException("fade-out ticks must not be negative");
	}

	public long totalTicks() {
		return fadeInTicks + holdTicks + fadeOutTicks;
	}
}
