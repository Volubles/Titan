package com.voluble.titanMC.regions.protection.policy;

import com.voluble.titanMC.regions.protection.model.ProtectionRequest;

@FunctionalInterface
public interface ProtectionBypass {

	boolean bypasses(ProtectionRequest request);

	static ProtectionBypass permission(String permission) {
		return request -> request.actor().hasPermission(permission);
	}

	static ProtectionBypass none() {
		return request -> false;
	}
}
