package com.voluble.titanMC.mines.reset;

public record MineResetWork(
	int scannedBlocks,
	int changedBlocks,
	boolean finished
) {
	public MineResetWork {
		if (scannedBlocks < 0) throw new IllegalArgumentException("scannedBlocks must not be negative");
		if (changedBlocks < 0 || changedBlocks > scannedBlocks) {
			throw new IllegalArgumentException("changedBlocks must be between zero and scannedBlocks");
		}
	}
}
