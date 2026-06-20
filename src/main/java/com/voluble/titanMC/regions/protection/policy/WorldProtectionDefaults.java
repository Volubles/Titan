package com.voluble.titanMC.regions.protection.policy;

import com.voluble.titanMC.regions.model.WorldId;
import com.voluble.titanMC.regions.protection.model.ProtectionAction;
import com.voluble.titanMC.regions.protection.model.ProtectionDecision;
import com.voluble.titanMC.regions.protection.model.ProtectionRequest;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class WorldProtectionDefaults implements ProtectionDefaults {

	private final ProtectionDecision fallback;
	private final Map<WorldId, WorldRules> worlds;

	private WorldProtectionDefaults(ProtectionDecision fallback, Map<WorldId, WorldRules> worlds) {
		this.fallback = requireExplicit(fallback, "fallback");
		this.worlds = Map.copyOf(worlds);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ProtectionDecision decide(ProtectionRequest request) {
		WorldRules rules = worlds.get(request.target().worldId());
		if (rules == null) return fallback;
		return rules.actions.getOrDefault(request.action(), rules.defaultDecision);
	}

	private static ProtectionDecision requireExplicit(ProtectionDecision decision, String label) {
		Objects.requireNonNull(decision, label);
		if (!decision.explicit()) throw new IllegalArgumentException(label + " must be ALLOW or DENY");
		return decision;
	}

	public static final class Builder {
		private ProtectionDecision fallback = ProtectionDecision.ALLOW;
		private final Map<WorldId, MutableWorldRules> worlds = new LinkedHashMap<>();

		public Builder fallback(ProtectionDecision decision) {
			this.fallback = requireExplicit(decision, "fallback");
			return this;
		}

		public Builder worldDefault(WorldId worldId, ProtectionDecision decision) {
			worlds.computeIfAbsent(Objects.requireNonNull(worldId, "worldId"), ignored -> new MutableWorldRules())
				.defaultDecision = requireExplicit(decision, "world default");
			return this;
		}

		public Builder action(WorldId worldId, ProtectionAction action, ProtectionDecision decision) {
			MutableWorldRules rules = worlds.computeIfAbsent(Objects.requireNonNull(worldId, "worldId"), ignored -> new MutableWorldRules());
			rules.actions.put(Objects.requireNonNull(action, "action"), requireExplicit(decision, "action decision"));
			return this;
		}

		public WorldProtectionDefaults build() {
			Map<WorldId, WorldRules> built = new LinkedHashMap<>();
			for (Map.Entry<WorldId, MutableWorldRules> entry : worlds.entrySet()) {
				built.put(entry.getKey(), new WorldRules(entry.getValue().defaultDecision, entry.getValue().actions));
			}
			return new WorldProtectionDefaults(fallback, built);
		}
	}

	private static final class MutableWorldRules {
		private ProtectionDecision defaultDecision = ProtectionDecision.DENY;
		private final EnumMap<ProtectionAction, ProtectionDecision> actions = new EnumMap<>(ProtectionAction.class);
	}

	private static final class WorldRules {
		private final ProtectionDecision defaultDecision;
		private final Map<ProtectionAction, ProtectionDecision> actions;

		private WorldRules(ProtectionDecision defaultDecision, Map<ProtectionAction, ProtectionDecision> actions) {
			this.defaultDecision = requireExplicit(defaultDecision, "world default");
			this.actions = Map.copyOf(actions);
		}
	}
}
