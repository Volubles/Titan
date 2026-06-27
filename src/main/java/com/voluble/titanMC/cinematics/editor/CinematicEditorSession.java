package com.voluble.titanMC.cinematics.editor;

import com.voluble.titanMC.cinematics.model.CinematicId;

import java.util.Objects;

final class CinematicEditorSession {
	private final CinematicId cinematicId;
	private int viewportSlot;
	private int viewportRow;

	CinematicEditorSession(CinematicId cinematicId) {
		this.cinematicId = Objects.requireNonNull(cinematicId, "cinematicId");
	}

	CinematicId cinematicId() {
		return cinematicId;
	}

	int viewportSlot() {
		return viewportSlot;
	}

	int viewportRow() {
		return viewportRow;
	}

	void moveSlots(int delta) {
		viewportSlot = Math.max(0, viewportSlot + delta);
	}

	void moveRows(int delta) {
		viewportRow = Math.max(0, viewportRow + delta);
	}

	void jumpToSlot(int slot) {
		viewportSlot = Math.max(0, slot);
	}
}
