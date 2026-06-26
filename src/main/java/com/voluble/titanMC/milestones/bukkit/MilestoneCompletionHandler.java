package com.voluble.titanMC.milestones.bukkit;

import com.voluble.titanMC.display.message.DisplayBroadcastService;
import com.voluble.titanMC.display.message.DisplayLine;
import com.voluble.titanMC.display.message.DisplayMessage;
import com.voluble.titanMC.milestones.config.MilestoneConfigurationManager;
import com.voluble.titanMC.milestones.config.MilestoneNotificationConfig;
import com.voluble.titanMC.milestones.model.MilestoneCompletion;
import com.voluble.titanMC.milestones.model.MilestoneNotificationPolicy;
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

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MilestoneCompletionHandler {
	private static final CredSource CRED_SOURCE = CredSource.of("milestone");

	private final Server server;
	private final MilestoneConfigurationManager configuration;
	private final ProgressionEngine progression;
	private final Economy economy;
	private final DisplayBroadcastService broadcasts;
	private final MiniMessage miniMessage = MiniMessage.miniMessage();
	private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.US);

	public MilestoneCompletionHandler(
		Server server,
		MilestoneConfigurationManager configuration,
		ProgressionEngine progression,
		Economy economy,
		DisplayBroadcastService broadcasts
	) {
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
		MilestoneNotificationConfig.Completion notification = configuration.current().notifications().completion();
		MilestoneNotificationPolicy policy = track.notifications().merge(tier.notifications());
		if (!policy.enabled(notification.enabled())) return;

		Map<String, String> placeholders = placeholders(player, track, tier);
		if (policy.playerMessage(notification.playerMessageEnabled())) {
			broadcasts.send(player, message(notification.playerLines(), placeholders));
		}
		if (policy.sound(true)) notification.sound().ifPresent(sound -> playSound(player, sound));

		MilestoneNotificationConfig.Broadcast broadcast = notification.broadcast();
		if (!policy.broadcast(broadcast.shouldBroadcast(tier.target()))) return;
		if (!broadcast.shouldBroadcast(tier.target())) return;
		broadcasts.broadcast(message(broadcast.lines(), placeholders));
		if (!policy.broadcastSound(true)) return;
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

	private Map<String, String> placeholders(Player player, MilestoneTrack track, MilestoneTier tier) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("player", player.getName());
		values.put("track", track.name());
		values.put("milestone", tier.name());
		values.put("target", numbers.format(tier.target()));
		values.put("rewards", rewards(tier.rewards()));
		return values;
	}

	private String rewards(MilestoneRewards rewards) {
		if (rewards.empty()) return "No reward";
		java.util.List<String> parts = new java.util.ArrayList<>();
		if (rewards.cred() > 0) parts.add(numbers.format(rewards.cred()) + " cred");
		if (rewards.money() > 0) parts.add("$" + numbers.format(rewards.money()));
		return String.join(", ", parts);
	}

	private DisplayMessage message(java.util.List<String> templates, Map<String, String> placeholders) {
		return new DisplayMessage(templates.stream()
			.map(template -> DisplayLine.left(render(template, placeholders)))
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
}
