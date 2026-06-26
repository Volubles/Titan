package com.voluble.titanMC.milestones.ui;

import java.util.List;

public final class MilestoneMenuLayout {
	public static final List<Integer> FRAME_SLOTS = List.of(
		0, 1, 2, 3, 5, 6, 7, 8,
		36, 37, 38, 39, 41, 42, 43, 44
	);
	public static final List<Integer> CATEGORY_SLOTS = List.of(11, 13, 15, 21, 23);
	public static final List<Integer> TRACK_SLOTS = List.of(
		10, 11, 12, 13, 14, 15, 16,
		19, 20, 21, 22, 23, 24, 25,
		28, 29, 30, 31, 32, 33, 34
	);
	public static final List<Integer> TIER_SLOTS = List.of(11, 12, 13, 14, 15, 20, 21, 22, 23, 24);
	public static final int PREVIOUS = 45;
	public static final int NEXT = 53;
	public static final int CLOSE_OVERVIEW = 40;
	public static final int BACK_CATEGORY = 49;
	public static final int BACK_TRACK = 49;

	private MilestoneMenuLayout() {
	}
}
