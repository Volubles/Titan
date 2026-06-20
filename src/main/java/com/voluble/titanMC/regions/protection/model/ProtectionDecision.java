package com.voluble.titanMC.regions.protection.model;

public enum ProtectionDecision {
	ALLOW,
	DENY,
	ABSTAIN;

	public boolean explicit() {
		return this != ABSTAIN;
	}
}
