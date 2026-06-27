package com.voluble.titanMC.cinematics.editor;

import io.voluble.michellelib.menu.item.ClickContext;

final class CinematicEditorClickSteps {
	private CinematicEditorClickSteps() {
	}

	static int signedSlotDelta(ClickContext context) {
		int amount = context.shiftClick() ? 20 : 1;
		return context.click().isLeftClick() ? -amount : amount;
	}

	static java.util.List<String> slotControlLore(String target) {
		return java.util.List.of(
			"<gray>Move " + target + " on the canvas.",
			"",
			"<gray>Left click: <white>-1 slot",
			"<gray>Right click: <white>+1 slot",
			"<gray>Shift left: <white>-20 slots",
			"<gray>Shift right: <white>+20 slots"
		);
	}
}
