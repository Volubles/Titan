package com.voluble.titanMC.onboarding.preview;

import org.bukkit.entity.Player;

public final class UnavailableOutfitPreview implements OutfitPreview {
	@Override
	public boolean available() {
		return false;
	}

	@Override
	public void show(Player player, PreviewModel model) throws PreviewException {
		throw new PreviewException("FancyNPCs is not installed or enabled");
	}

	@Override
	public void remove(Player player) {
	}
}
