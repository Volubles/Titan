package com.voluble.titanMC.cinematics.editor;

record CinematicTimelineViewport(int startSlot, int startRow) {
	static final int COLUMNS = 9;
	static final int ROWS = 5;

	int slot(int visibleRow, int column) {
		return visibleRow * COLUMNS + column;
	}

	int timelineSlot(int column) {
		return startSlot + column;
	}

	int row(int visibleRow) {
		return startRow + visibleRow;
	}
}
