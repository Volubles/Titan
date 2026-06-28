package com.voluble.titanMC.onboarding;

import com.voluble.titanMC.outfits.model.OutfitId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnboardingOutfitSelectionTest {
	@Test
	void parsesOriginalSelection() {
		OnboardingOutfitSelection selection = OnboardingOutfitSelection.parse("original");

		assertTrue(selection.original());
		assertEquals("original", selection.storageValue());
	}

	@Test
	void parsesOutfitSelection() {
		OnboardingOutfitSelection selection = OnboardingOutfitSelection.parse("prison");

		assertEquals(OutfitId.of("prison"), selection.outfitId());
		assertEquals("prison", selection.storageValue());
	}
}
