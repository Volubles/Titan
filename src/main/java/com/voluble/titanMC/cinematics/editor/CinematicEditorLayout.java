package com.voluble.titanMC.cinematics.editor;

final class CinematicEditorLayout {
	static final int ROWS = 6;
	static final int FOOTER_START = 45;
	static final int SUMMARY = 45;
	static final int SLOTS_BACK = 46;
	static final int ROW_UP = 47;
	static final int ROW_DOWN = 48;
	static final int PLAY = 49;
	static final int DURATION = 50;
	static final int SLOTS_FORWARD = 52;
	static final int CLOSE = 53;

	private CinematicEditorLayout() {
	}

	static boolean timelineSlot(int slot) {
		return slot >= 0 && slot < FOOTER_START;
	}
}
