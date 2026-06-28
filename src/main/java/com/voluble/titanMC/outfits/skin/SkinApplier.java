package com.voluble.titanMC.outfits.skin;

import org.bukkit.entity.Player;

import java.util.Optional;

public interface SkinApplier {
	static SkinApplier unavailable() {
		return new SkinApplier() {
			@Override
			public boolean available() {
				return false;
			}

			@Override
			public void apply(Player player, SkinPropertyData property) throws SkinApplyException {
				throw new SkinApplyException("SkinsRestorer is not installed or enabled");
			}

			@Override
			public void applyOriginal(Player player) throws SkinApplyException {
				throw new SkinApplyException("SkinsRestorer is not installed or enabled");
			}

			@Override
			public void clearOriginalAssignment(Player player) throws SkinApplyException {
				throw new SkinApplyException("SkinsRestorer is not installed or enabled");
			}

			@Override
			public Optional<SkinPropertyData> resolveOriginal(String playerName) throws SkinApplyException {
				throw new SkinApplyException("SkinsRestorer is not installed or enabled");
			}
		};
	}

	boolean available();

	void apply(Player player, SkinPropertyData property) throws SkinApplyException;

	void applyOriginal(Player player) throws SkinApplyException;

	void clearOriginalAssignment(Player player) throws SkinApplyException;

	Optional<SkinPropertyData> resolveOriginal(String playerName) throws SkinApplyException;
}
