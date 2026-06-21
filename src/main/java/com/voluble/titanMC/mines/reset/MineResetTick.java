package com.voluble.titanMC.mines.reset;

public record MineResetTick(
	int scannedBlocks,
	int changedBlocks,
	int completedResets
) {
	public MineResetTick {
		if (scannedBlocks < 0 || changedBlocks < 0 || completedResets < 0) {
			throw new IllegalArgumentException("reset tick values must not be negative");
		}
	}
}
