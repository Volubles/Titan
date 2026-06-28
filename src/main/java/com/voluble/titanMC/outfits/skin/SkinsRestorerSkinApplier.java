package com.voluble.titanMC.outfits.skin;

import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.MojangSkinDataResult;
import net.skinsrestorer.api.property.SkinProperty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class SkinsRestorerSkinApplier implements SkinApplier {
	@Override
	public boolean available() {
		return Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer");
	}

	@Override
	public void apply(Player player, SkinPropertyData property) throws SkinApplyException {
		try {
			api().getSkinApplier(Player.class).applySkin(player, SkinProperty.of(property.value(), property.signature()));
		} catch (Exception exception) {
			throw new SkinApplyException("SkinsRestorer could not apply the outfit skin", exception);
		}
	}

	@Override
	public void applyOriginal(Player player) throws SkinApplyException {
		try {
			api().getSkinApplier(Player.class).applySkin(player);
		} catch (Exception exception) {
			throw new SkinApplyException("SkinsRestorer could not restore the original skin", exception);
		}
	}

	@Override
	public void clearOriginalAssignment(Player player) throws SkinApplyException {
		try {
			api().getPlayerStorage().removeSkinIdOfPlayer(player.getUniqueId());
		} catch (Exception exception) {
			throw new SkinApplyException("SkinsRestorer could not clear the stored skin assignment", exception);
		}
	}

	@Override
	public Optional<SkinPropertyData> resolveOriginal(String playerName) throws SkinApplyException {
		try {
			return api().getMojangAPI().getSkin(playerName)
				.map(MojangSkinDataResult::getSkinProperty)
				.map(property -> new SkinPropertyData(property.getValue(), property.getSignature()));
		} catch (Exception exception) {
			throw new SkinApplyException("SkinsRestorer could not resolve the original skin", exception);
		}
	}

	private SkinsRestorer api() throws SkinApplyException {
		if (!available()) throw new SkinApplyException("SkinsRestorer is not installed or enabled");
		try {
			return SkinsRestorerProvider.get();
		} catch (RuntimeException exception) {
			throw new SkinApplyException("SkinsRestorer API is not available", exception);
		}
	}
}
