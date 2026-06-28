package com.voluble.titanMC.onboarding.presentation;

import com.voluble.titanMC.onboarding.config.OnboardingPresentationConfiguration;
import com.voluble.titanMC.onboarding.config.OnboardingPresentationStep;
import com.voluble.titanMC.onboarding.config.OnboardingSoundCue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class OnboardingPresentationRunner {
	private static final Title.Times TITLE_TIMES = Title.Times.times(
		Duration.ZERO,
		Duration.ofMillis(1500),
		Duration.ZERO
	);

	private final Plugin plugin;
	private final MiniMessage miniMessage = MiniMessage.miniMessage();

	public OnboardingPresentationRunner(Plugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	public OnboardingPresentationPlayback play(Player player, OnboardingPresentationConfiguration configuration) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(configuration, "configuration");
		if (!configuration.enabled()) {
			return completed();
		}
		Playback playback = new Playback(player, configuration);
		playback.start();
		return new OnboardingPresentationPlayback(playback.completion, playback::cancel);
	}

	public void playSound(Player player, OnboardingSoundCue sound) {
		if (!sound.enabled() || !player.isOnline()) return;
		player.playSound(player.getLocation(), sound.key(), sound.soundCategory(), sound.volume(), sound.pitch());
	}

	private OnboardingPresentationPlayback completed() {
		return new OnboardingPresentationPlayback(CompletableFuture.completedFuture(null), () -> {
		});
	}

	private final class Playback implements Runnable {
		private final Player player;
		private final OnboardingPresentationConfiguration configuration;
		private final List<OnboardingPresentationStep> steps;
		private final CompletableFuture<Void> completion = new CompletableFuture<>();
		private BukkitTask task;
		private int stepIndex;
		private long stepTick;
		private int previousVisibleCharacters;

		private Playback(Player player, OnboardingPresentationConfiguration configuration) {
			this.player = player;
			this.configuration = configuration;
			this.steps = configuration.steps();
		}

		private void start() {
			task = plugin.getServer().getScheduler().runTaskTimer(plugin, this, 0L, 1L);
		}

		@Override
		public void run() {
			if (!player.isOnline()) {
				complete();
				return;
			}
			if (stepIndex >= steps.size()) {
				playSound(player, configuration.completeSound());
				complete();
				return;
			}
			OnboardingPresentationStep step = steps.get(stepIndex);
			show(step);
			stepTick++;
			if (stepTick >= step.typewriter().totalTicks() + step.holdTicks()) {
				stepIndex++;
				stepTick = 0L;
				previousVisibleCharacters = 0;
			}
		}

		private void show(OnboardingPresentationStep step) {
			int titleLength = length(step.title().text());
			int subtitleLength = length(step.subtitle().text());
			int totalLength = titleLength + subtitleLength;
			int visible = visibleCharacters(totalLength, step.typewriter().totalTicks());
			if (visible > previousVisibleCharacters && revealedNonWhitespace(step, previousVisibleCharacters, visible)) {
				playSound(player, step.typewriter().sound());
			}
			previousVisibleCharacters = visible;

			int titleVisible = Math.min(titleLength, visible);
			int subtitleVisible = Math.max(0, Math.min(subtitleLength, visible - titleLength));
			Component title = render(step.title().render(left(step.title().text(), titleVisible)));
			Component subtitle = render(step.subtitle().render(left(step.subtitle().text(), subtitleVisible)));
			player.showTitle(Title.title(title, subtitle, TITLE_TIMES));
		}

		private int visibleCharacters(int totalLength, long totalTicks) {
			if (stepTick >= totalTicks) return totalLength;
			return Math.max(1, (int) Math.ceil(((stepTick + 1.0D) / totalTicks) * totalLength));
		}

		private boolean revealedNonWhitespace(OnboardingPresentationStep step, int previous, int visible) {
			String combined = step.title().text() + step.subtitle().text();
			for (int index = previous; index < visible && index < length(combined); index++) {
				int offset = combined.offsetByCodePoints(0, index);
				if (!Character.isWhitespace(combined.codePointAt(offset))) return true;
			}
			return false;
		}

		private Component render(String miniMessageText) {
			return miniMessage.deserialize(miniMessageText);
		}

		private void cancel() {
			if (task != null) task.cancel();
			completion.cancel(false);
		}

		private void complete() {
			if (task != null) task.cancel();
			completion.complete(null);
		}
	}

	private static int length(String text) {
		return text.codePointCount(0, text.length());
	}

	private static String left(String text, int codePoints) {
		if (codePoints <= 0) return "";
		if (codePoints >= length(text)) return text;
		return text.substring(0, text.offsetByCodePoints(0, codePoints));
	}
}
