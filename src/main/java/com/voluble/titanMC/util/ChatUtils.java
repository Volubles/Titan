package com.voluble.titanMC.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChatUtils {

	private static final MiniMessage miniMessage = MiniMessage.miniMessage();

	// Streamlined Color Palette - Designed to work harmoniously together
	public static final String PRIMARY_COLOR   = "#FFD700";  // Gold - Main highlights, headers, important text
	public static final String SECONDARY_COLOR = "#4A9EFF";  // Bright Blue - Secondary elements, accents
	public static final String SUCCESS_COLOR   = "#4ADE80";  // Green - Success messages, confirmations
	public static final String ERROR_COLOR    = "#F87171";  // Soft Red - Errors, failures
	public static final String WARNING_COLOR   = "#FBBF24";  // Amber - Warnings, alerts, cooldowns
	public static final String INFO_COLOR      = "#8B5CF6";  // Purple - Info text, help messages
	public static final String MUTED_COLOR     = "#94A3B8";  // Slate Gray - Muted text, descriptions


	/**
	 * Formats a MiniMessage-formatted string.
	 * - If a Player is provided, applies PlaceholderAPI placeholders.
	 * - If no Player is provided, just formats using MiniMessage.
	 *
	 * @param player The player whose placeholders should be replaced (nullable).
	 * @param text   The text with MiniMessage formatting.
	 * @return The formatted Component with placeholders applied (if a player is provided).
	 */
	public static Component format(Player player, String text) {
		// Ensure placeholders are replaced first
		if (player != null && hasPlaceholderAPI()) {
			text = PlaceholderAPI.setPlaceholders(player, text);
		}

		// Strip legacy color codes before passing to MiniMessage
		text = ChatColor.stripColor(text);

		return miniMessage.deserialize(text);
	}

	public static String sanitizeUserInput(Player player, String input) {
		if (input == null || input.isEmpty()) {
			return input;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);

			// If the player does NOT have permission to use legacy color codes, we "escape" &
			if (c == '&' && !player.hasPermission("neochat.format.legacy")) {
				sb.append("&&");
			}
			// If we see a <, check if it's a MiniMessage tag
			else if (c == '<') {
				// Look ahead to see if this is a MiniMessage tag
				int endIndex = input.indexOf('>', i);
				if (endIndex != -1) {
					String potentialTag = input.substring(i, endIndex + 1);
					// If it's a MiniMessage tag and player doesn't have permission, skip it
					if (!player.hasPermission("neochat.format.minimessage")) {
						i = endIndex;
					} else {
						sb.append(c);
					}
				} else {
					sb.append(c);
				}
			}
			else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Overloaded method for formatting MiniMessage text **without a Player** (No PlaceholderAPI).
	 *
	 * @param text The text with MiniMessage formatting.
	 * @return The formatted Component.
	 */
	public static Component format(String text) {
		// Strip legacy color codes before passing to MiniMessage
		text = ChatColor.stripColor(text);

		return miniMessage.deserialize(text);
	}

	/**
	 * Formats multiple MiniMessage-formatted strings and applies PlaceholderAPI if available.
	 *
	 * @param player The player whose placeholders should be replaced (nullable).
	 * @param texts  The texts with MiniMessage formatting.
	 * @return A list of formatted Components with placeholders applied (if a player is provided).
	 */
	public static List<Component> formatList(Player player, String... texts) {
		return Arrays.stream(texts)
				.map(text -> format(player, text)) // Apply PlaceholderAPI and MiniMessage formatting
				.collect(Collectors.toList());
	}

	/**
	 * Formats text with a predefined stock color.
	 *
	 * @param text  The text to colorize.
	 * @param color The color constant (e.g., FAIL_COLOR, APPROVED_COLOR).
	 * @return The formatted Component.
	 */
	public static Component formatWithColor(String text, String color) {
		return format("<color:" + color + ">" + text + "</color>");
	}

	/**
	 * Formats text with a predefined stock color, with PlaceholderAPI support.
	 *
	 * @param player The player whose placeholders should be replaced (nullable).
	 * @param text   The text to colorize.
	 * @param color  The color constant (e.g., FAIL_COLOR, APPROVED_COLOR).
	 * @return The formatted Component.
	 */
	public static Component formatWithColor(Player player, String text, String color) {
		return format(player, "<color:" + color + ">" + text + "</color>");
	}

	/**
	 * Checks if PlaceholderAPI is installed.
	 *
	 * @return true if PlaceholderAPI is installed, otherwise false.
	 */
	private static boolean hasPlaceholderAPI() {
		return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
	}

	/**
	 * Serializes a Component back into a MiniMessage string.
	 *
	 * @param component The Component to serialize.
	 * @return The MiniMessage-formatted string.
	 */
	public static String serialize(Component component) {
		return miniMessage.serialize(component);
	}

	/**
	 * Formats text for item display names and lore, ensuring no inherited styles.
	 * This is specifically for items to prevent italic text by default.
	 * Preserves any MiniMessage formatting while resetting inherited styles.
	 *
	 * @param text The text to format
	 * @return The formatted Component with reset styles
	 */
	public static Component formatItem(String text) {
		return format("<reset><italic:false>" + text);
	}

	/**
	 * Formats text for item display names and lore with PlaceholderAPI support,
	 * ensuring no inherited styles. This is specifically for items to prevent
	 * italic text by default. Preserves any MiniMessage formatting while resetting
	 * inherited styles.
	 *
	 * @param player The player whose placeholders should be replaced
	 * @param text The text to format
	 * @return The formatted Component with reset styles and placeholders replaced
	 */
	public static Component formatItem(Player player, String text) {
		return format(player, "<reset><italic:false>" + text);
	}

	/**
	 * Formats a Component for item display names and lore, ensuring no inherited styles.
	 * This is specifically for items to prevent italic text by default.
	 * Preserves any existing formatting while resetting inherited styles.
	 *
	 * @param component The Component to format
	 * @return The formatted Component with reset styles
	 */
	public static Component formatItem(Component component) {
		return format("<reset><italic:false>" + serialize(component));
	}


	/**
	 * Formats a Component for item display names and lore with PlaceholderAPI support,
	 * ensuring no inherited styles. This is specifically for items to prevent
	 * italic text by default. Preserves any existing formatting while resetting
	 * inherited styles.
	 *
	 * @param player The player whose placeholders should be replaced
	 * @param component The Component to format
	 * @return The formatted Component with reset styles and placeholders replaced
	 */
	public static Component formatItem(Player player, Component component) {
		return format(player, "<reset><italic:false>" + serialize(component));
	}

	/**
	 * Formats a list of strings for item lore, ensuring no inherited styles.
	 * This is specifically for items to prevent italic text by default.
	 *
	 * @param texts The texts to format
	 * @return A list of formatted Components with reset styles
	 */
	public static List<Component> formatItemLore(String... texts) {
		return Arrays.stream(texts)
				.map(ChatUtils::formatItem)
				.collect(Collectors.toList());
	}

	/**
	 * Formats a list of strings for item lore with PlaceholderAPI support,
	 * ensuring no inherited styles. This is specifically for items to prevent
	 * italic text by default.
	 *
	 * @param player The player whose placeholders should be replaced
	 * @param texts The texts to format
	 * @return A list of formatted Components with reset styles and placeholders replaced
	 */
	public static List<Component> formatItemLore(Player player, String... texts) {
		return Arrays.stream(texts)
				.map(text -> formatItem(player, text))
				.collect(Collectors.toList());
	}

	/**
	 * Formats a list of Components for item lore, ensuring no inherited styles.
	 * This is specifically for items to prevent italic text by default.
	 *
	 * @param components The Components to format
	 * @return A list of formatted Components with reset styles
	 */
	public static List<Component> formatItemLore(Component... components) {
		return Arrays.stream(components)
				.map(ChatUtils::formatItem)
				.collect(Collectors.toList());
	}

	/**
	 * Formats a list of Components for item lore with PlaceholderAPI support,
	 * ensuring no inherited styles. This is specifically for items to prevent
	 * italic text by default.
	 *
	 * @param player The player whose placeholders should be replaced
	 * @param components The Components to format
	 * @return A list of formatted Components with reset styles and placeholders replaced
	 */
	public static List<Component> formatItemLore(Player player, Component... components) {
		return Arrays.stream(components)
				.map(component -> formatItem(player, component))
				.collect(Collectors.toList());
	}

	/**
	 * Sends an action bar message to a player.
	 *
	 * @param player The player to send the message to
	 * @param message The MiniMessage-formatted message to send
	 */
	public static void sendActionBar(Player player, String message) {
		if (player == null || !player.isOnline()) return;
		player.sendActionBar(format(player, message));
	}

}
