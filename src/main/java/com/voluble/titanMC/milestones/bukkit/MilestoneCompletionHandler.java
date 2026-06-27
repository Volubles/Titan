package com.voluble.titanMC.milestones.bukkit;

import com.voluble.titanMC.display.message.DisplayBroadcastService;
import com.voluble.titanMC.display.message.DisplayLine;
import com.voluble.titanMC.display.message.DisplayMessage;
import com.voluble.titanMC.milestones.config.MilestoneConfigurationManager;
import com.voluble.titanMC.milestones.config.MilestoneNotificationConfig;
import com.voluble.titanMC.milestones.model.MilestoneCompletion;
import com.voluble.titanMC.milestones.model.MilestoneRewards;
import com.voluble.titanMC.milestones.model.MilestoneTier;
import com.voluble.titanMC.milestones.model.MilestoneTrack;
import com.voluble.titanMC.milestones.service.MilestoneUpdate;
import com.voluble.titanMC.progression.model.CredAmount;
import com.voluble.titanMC.progression.model.CredSource;
import com.voluble.titanMC.progression.service.ProgressionEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Server;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class MilestoneCompletionHandler {
	private static final CredSource CRED_SOURCE = CredSource.of("milestone");
	private static final long NOTIFICATION_DELAY_TICKS = 50L;

	private final Plugin plugin;
	private final Server server;
	private final MilestoneConfigurationManager configuration;
	private final ProgressionEngine progression;
	private final Economy economy;
	private final DisplayBroadcastService broadcasts;
	private final MiniMessage miniMessage = MiniMessage.miniMessage();
	private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.US);

	public MilestoneCompletionHandler(
		Plugin plugin,
		Server server,
		MilestoneConfigurationManager configuration,
		ProgressionEngine progression,
		Economy economy,
		DisplayBroadcastService broadcasts
	) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.server = Objects.requireNonNull(server, "server");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.progression = Objects.requireNonNull(progression, "progression");
		this.economy = economy;
		this.broadcasts = Objects.requireNonNull(broadcasts, "broadcasts");
	}

	public void handle(Player player, MilestoneUpdate update) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(update, "update");
		for (MilestoneCompletion completion : update.completions()) {
			handle(player, completion);
		}
	}

	private void handle(Player player, MilestoneCompletion completion) {
		var catalog = configuration.current().catalog();
		MilestoneTrack track = catalog.trackForTier(completion.tierId()).orElse(null);
		if (track == null) return;
		Optional<MilestoneTier> tier = track.tiers().stream()
			.filter(candidate -> candidate.id().equals(completion.tierId()))
			.findFirst();
		tier.ifPresent(value -> complete(player, track, value));
	}

	private void complete(Player player, MilestoneTrack track, MilestoneTier tier) {
		award(player, tier.rewards());
		scheduleNotification(NotificationContext.of(player, track, tier, rewards(tier.rewards())));
	}

	private void scheduleNotification(NotificationContext context) {
		server.getScheduler().runTaskLater(plugin, () -> notify(context), NOTIFICATION_DELAY_TICKS);
	}

	private void notify(NotificationContext context) {
		MilestoneNotificationConfig.Completion notification = configuration.current().notifications().completion();
		if (!notification.enabled()) return;

		Map<String, String> placeholders = context.placeholders();
		Player player = server.getPlayer(context.playerId());
		if (player != null && notification.playerMessageEnabled()) {
			broadcasts.send(player, message(notification.playerLines(), placeholders, notification.playerMessageCentered()));
		}
		if (player != null) notification.sound().ifPresent(sound -> playSound(player, sound));

		MilestoneNotificationConfig.Broadcast broadcast = notification.broadcast();
		if (!broadcast.shouldBroadcast(context.target())) return;
		broadcasts.broadcast(message(broadcast.lines(), placeholders, broadcast.centered()));
		broadcast.sound().ifPresent(sound -> {
			for (Player online : server.getOnlinePlayers()) playSound(online, sound);
		});
	}

	private void award(Player player, MilestoneRewards rewards) {
		if (rewards.cred() > 0) {
			progression.give(player.getUniqueId(), CredAmount.of(rewards.cred()), CRED_SOURCE);
		}
		if (rewards.money() > 0 && economy != null) {
			economy.depositPlayer(player, rewards.money());
		}
	}

	private String rewards(MilestoneRewards rewards) {
		if (rewards.empty()) return "No reward";
		java.util.List<String> parts = new java.util.ArrayList<>();
		if (rewards.cred() > 0) parts.add(numbers.format(rewards.cred()) + " cred");
		if (rewards.money() > 0) parts.add("$" + numbers.format(rewards.money()));
		return String.join(", ", parts);
	}

	private DisplayMessage message(java.util.List<String> templates, Map<String, String> placeholders, boolean centered) {
		return new DisplayMessage(templates.stream()
			.map(template -> centered
				? DisplayLine.centered(render(template, placeholders))
				: DisplayLine.left(render(template, placeholders)))
			.toList());
	}

	private Component render(String template, Map<String, String> placeholders) {
		String rendered = template;
		for (var entry : placeholders.entrySet()) {
			rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
		}
		return miniMessage.deserialize(rendered);
	}

	private void playSound(Player player, String sound) {
		player.playSound(player.getLocation(), sound, SoundCategory.MASTER, 1.0f, 1.0f);
	}

	private record NotificationContext(
		UUID playerId,
		String playerName,
		String trackName,
		String milestoneName,
		long target,
		String targetText,
		String rewardText
	) {
		private static NotificationContext of(Player player, MilestoneTrack track, MilestoneTier tier, String rewardText) {
			return new NotificationContext(
				player.getUniqueId(),
				player.getName(),
				track.name(),
				tier.name(),
				tier.target(),
				NumberFormat.getIntegerInstance(Locale.US).format(tier.target()),
				rewardText
			);
		}

		private Map<String, String> placeholders() {
			Map<String, String> values = new LinkedHashMap<>();
			values.put("player", playerName);
			values.put("track", trackName);
			values.put("milestone", milestoneName);
			values.put("target", targetText);
			values.put("rewards", rewardText);
			return values;
		}
	}
}
