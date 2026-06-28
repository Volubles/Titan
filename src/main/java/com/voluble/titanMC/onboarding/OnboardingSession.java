package com.voluble.titanMC.onboarding;

import com.voluble.titanMC.cinematics.runtime.CinematicRuntime;
import com.voluble.titanMC.cinematics.runtime.CinematicPlaybackOptions;
import com.voluble.titanMC.display.notice.MessageDefaults;
import com.voluble.titanMC.display.notice.PluginMessageService;
import com.voluble.titanMC.onboarding.config.OnboardingConfiguration;
import com.voluble.titanMC.onboarding.config.OnboardingPreviewMode;
import com.voluble.titanMC.onboarding.persistence.OnboardingStorage;
import com.voluble.titanMC.onboarding.presentation.OnboardingPresentationPlayback;
import com.voluble.titanMC.onboarding.presentation.OnboardingPresentationRunner;
import com.voluble.titanMC.onboarding.preview.OutfitPreview;
import com.voluble.titanMC.outfits.OutfitResult;
import com.voluble.titanMC.outfits.OutfitService;
import com.voluble.titanMC.outfits.PreparedOutfitSkin;
import com.voluble.titanMC.outfits.config.OutfitConfigurationManager;
import com.voluble.titanMC.outfits.model.OutfitDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class OnboardingSession {
	private final Plugin plugin;
	private final Player player;
	private final OnboardingConfiguration configuration;
	private final CinematicRuntime cinematics;
	private final OutfitService outfits;
	private final OutfitConfigurationManager outfitConfiguration;
	private final OutfitPreview preview;
	private final OnboardingPresentationRunner presentation;
	private final OnboardingStorage storage;
	private final PluginMessageService messages;
	private final Logger logger;
	private final Consumer<UUID> completion;
	private int outfitIndex;
	private long lastInputMillis;
	private boolean stopping;
	private boolean interactive;
	private boolean previewTransitioning;
	private int previewGeneration;
	private OnboardingPresentationPlayback presentationPlayback;

	public OnboardingSession(
		Plugin plugin,
		Player player,
		OnboardingConfiguration configuration,
		CinematicRuntime cinematics,
		OutfitService outfits,
		OutfitConfigurationManager outfitConfiguration,
		OutfitPreview preview,
		OnboardingPresentationRunner presentation,
		OnboardingStorage storage,
		PluginMessageService messages,
		Logger logger,
		Consumer<UUID> completion
	) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.player = Objects.requireNonNull(player, "player");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.cinematics = Objects.requireNonNull(cinematics, "cinematics");
		this.outfits = Objects.requireNonNull(outfits, "outfits");
		this.outfitConfiguration = Objects.requireNonNull(outfitConfiguration, "outfitConfiguration");
		this.preview = Objects.requireNonNull(preview, "preview");
		this.presentation = Objects.requireNonNull(presentation, "presentation");
		this.storage = Objects.requireNonNull(storage, "storage");
		this.messages = Objects.requireNonNull(messages, "messages");
		this.logger = Objects.requireNonNull(logger, "logger");
		this.completion = Objects.requireNonNull(completion, "completion");
	}

	public UUID playerId() {
		return player.getUniqueId();
	}

	public void start() {
		CinematicRuntime.StartResult result = cinematics.start(
			player,
			configuration.cinematic(),
			CinematicPlaybackOptions.holdLastFrame().withHoldCallback(this::beginSelection)
		);
		if (result != CinematicRuntime.StartResult.STARTED) {
			messages.send(player, MessageDefaults.ONBOARDING_START_FAILED);
			stop(false);
			return;
		}
	}

	public void handle(Input input) {
		if (stopping || !interactive || input == null) return;
		long now = System.currentTimeMillis();
		if (now - lastInputMillis < configuration.inputCooldownMillis()) return;
		if (input.isRight() && !input.isLeft()) {
			if (previewTransitioning) return;
			lastInputMillis = now;
			nextOutfit(1);
		} else if (input.isLeft() && !input.isRight()) {
			if (previewTransitioning) return;
			lastInputMillis = now;
			previousOutfit(-1);
		} else if (input.isJump()) {
			lastInputMillis = now;
			confirm();
		} else if (input.isSneak()) {
			lastInputMillis = now;
			cancel();
		}
	}

	public void stop(boolean restorePlayer) {
		if (stopping) return;
		stopping = true;
		if (presentationPlayback != null) presentationPlayback.cancel();
		preview.remove(player);
		cinematics.stop(player.getUniqueId(), restorePlayer);
		completion.accept(player.getUniqueId());
	}

	private void nextOutfit(int direction) {
		if (configuration.outfits().size() <= 1) return;
		outfitIndex = (outfitIndex + 1) % configuration.outfits().size();
		showSelectedOutfit(direction);
	}

	private void beginSelection() {
		if (stopping || !player.isOnline()) return;
		presentationPlayback = presentation.play(player, configuration.presentation());
		presentationPlayback.completion().whenComplete((ignored, failure) -> {
			if (failure != null) return;
			Bukkit.getScheduler().runTask(plugin, this::beginInteractiveSelection);
		});
	}

	private void beginInteractiveSelection() {
		if (stopping || !player.isOnline()) return;
		interactive = true;
		messages.send(player, MessageDefaults.ONBOARDING_STARTED);
		presentation.playSound(player, configuration.presentation().previewSpawnSound());
		showSelectedOutfit(0);
	}

	private void previousOutfit(int direction) {
		if (configuration.outfits().size() <= 1) return;
		outfitIndex = (outfitIndex - 1 + configuration.outfits().size()) % configuration.outfits().size();
		showSelectedOutfit(direction);
	}

	private void showSelectedOutfit(int direction) {
		OnboardingOutfitSelection selection = selectedOutfit();
		messages.send(player, MessageDefaults.ONBOARDING_OUTFIT_SELECTED, args -> args.plain("outfit", selectionName(selection)));
		if (!preview.available()) {
			messages.send(player, MessageDefaults.ONBOARDING_PREVIEW_UNAVAILABLE);
			return;
		}
		int generation = ++previewGeneration;
		if (configuration.previewMode() == OnboardingPreviewMode.CAROUSEL) {
			previewTransitioning = true;
			showCarouselPreview(generation, direction);
		} else {
			preparePreviewModel(generation, outfitIndex, model -> {
				if (model != null) showPreview(generation, model);
			});
		}
	}

	private void showCarouselPreview(int generation, int direction) {
		int size = configuration.outfits().size();
		int previous = Math.floorMod(outfitIndex - 1, size);
		int focus = outfitIndex;
		int next = Math.floorMod(outfitIndex + 1, size);
		List<Integer> required = List.of(previous, focus, next);
		Map<Integer, OutfitPreview.PreviewModel> models = new HashMap<>();
		preparePreviewModels(generation, required, 0, models, prepared -> {
			OutfitPreview.PreviewModel previousModel = prepared.get(previous);
			OutfitPreview.PreviewModel focusModel = prepared.get(focus);
			OutfitPreview.PreviewModel nextModel = prepared.get(next);
			if (previousModel == null || focusModel == null || nextModel == null) {
				finishPreviewTransition(generation);
				return;
			}
			showPreview(generation, new OutfitPreview.PreviewScene(
				configuration.previewMode(),
				configuration.previewStage(),
				previousModel,
				focusModel,
				nextModel,
				outfitIndex,
				size,
				direction
			));
		});
	}

	private void preparePreviewModels(
		int generation,
		List<Integer> indices,
		int position,
		Map<Integer, OutfitPreview.PreviewModel> models,
		Consumer<Map<Integer, OutfitPreview.PreviewModel>> callback
	) {
		if (stopping || generation != previewGeneration) return;
		if (position >= indices.size()) {
			callback.accept(models);
			return;
		}
		int index = indices.get(position);
		if (models.containsKey(index)) {
			preparePreviewModels(generation, indices, position + 1, models, callback);
			return;
		}
		preparePreviewModel(generation, index, model -> {
			if (model == null) {
				finishPreviewTransition(generation);
				return;
			}
			models.put(index, model);
			Bukkit.getScheduler().runTask(plugin, () ->
				preparePreviewModels(generation, indices, position + 1, models, callback)
			);
		});
	}

	private void preparePreviewModel(int generation, int index, Consumer<OutfitPreview.PreviewModel> callback) {
		OnboardingOutfitSelection selection = configuration.outfits().get(index);
		Consumer<PreparedOutfitSkin> preparedCallback = prepared -> {
			if (stopping || generation != previewGeneration) return;
			if (!prepared(prepared)) {
				messages.send(player, MessageDefaults.ONBOARDING_PREVIEW_FAILED);
				callback.accept(null);
				return;
			}
			callback.accept(new OutfitPreview.PreviewModel(
				selectionName(selection),
				configuration.previewStage(),
				prepared.property()
			));
		};
		if (selection.original()) {
			outfits.prepareOriginalSkin(player, preparedCallback);
			return;
		}
		outfits.prepareOutfitSkin(player, selection.outfitId(), preparedCallback);
	}

	private boolean prepared(PreparedOutfitSkin prepared) {
		return prepared.result() == OutfitResult.APPLIED && prepared.property() != null;
	}

	private void showPreview(int generation, OutfitPreview.PreviewModel model) {
		showPreview(generation, new OutfitPreview.PreviewScene(
			configuration.previewMode(),
			configuration.previewStage(),
			model,
			model,
			model,
			0,
			1,
			0
		));
	}

	private void showPreview(int generation, OutfitPreview.PreviewScene scene) {
		preview.show(player, scene)
			.whenComplete((ignored, failure) -> {
				if (stopping || generation != previewGeneration) return;
				finishPreviewTransition(generation);
				if (failure == null) return;
				logger.log(Level.WARNING, "Failed to show onboarding preview for " + player.getUniqueId(), failure);
				messages.send(player, MessageDefaults.ONBOARDING_PREVIEW_FAILED);
			});
	}

	private void finishPreviewTransition(int generation) {
		if (generation == previewGeneration) previewTransitioning = false;
	}

	private void confirm() {
		OnboardingOutfitSelection selection = selectedOutfit();
		messages.send(player, MessageDefaults.ONBOARDING_APPLYING, args -> args.plain("outfit", selectionName(selection)));
		if (selection.original()) {
			outfits.applyOriginal(player, result -> confirm(selection, result));
			return;
		}
		outfits.applyOutfit(player, selection.outfitId(), result -> confirm(selection, result));
	}

	private void confirm(OnboardingOutfitSelection selection, OutfitResult result) {
		if (!confirmed(selection, result)) {
			messages.send(player, MessageDefaults.ONBOARDING_APPLY_FAILED);
			return;
		}
		try {
			storage.complete(player.getUniqueId(), selection.storageValue(), System.currentTimeMillis());
		} catch (SQLException exception) {
			logger.log(Level.WARNING, "Failed to save onboarding completion for " + player.getUniqueId(), exception);
			messages.send(player, MessageDefaults.ONBOARDING_APPLY_FAILED);
			return;
		}
		messages.send(player, MessageDefaults.ONBOARDING_COMPLETED);
		stop(true);
	}

	private boolean confirmed(OnboardingOutfitSelection selection, OutfitResult result) {
		return selection.original() ? result == OutfitResult.ORIGINAL : result == OutfitResult.APPLIED;
	}

	private void cancel() {
		messages.send(player, MessageDefaults.ONBOARDING_CANCELLED);
		stop(true);
	}

	private OnboardingOutfitSelection selectedOutfit() {
		List<OnboardingOutfitSelection> configured = configuration.outfits();
		return configured.get(Math.max(0, Math.min(outfitIndex, configured.size() - 1)));
	}

	private String selectionName(OnboardingOutfitSelection selection) {
		if (selection.original()) return "Original Skin";
		return outfitConfiguration.current().find(selection.outfitId())
			.map(OutfitDefinition::displayName)
			.orElse(selection.outfitId().value());
	}
}
