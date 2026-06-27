package com.voluble.titanMC.onboarding.preview.actor;

import com.voluble.titanMC.onboarding.preview.OutfitPreview;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

final class RunwayPreviewStrategy implements PreviewStrategy {
	private final PreviewActorFactory actors;
	private final Set<PreviewActor> visible = new LinkedHashSet<>();
	private PreviewActor active;

	RunwayPreviewStrategy(PreviewActorFactory actors) {
		this.actors = actors;
	}

	@Override
	public CompletableFuture<Void> show(OutfitPreview.PreviewScene scene) {
		if (active != null) {
			PreviewActor outgoing = active;
			outgoing.exit().whenComplete((ignored, failure) -> visible.remove(outgoing));
		}
		PreviewActor next = actors.create(scene.focus());
		active = next;
		visible.add(next);
		return next.enter();
	}

	@Override
	public void remove() {
		for (PreviewActor actor : Set.copyOf(visible)) {
			actor.remove();
		}
		visible.clear();
		active = null;
	}
}
