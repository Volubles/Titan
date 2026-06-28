package com.voluble.titanMC.display.screen;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

public final class ScreenEffectOverlayResolver {
	private final Plugin plugin;
	private final MiniMessage miniMessage = MiniMessage.miniMessage();

	public ScreenEffectOverlayResolver(Plugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	public Component resolve(ScreenEffectDefinition definition) {
		if (definition.overlay().isPresent()) {
			return miniMessage.deserialize(definition.overlay().get()).color(definition.color());
		}
		ScreenEffectOverlay overlay = resolveNexo(definition.nexoGlyph().orElseThrow(), definition.id());
		return Component.text(String.valueOf(overlay.character()))
			.font(overlay.font())
			.color(definition.color());
	}

	private ScreenEffectOverlay resolveNexo(String glyphId, ScreenEffectId screenId) {
		try {
			Class<?> pluginClass = Class.forName("com.nexomc.nexo.NexoPlugin");
			Method instance = pluginClass.getMethod("instance");
			Object nexo = instance.invoke(null);
			Object fontManager = pluginClass.getMethod("fontManager").invoke(nexo);
			Object glyph = fontManager.getClass().getMethod("glyphFromID", String.class).invoke(fontManager, glyphId);
			if (glyph == null) throw new IllegalStateException("Unknown Nexo glyph: " + glyphId);
			Object chars = glyph.getClass().getMethod("getChars").invoke(glyph);
			char character = firstChar(chars);
			Object font = glyph.getClass().getMethod("getFont").invoke(glyph);
			if (!(font instanceof Key key)) throw new IllegalStateException("Nexo glyph has unsupported font key: " + glyphId);
			return new ScreenEffectOverlay(character, key);
		} catch (ReflectiveOperationException | RuntimeException exception) {
			plugin.getLogger().warning(
				"Could not resolve screen effect glyph '" + glyphId + "' for " + screenId.value() + ": " + exception.getMessage()
			);
			throw new IllegalStateException("Could not resolve screen effect glyph: " + glyphId, exception);
		}
	}

	private char firstChar(Object chars) {
		if (chars instanceof char[] array && array.length > 0) return array[0];
		if (chars instanceof Character[] array && array.length > 0) return array[0];
		if (chars instanceof String value && !value.isEmpty()) return value.charAt(0);
		if (chars instanceof java.util.List<?> list && !list.isEmpty()) {
			Object first = list.get(0);
			if (first instanceof Character character) return character;
			if (first instanceof String value && !value.isEmpty()) return value.charAt(0);
		}
		throw new IllegalStateException("glyph does not contain any characters");
	}
}
