package com.voluble.titanMC.onboarding.preview.actor;

import com.voluble.titanMC.onboarding.preview.OutfitPreview;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class PreviewActorController {
	private final Plugin plugin;
	private final Player player;
	private final PreviewMotion motion;
	private final Set<PreviewActor> actors = new LinkedHashSet<>();
	private PreviewActor active;

	public PreviewActorController(Plugin plugin, Player player, PreviewMotion motion) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.player = Objects.requireNonNull(player, "player");
		this.motion = Objects.requireNonNull(motion, "motion");
	}

	public CompletableFuture<Void> show(OutfitPreview.PreviewModel model) {
		Objects.requireNonNull(model, "model");
		PreviewPath path = PreviewPath.from(model.stage());
		if (active != null) {
			PreviewActor outgoing = active;
			outgoing.exit().whenComplete((ignored, failure) -> actors.remove(outgoing));
		}
		PreviewActor next = new PreviewActor(plugin, player, model.name(), path, model.skin(), motion);
		active = next;
		actors.add(next);
		return next.enter();
	}

	public void remove() {
		for (PreviewActor actor : Set.copyOf(actors)) {
			actor.remove();
		}
		actors.clear();
		active = null;
	}
}
