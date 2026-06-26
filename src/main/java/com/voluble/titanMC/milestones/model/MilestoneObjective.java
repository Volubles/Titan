package com.voluble.titanMC.milestones.model;

import java.util.Objects;

public record MilestoneObjective(MilestoneMetric metric, String subject, long target) {
	public MilestoneObjective {
		Objects.requireNonNull(metric, "metric");
		subject = subject == null ? "" : subject.trim().toLowerCase(java.util.Locale.ROOT);
		if (target <= 0) throw new IllegalArgumentException("objective target must be positive");
	}

	public boolean matches(MilestoneProgressKey key) {
		return metric == key.metric() && subject.equals(key.subject());
	}
}
