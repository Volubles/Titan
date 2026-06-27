package com.voluble.titanMC.cinematics.editor;

record CinematicTimelineViewport(int startTick, int startRow) {
	static final int COLUMNS = 9;
	static final int ROWS = 5;

	int slot(int visibleRow, int column) {
		return visibleRow * COLUMNS + column;
	}

	int tick(int column) {
		return startTick + column;
	}

	int row(int visibleRow) {
		return startRow + visibleRow;
	}
}
