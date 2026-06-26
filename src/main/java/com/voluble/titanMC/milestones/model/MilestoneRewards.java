package com.voluble.titanMC.milestones.model;

public record MilestoneRewards(long cred, long money) {
	public static final MilestoneRewards NONE = new MilestoneRewards(0L, 0L);

	public MilestoneRewards {
		if (cred < 0) throw new IllegalArgumentException("cred reward must not be negative");
		if (money < 0) throw new IllegalArgumentException("money reward must not be negative");
	}

	public boolean empty() {
		return cred == 0L && money == 0L;
	}
}
