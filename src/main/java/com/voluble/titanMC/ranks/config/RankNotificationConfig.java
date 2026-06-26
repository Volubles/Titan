package com.voluble.titanMC.ranks.config;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RankNotificationConfig(RankNotificationEvent rankup, RankNotificationEvent wardEntry) {
	public RankNotificationConfig {
		Objects.requireNonNull(rankup, "rankup");
		Objects.requireNonNull(wardEntry, "wardEntry");
	}

	public static RankNotificationConfig defaults() {
		return new RankNotificationConfig(
			new RankNotificationEvent(
				true,
				new RankNotificationMessage(true, true, List.of(
					"",
					"<gold><bold>RANK UP</bold></gold>",
					"<gray>You advanced to <yellow>{rank}</yellow><gray>.",
					""
				)),
				new RankNotificationMessage(true, true, List.of(
					"",
					"<gold>{player}</gold> <gray>ranked up to <yellow>{rank}</yellow><gray>.",
					""
				)),
				Optional.of("entity.player.levelup"),
				Optional.of("entity.experience_orb.pickup")
			),
			new RankNotificationEvent(
				true,
				new RankNotificationMessage(true, true, List.of(
					"",
					"<gradient:#f7d774:#f09c2e><bold>NEW WARD UNLOCKED</bold></gradient>",
					"<gray>You entered <yellow>{ward}</yellow><gray>.",
					""
				)),
				new RankNotificationMessage(true, true, List.of(
					"",
					"<gradient:#f7d774:#f09c2e><bold>NEW WARD UNLOCKED</bold></gradient>",
					"<gold>{player}</gold> <gray>entered <yellow>{ward}</yellow><gray>.",
					""
				)),
				Optional.of("ui.toast.challenge_complete"),
				Optional.of("ui.toast.challenge_complete")
			)
		);
	}
}
