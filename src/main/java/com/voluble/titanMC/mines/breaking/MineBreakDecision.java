package com.voluble.titanMC.mines.breaking;

public enum MineBreakDecision {
	OUTSIDE_MINE(true),
	BYPASS(true),
	ALLOWED(true),
	MATERIAL_DENIED(false);

	private final boolean allowed;

	MineBreakDecision(boolean allowed) {
		this.allowed = allowed;
	}

	public boolean allowed() {
		return allowed;
	}
}
