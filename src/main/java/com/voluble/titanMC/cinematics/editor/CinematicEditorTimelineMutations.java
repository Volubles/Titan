package com.voluble.titanMC.cinematics.editor;

import com.voluble.titanMC.cinematics.model.CameraPoint;
import com.voluble.titanMC.cinematics.model.CinematicEvent;
import com.voluble.titanMC.util.ChatUtils;
import io.voluble.michellelib.menu.item.MenuActions;
import org.bukkit.entity.Player;

final class CinematicEditorTimelineMutations {
	private CinematicEditorTimelineMutations() {
	}

	static void moveCameraPointSlot(
		Player player,
		CinematicEditorService editor,
		CameraPoint point,
		int deltaSlots,
		MenuActions actions
	) {
		editor.moveCameraPointSlot(player, point, deltaSlots)
			.ifPresentOrElse(
				updated -> actions.transition(() -> editor.openCameraOptions(player, updated)),
				() -> {
					player.sendMessage(ChatUtils.format("<#d43030>That canvas slot is not available for this camera point."));
					actions.transition(() -> editor.openCameraOptions(player, point));
				}
			);
	}

	static void moveEventSlot(
		Player player,
		CinematicEditorService editor,
		CinematicEvent event,
		int deltaSlots,
		MenuActions actions
	) {
		editor.moveEventSlot(player, event, deltaSlots)
			.ifPresentOrElse(
				updated -> actions.transition(() -> editor.openEventOptions(player, updated)),
				() -> {
					player.sendMessage(ChatUtils.format("<#d43030>That canvas slot and row are not available for this event."));
					actions.transition(() -> editor.openEventOptions(player, event));
				}
			);
	}

	static void shiftTimeline(
		Player player,
		CinematicEditorService editor,
		int startSlot,
		int deltaSlots,
		MenuActions actions
	) {
		if (!editor.shiftTimeline(player, startSlot, deltaSlots)) {
			player.sendMessage(ChatUtils.format("<#d43030>Timeline shift would create a collision or a negative canvas slot."));
		}
		actions.transition(() -> editor.openTimeline(player));
	}
}
