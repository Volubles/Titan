package com.voluble.titanMC.display.notice;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.Objects;
import java.util.regex.Pattern;

public final class MessageRenderer {
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z0-9_.-]+)}}");

	private final MiniMessage miniMessage;

	public MessageRenderer(MiniMessage miniMessage) {
		this.miniMessage = Objects.requireNonNull(miniMessage, "miniMessage");
	}

	public Component render(MessageCatalog catalog, MessageDefinition definition) {
		return render(catalog, definition, new MessageArguments());
	}

	public Component render(MessageCatalog catalog, MessageDefinition definition, MessageArguments arguments) {
		Objects.requireNonNull(catalog, "catalog");
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(arguments, "arguments");
		MessageEntry entry = catalog.find(definition).orElseGet(() ->
			new MessageEntry(definition.type(), definition.key(), definition.defaultText())
		);
		Component message = miniMessage.deserialize(
			normalizePlaceholders(entry.text()),
			arguments.resolver(catalog)
		);
		return miniMessage.deserialize(
			normalizePlaceholders(catalog.template(entry.type())),
			Placeholder.component("message", message),
			arguments.resolver(catalog)
		);
	}

	private static String normalizePlaceholders(String input) {
		return PLACEHOLDER.matcher(input).replaceAll("<$1>");
	}
}
