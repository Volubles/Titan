package com.voluble.titanMC.regions.service;

public enum RegionEngineHealth {
	HEALTHY,
	FAILED,
	CLOSED;

	public boolean acceptsMutations() {
		return this == HEALTHY;
	}
}
