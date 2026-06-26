package com.voluble.titanMC.milestones.model;

import java.util.Objects;

public record MilestoneTier(String id, String name, MilestoneObjective objective, MilestoneRewards rewards, int menuSlot) {
	public MilestoneTier {
		id = requireId(id, "tier id");
		name = requireText(name, "tier name");
		Objects.requireNonNull(objective, "objective");
		rewards = Objects.requireNonNull(rewards, "rewards");
		if (menuSlot < -1) throw new IllegalArgumentException("tier menu slot must be -1 or greater");
	}

	public long target() {
		return objective.target();
	}

	public MilestoneMetric metric() {
		return objective.metric();
	}

	public String subject() {
		return objective.subject();
	}

	private static String requireId(String value, String name) {
		String normalized = Objects.requireNonNull(value, name).trim().toLowerCase(java.util.Locale.ROOT);
		if (normalized.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
		return normalized;
	}

	private static String requireText(String value, String name) {
		String normalized = Objects.requireNonNull(value, name).trim();
		if (normalized.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
		return normalized;
	}
}
