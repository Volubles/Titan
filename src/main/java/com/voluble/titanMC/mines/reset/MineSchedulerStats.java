package com.voluble.titanMC.mines.reset;

public record MineSchedulerStats(
	int activeResets,
	int scheduledDepletionResets,
	long totalScannedBlocks,
	long totalChangedBlocks,
	long lastResetTickNanos
) {}
