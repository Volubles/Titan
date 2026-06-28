package com.voluble.titanMC.onboarding.presentation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class OnboardingPresentationPlayback {
	private final CompletableFuture<Void> completion;
	private final Runnable cancel;

	OnboardingPresentationPlayback(CompletableFuture<Void> completion, Runnable cancel) {
		this.completion = completion;
		this.cancel = cancel;
	}

	public CompletionStage<Void> completion() {
		return completion;
	}

	public void cancel() {
		cancel.run();
	}
}
