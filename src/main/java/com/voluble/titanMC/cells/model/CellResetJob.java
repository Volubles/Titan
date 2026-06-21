package com.voluble.titanMC.cells.model;

import java.util.Objects;
import java.util.UUID;

public record CellResetJob(String cellId,long leaseGeneration,UUID ownerId,Phase phase,Long recoveryLotId) {
	public CellResetJob { Objects.requireNonNull(cellId); Objects.requireNonNull(ownerId); Objects.requireNonNull(phase); if(leaseGeneration<1)throw new IllegalArgumentException("generation must be positive"); if(phase==Phase.PREPARED&&recoveryLotId==null)throw new IllegalArgumentException("prepared reset requires recovery lot"); }
	public enum Phase { COLLECTING, PREPARED }
}
