package com.voluble.titanMC.cinematics.editor;

import io.voluble.michellelib.menu.item.ClickContext;

final class CinematicEditorClickSteps {
	private CinematicEditorClickSteps() {
	}

	static int signedTickDelta(ClickContext context) {
		int amount = context.shiftClick() ? 20 : 1;
		return context.click().isLeftClick() ? -amount : amount;
	}

	static java.util.List<String> tickControlLore(String target) {
		return java.util.List.of(
			"<gray>Move " + target + " on the timeline.",
			"",
			"<gray>Left click: <white>-1 tick",
			"<gray>Right click: <white>+1 tick",
			"<gray>Shift left: <white>-20 ticks",
			"<gray>Shift right: <white>+20 ticks"
		);
	}
}
