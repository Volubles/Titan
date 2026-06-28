package com.voluble.titanMC.onboarding.preview.actor;

import com.voluble.titanMC.onboarding.preview.OutfitPreview;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

final class PreviewActorFactory {
	private final Plugin plugin;
	private final Player player;
	private final PreviewMotion motion;

	PreviewActorFactory(Plugin plugin, Player player, PreviewMotion motion) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.player = Objects.requireNonNull(player, "player");
		this.motion = Objects.requireNonNull(motion, "motion");
	}

	PreviewActor create(OutfitPreview.PreviewModel model) {
		return new PreviewActor(plugin, player, PreviewPath.runway(model.stage()), model.skin(), motion);
	}
}
