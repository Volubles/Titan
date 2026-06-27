package com.voluble.titanMC.onboarding.preview;

import com.voluble.titanMC.outfits.skin.SkinPropertyData;
import com.voluble.titanMC.onboarding.config.OnboardingPreviewStage;
import org.bukkit.entity.Player;

public interface OutfitPreview {
	boolean available();

	void show(Player player, PreviewModel model) throws PreviewException;

	void remove(Player player);

	record PreviewModel(String name, OnboardingPreviewStage stage, SkinPropertyData skin) {
		public PreviewModel {
			java.util.Objects.requireNonNull(name, "name");
			java.util.Objects.requireNonNull(stage, "stage");
			java.util.Objects.requireNonNull(skin, "skin");
		}
	}
}
