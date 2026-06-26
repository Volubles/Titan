package com.voluble.titanMC.ranks.bukkit;

import com.voluble.titanMC.display.message.DisplayBroadcastService;
import com.voluble.titanMC.ranks.config.RankConfiguration;
import com.voluble.titanMC.ranks.config.RankNotificationEvent;
import com.voluble.titanMC.ranks.event.PlayerRankChangeCause;
import com.voluble.titanMC.ranks.event.PlayerRankChangedEvent;
import com.voluble.titanMC.ranks.model.PrisonRank;
import com.voluble.titanMC.ranks.model.WardDefinition;
import com.voluble.titanMC.ranks.service.RankCatalog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Server;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class RankNotificationService implements Listener {
	private final Server server;
	private final DisplayBroadcastService broadcasts;
	private final Supplier<RankConfiguration> configuration;
	private final MiniMessage miniMessage = MiniMessage.miniMessage();

	public RankNotificationService(
		Server server,
		DisplayBroadcastService broadcasts,
		Supplier<RankConfiguration> configuration
	) {
		this.server = Objects.requireNonNull(server, "server");
		this.broadcasts = Objects.requireNonNull(broadcasts, "broadcasts");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onRankChanged(PlayerRankChangedEvent event) {
		if (event.cause() != PlayerRankChangeCause.RANKUP) return;
		Player player = server.getPlayer(event.playerId());
		if (player == null) return;
		RankConfiguration config = configuration.get();
		RankCatalog catalog = config.catalog();
		PrisonRank previous = event.previous()
			.map(rank -> catalog.requireRank(rank.rankId()))
			.orElse(null);
		if (previous == null) return;
		PrisonRank current = catalog.requireRank(event.current().rankId());
		if (previous.id().equals(current.id())) return;

		WardDefinition previousWard = catalog.requireWard(previous.wardId());
		WardDefinition currentWard = catalog.requireWard(current.wardId());
		boolean changedWard = !previousWard.id().equals(currentWard.id());
		RankNotificationEvent notification = changedWard
			? config.notifications().wardEntry()
			: config.notifications().rankup();
		if (!notification.enabled()) return;

		Map<String, String> placeholders = placeholders(player, previous, current, previousWard, currentWard);
		if (notification.broadcasts()) {
			broadcasts.broadcast(notification.broadcastMessage().render(template -> render(template, placeholders)));
			notification.broadcastSound().ifPresent(sound -> {
				for (Player online : server.getOnlinePlayers()) playSound(online, sound);
			});
		} else if (notification.sendsPlayerMessage()) {
			broadcasts.send(player, notification.playerMessage().render(template -> render(template, placeholders)));
			notification.sound().ifPresent(sound -> playSound(player, sound));
		}
	}

	private Map<String, String> placeholders(
		Player player,
		PrisonRank previous,
		PrisonRank current,
		WardDefinition previousWard,
		WardDefinition currentWard
	) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("player", player.getName());
		values.put("old_rank", previous.displayName());
		values.put("rank", current.displayName());
		values.put("old_rank_id", previous.id().value());
		values.put("rank_id", current.id().value());
		values.put("old_ward", previousWard.displayName());
		values.put("ward", currentWard.displayName());
		values.put("old_ward_id", previousWard.id().value());
		values.put("ward_id", currentWard.id().value());
		return values;
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
