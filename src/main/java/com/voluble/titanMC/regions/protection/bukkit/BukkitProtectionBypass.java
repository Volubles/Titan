package com.voluble.titanMC.regions.protection.bukkit;

import com.voluble.titanMC.regions.protection.model.ProtectionActor;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;
import com.voluble.titanMC.regions.protection.policy.ProtectionBypass;
import com.voluble.titanMC.regions.protection.policy.ProtectionEvaluationContext;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class BukkitProtectionBypass {

	private BukkitProtectionBypass() {}

	public static ProtectionBypass permission(Server server, String permission) {
		Objects.requireNonNull(server, "server");
		String checkedPermission = Objects.requireNonNull(permission, "permission");
		return new ProtectionBypass() {
			@Override
			public boolean bypasses(ProtectionRequest request) {
				return allowed(server, checkedPermission, request.actor());
			}

			@Override
			public ProtectionBypass openEvaluation(ProtectionEvaluationContext context) {
				boolean bypasses = allowed(server, checkedPermission, context.actor());
				return request -> bypasses;
			}
		};
	}

	private static boolean allowed(Server server, String permission, ProtectionActor actor) {
		if (actor.type() != ProtectionActor.Type.PLAYER) return false;
		Player player = server.getPlayer(actor.playerId());
		return player != null && player.hasPermission(permission);
	}
}
