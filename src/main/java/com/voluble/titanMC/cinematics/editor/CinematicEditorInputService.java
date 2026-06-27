package com.voluble.titanMC.cinematics.editor;

import com.voluble.titanMC.util.ChatUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class CinematicEditorInputService implements Listener {
	private final Plugin plugin;
	private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();

	public CinematicEditorInputService(Plugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	void prompt(Player player, String prompt, Consumer<String> accepted, Runnable cancelled) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(accepted, "accepted");
		Objects.requireNonNull(cancelled, "cancelled");
		pending.put(player.getUniqueId(), new PendingInput(accepted, cancelled));
		player.sendMessage(ChatUtils.format("<#30bbf1>" + prompt));
		player.sendMessage(ChatUtils.format("<gray>Type <white>cancel</white> to cancel."));
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onChat(AsyncChatEvent event) {
		Player player = event.getPlayer();
		PendingInput input = pending.remove(player.getUniqueId());
		if (input == null) return;
		event.setCancelled(true);
		String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
		plugin.getServer().getScheduler().runTask(plugin, () -> {
			if (!player.isOnline()) return;
			if (message.equalsIgnoreCase("cancel")) {
				input.cancelled().run();
				return;
			}
			input.accepted().accept(message);
		});
	}

	private record PendingInput(Consumer<String> accepted, Runnable cancelled) {
	}
}
