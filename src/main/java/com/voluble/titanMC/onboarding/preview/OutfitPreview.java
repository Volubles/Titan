package com.voluble.titanMC.onboarding.preview;

import com.voluble.titanMC.outfits.skin.SkinPropertyData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface OutfitPreview {
	boolean available();

	void show(Player player, PreviewModel model) throws PreviewException;

	void remove(Player player);

	record PreviewModel(String name, Location location, SkinPropertyData skin) {
		public PreviewModel {
			java.util.Objects.requireNonNull(name, "name");
			java.util.Objects.requireNonNull(location, "location");
			java.util.Objects.requireNonNull(skin, "skin");
		}
	}
}
