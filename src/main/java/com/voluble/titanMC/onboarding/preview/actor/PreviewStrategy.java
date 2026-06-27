package com.voluble.titanMC.onboarding.preview.actor;

import com.voluble.titanMC.onboarding.preview.OutfitPreview;

import java.util.concurrent.CompletableFuture;

interface PreviewStrategy {
	CompletableFuture<Void> show(OutfitPreview.PreviewScene scene);

	void remove();
}
