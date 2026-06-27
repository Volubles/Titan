package com.voluble.titanMC.cinematics.runtime;

import com.voluble.titanMC.cinematics.model.CinematicEvent;
import com.voluble.titanMC.cinematics.model.CommandCinematicEvent;
import com.voluble.titanMC.cinematics.model.ParticleCinematicEvent;
import com.voluble.titanMC.cinematics.model.SoundCinematicEvent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Objects;

final class CinematicEventExecutor {
	private final Plugin plugin;

	CinematicEventExecutor(Plugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	void execute(Player player, CinematicEvent event) {
		try {
			switch (event) {
				case CommandCinematicEvent command -> command(player, command);
				case ParticleCinematicEvent particle -> particle(player, particle);
				case SoundCinematicEvent sound -> sound(player, sound);
			}
		} catch (Exception exception) {
			plugin.getLogger().warning("Failed to execute cinematic event " + event.type() + " at tick " + event.tick() + ": " + exception.getMessage());
		}
	}

	private void command(Player player, CommandCinematicEvent event) {
		String command = event.command().replace("{player}", player.getName());
		if (command.startsWith("/")) command = command.substring(1);
		CommandSender sender = event.console() ? Bukkit.getConsoleSender() : player;
		Bukkit.dispatchCommand(sender, command);
	}

	private void particle(Player player, ParticleCinematicEvent event) {
		Particle particle = Particle.valueOf(event.particle().trim().toUpperCase(Locale.ROOT));
		player.spawnParticle(
			particle,
			event.position().toLocation(),
			event.count(),
			event.offsetX(),
			event.offsetY(),
			event.offsetZ(),
			event.speed()
		);
	}

	private void sound(Player player, SoundCinematicEvent event) {
		SoundCategory category = SoundCategory.valueOf(event.category().trim().replace('-', '_').toUpperCase(Locale.ROOT));
		player.playSound(event.position().toLocation(), event.key(), category, event.volume(), event.pitch());
	}
}
