package com.voluble.titanMC.cinematics.editor;

import com.voluble.titanMC.cinematics.model.CameraPoint;
import com.voluble.titanMC.cinematics.model.CinematicEvent;
import com.voluble.titanMC.util.ChatUtils;
import io.voluble.michellelib.menu.item.MenuActions;
import org.bukkit.entity.Player;

final class CinematicEditorTimelineMutations {
	private CinematicEditorTimelineMutations() {
	}

	static void moveCameraPoint(
		Player player,
		CinematicEditorService editor,
		CameraPoint point,
		int deltaTicks,
		MenuActions actions
	) {
		editor.moveCameraPoint(player, point, deltaTicks)
			.ifPresentOrElse(
				updated -> actions.transition(() -> editor.openCameraOptions(player, updated)),
				() -> {
					player.sendMessage(ChatUtils.format("<#d43030>That tick is not available for this camera point."));
					actions.transition(() -> editor.openCameraOptions(player, point));
				}
			);
	}

	static void moveEvent(
		Player player,
		CinematicEditorService editor,
		CinematicEvent event,
		int deltaTicks,
		MenuActions actions
	) {
		editor.moveEvent(player, event, deltaTicks)
			.ifPresentOrElse(
				updated -> actions.transition(() -> editor.openEventOptions(player, updated)),
				() -> {
					player.sendMessage(ChatUtils.format("<#d43030>That tick and row are not available for this event."));
					actions.transition(() -> editor.openEventOptions(player, event));
				}
			);
	}

	static void shiftTimeline(
		Player player,
		CinematicEditorService editor,
		int startTick,
		int deltaTicks,
		MenuActions actions
	) {
		if (!editor.shiftTimeline(player, startTick, deltaTicks)) {
			player.sendMessage(ChatUtils.format("<#d43030>Timeline shift would create a collision or a negative tick."));
		}
		actions.transition(() -> editor.openTimeline(player));
	}
}
