package com.voluble.titanMC.onboarding.preview;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.skins.SkinData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FancyNpcOutfitPreview implements OutfitPreview {
	private final Map<UUID, Npc> previews = new ConcurrentHashMap<>();

	@Override
	public boolean available() {
		return Bukkit.getPluginManager().isPluginEnabled("FancyNpcs");
	}

	@Override
	public void show(Player player, PreviewModel model) throws PreviewException {
		if (!available()) throw new PreviewException("FancyNPCs is not installed or enabled");
		remove(player);
		try {
			String name = "titan_onboarding_" + player.getUniqueId().toString().substring(0, 8);
			Location focus = model.stage().focus().toLocation();
			NpcData data = new NpcData(name, player.getUniqueId(), focus)
				.setDisplayName(model.name())
				.setShowInTab(false)
				.setCollidable(false)
				.setTurnToPlayer(false)
				.setSkinData(new SkinData(
					"titanmc_" + name,
					SkinData.SkinVariant.AUTO,
					model.skin().value(),
					model.skin().signature()
				));
			Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(data);
			npc.setSaveToFile(false);
			npc.create();
			npc.spawn(player);
			previews.put(player.getUniqueId(), npc);
		} catch (Exception exception) {
			throw new PreviewException("Could not show FancyNPCs outfit preview", exception);
		}
	}

	@Override
	public void remove(Player player) {
		Npc npc = previews.remove(player.getUniqueId());
		if (npc == null) return;
		try {
			npc.remove(player);
		} catch (Exception ignored) {
			// Preview cleanup must not break onboarding teardown.
		}
	}
}
