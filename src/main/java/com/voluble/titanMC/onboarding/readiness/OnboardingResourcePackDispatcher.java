package com.voluble.titanMC.onboarding.readiness;

import org.bukkit.entity.Player;

public interface OnboardingResourcePackDispatcher {
	boolean available();

	void dispatch(Player player);

	static OnboardingResourcePackDispatcher unavailable() {
		return new OnboardingResourcePackDispatcher() {
			@Override
			public boolean available() {
				return false;
			}

			@Override
			public void dispatch(Player player) {
				throw new IllegalStateException("resource pack dispatcher is unavailable");
			}
		};
	}
}
