package com.voluble.titanMC.onboarding.preview.actor;

import com.voluble.titanMC.onboarding.preview.OutfitPreview;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

final class CarouselPreviewStrategy implements PreviewStrategy {
	private static final long SIDE_SPAWN_DELAY_TICKS = 6L;

	private final PreviewActorFactory actors;
	private final Set<PreviewActor> visible = new LinkedHashSet<>();
	private Wheel wheel;
	private Integer focusIndex;

	CarouselPreviewStrategy(PreviewActorFactory actors) {
		this.actors = actors;
	}

	@Override
	public CompletableFuture<Void> show(OutfitPreview.PreviewScene scene) {
		if (wheel == null || focusIndex == null) return initialize(scene);
		int direction = scene.rotationDirection() != 0
			? scene.rotationDirection()
			: direction(focusIndex, scene.selectedIndex(), scene.selectionSize());
		focusIndex = scene.selectedIndex();
		if (direction >= 0) return rotateForward(scene);
		return rotateBackward(scene);
	}

	@Override
	public void remove() {
		for (PreviewActor actor : Set.copyOf(visible)) {
			actor.remove();
		}
		visible.clear();
		wheel = null;
		focusIndex = null;
	}

	private CompletableFuture<Void> initialize(OutfitPreview.PreviewScene scene) {
		focusIndex = scene.selectedIndex();
		CarouselPreviewLayout path = CarouselPreviewLayout.from(scene.stage());
		PreviewActor left = actors.create(scene.previous());
		PreviewActor focus = actors.create(scene.focus());
		PreviewActor right = actors.create(scene.next());
		left.stageAt(path.leftStage());
		focus.stageAt(path.focus());
		right.stageAt(path.rightStage());
		visible.add(left);
		visible.add(focus);
		visible.add(right);
		wheel = new Wheel(left, focus, right);
		return CompletableFuture.completedFuture(null);
	}

	private CompletableFuture<Void> rotateForward(OutfitPreview.PreviewScene scene) {
		CarouselPreviewLayout path = CarouselPreviewLayout.from(scene.stage());
		PreviewActor oldLeft = wheel.left;
		PreviewActor oldFocus = wheel.focus;
		PreviewActor oldRight = wheel.right;
		PreviewActor newRight = actors.create(scene.next());
		visible.add(newRight);
		CompletableFuture<Void> oldLeftMove = oldLeft.exitTo(path.leftExit())
			.whenComplete((ignored, failure) -> visible.remove(oldLeft));
		CompletableFuture<Void> focusSideMove = oldFocus.moveToStage(path.leftStage());
		CompletableFuture<Void> focusMove = oldRight.moveToFocus(path.focus());
		CompletableFuture<Void> spawnMove = newRight.stageAtLater(path.rightEntrance(), SIDE_SPAWN_DELAY_TICKS)
			.thenCompose(ignored -> newRight.moveToStage(path.rightStage()));
		wheel = new Wheel(oldFocus, oldRight, newRight);
		return CompletableFuture.allOf(oldLeftMove, focusSideMove, focusMove, spawnMove);
	}

	private CompletableFuture<Void> rotateBackward(OutfitPreview.PreviewScene scene) {
		CarouselPreviewLayout path = CarouselPreviewLayout.from(scene.stage());
		PreviewActor oldLeft = wheel.left;
		PreviewActor oldFocus = wheel.focus;
		PreviewActor oldRight = wheel.right;
		PreviewActor newLeft = actors.create(scene.previous());
		visible.add(newLeft);
		CompletableFuture<Void> oldRightMove = oldRight.exitTo(path.rightExit())
			.whenComplete((ignored, failure) -> visible.remove(oldRight));
		CompletableFuture<Void> focusSideMove = oldFocus.moveToStage(path.rightStage());
		CompletableFuture<Void> focusMove = oldLeft.moveToFocus(path.focus());
		CompletableFuture<Void> spawnMove = newLeft.stageAtLater(path.leftEntrance(), SIDE_SPAWN_DELAY_TICKS)
			.thenCompose(ignored -> newLeft.moveToStage(path.leftStage()));
		wheel = new Wheel(newLeft, oldLeft, oldFocus);
		return CompletableFuture.allOf(oldRightMove, focusSideMove, focusMove, spawnMove);
	}

	private int direction(int previous, int next, int size) {
		if (previous == next || size <= 1) return 1;
		if (Math.floorMod(previous + 1, size) == next) return 1;
		if (Math.floorMod(previous - 1, size) == next) return -1;
		int forward = Math.floorMod(next - previous, size);
		int backward = Math.floorMod(previous - next, size);
		return forward <= backward ? 1 : -1;
	}

	private record Wheel(PreviewActor left, PreviewActor focus, PreviewActor right) {
		private Wheel {
			Objects.requireNonNull(left, "left");
			Objects.requireNonNull(focus, "focus");
			Objects.requireNonNull(right, "right");
		}
	}
}
