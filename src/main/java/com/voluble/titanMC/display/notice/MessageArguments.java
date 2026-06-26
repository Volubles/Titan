package com.voluble.titanMC.display.notice;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class MessageArguments {
	private final Map<String, Component> values = new LinkedHashMap<>();

	public MessageArguments plain(String key, Object value) {
		values.put(normalizeKey(key), Component.text(Objects.toString(value, "")));
		return this;
	}

	public MessageArguments component(String key, Component value) {
		values.put(normalizeKey(key), Objects.requireNonNull(value, "value"));
		return this;
	}

	TagResolver resolver(MessageCatalog catalog) {
		Objects.requireNonNull(catalog, "catalog");
		TagResolver.Builder builder = TagResolver.builder();
		for (var entry : values.entrySet()) {
			builder.resolver(Placeholder.component(entry.getKey(), entry.getValue()));
		}
		builder.tag("glyph", (argumentQueue, context) -> {
			String name = argumentQueue.popOr("glyph name expected").value();
			return net.kyori.adventure.text.minimessage.tag.Tag.inserting(Component.text(catalog.glyph(name)));
		});
		return builder.build();
	}

	private static String normalizeKey(String key) {
		String normalized = Objects.requireNonNull(key, "key").trim();
		if (normalized.isEmpty()) throw new IllegalArgumentException("argument key must not be blank");
		return normalized;
	}
}
