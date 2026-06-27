package com.voluble.titanMC.cinematics.editor;

import com.voluble.titanMC.cinematics.model.CinematicId;

import java.util.Objects;

final class CinematicEditorSession {
	private final CinematicId cinematicId;
	private int viewportTick;
	private int viewportRow;

	CinematicEditorSession(CinematicId cinematicId) {
		this.cinematicId = Objects.requireNonNull(cinematicId, "cinematicId");
	}

	CinematicId cinematicId() {
		return cinematicId;
	}

	int viewportTick() {
		return viewportTick;
	}

	int viewportRow() {
		return viewportRow;
	}

	void moveTicks(int delta) {
		viewportTick = Math.max(0, viewportTick + delta);
	}

	void moveRows(int delta) {
		viewportRow = Math.max(0, viewportRow + delta);
	}

	void jumpTo(int tick) {
		viewportTick = Math.max(0, tick);
	}
}
