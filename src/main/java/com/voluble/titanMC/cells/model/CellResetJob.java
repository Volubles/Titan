package com.voluble.titanMC.cells.model;

import java.util.Objects;
import java.util.UUID;

public record CellResetJob(
	String cellId,
	long leaseGeneration,
	UUID ownerId,
	Phase phase,
	Long recoveryLotId,
	int attempts,
	long nextAttemptAt,
	String lastError
) {
	public CellResetJob {
		Objects.requireNonNull(cellId, "cellId");
		Objects.requireNonNull(ownerId, "ownerId");
		Objects.requireNonNull(phase, "phase");
		if (leaseGeneration < 1) throw new IllegalArgumentException("generation must be positive");
		if (phase == Phase.PREPARED && recoveryLotId == null) {
			throw new IllegalArgumentException("prepared reset requires recovery lot");
		}
		if (attempts < 0) throw new IllegalArgumentException("attempts must not be negative");
	}

	public CellResetJob(String cellId, long leaseGeneration, UUID ownerId, Phase phase, Long recoveryLotId) {
		this(cellId, leaseGeneration, ownerId, phase, recoveryLotId, 0, 0, null);
	}

	public CellResetJob prepared(long lotId) {
		return new CellResetJob(cellId, leaseGeneration, ownerId, Phase.PREPARED, lotId, 0, 0, null);
	}

	public CellResetJob failed(long nextAttemptAt, String error) {
		return new CellResetJob(
			cellId,
			leaseGeneration,
			ownerId,
			phase,
			recoveryLotId,
			attempts + 1,
			nextAttemptAt,
			Objects.requireNonNull(error, "error")
		);
	}

	public boolean ready(long now) {
		return nextAttemptAt <= now;
	}

	public enum Phase {
		COLLECTING,
		PREPARED
	}
}
