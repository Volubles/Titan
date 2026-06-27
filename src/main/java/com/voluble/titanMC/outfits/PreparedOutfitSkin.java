package com.voluble.titanMC.outfits;

import com.voluble.titanMC.outfits.skin.SkinPropertyData;

import java.util.Optional;

public record PreparedOutfitSkin(OutfitResult result, SkinPropertyData property) {
	public PreparedOutfitSkin {
		java.util.Objects.requireNonNull(result, "result");
	}

	public Optional<SkinPropertyData> propertyOptional() {
		return Optional.ofNullable(property);
	}

	public static PreparedOutfitSkin success(SkinPropertyData property) {
		return new PreparedOutfitSkin(OutfitResult.APPLIED, java.util.Objects.requireNonNull(property, "property"));
	}

	public static PreparedOutfitSkin failed(OutfitResult result) {
		return new PreparedOutfitSkin(result, null);
	}
}
