package com.voluble.titanMC.display.notice;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageRenderer {
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z0-9_.-]+)}}");

	private final MiniMessage miniMessage;

	public MessageRenderer(MiniMessage miniMessage) {
		this.miniMessage = Objects.requireNonNull(miniMessage, "miniMessage");
	}

	public List<Component> render(MessageCatalog catalog, MessageDefinition definition) {
		return render(catalog, definition, new MessageArguments());
	}

	public List<Component> render(MessageCatalog catalog, MessageDefinition definition, MessageArguments arguments) {
		Objects.requireNonNull(catalog, "catalog");
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(arguments, "arguments");
		MessageEntry entry = catalog.find(definition).orElseGet(() ->
			new MessageEntry(definition.type(), definition.key(), definition.defaultLines())
		);
		return entry.lines().stream()
			.map(line -> style(catalog, entry.type(), renderLine(line, arguments), arguments))
			.toList();
	}

	private static Component renderLine(String input, MessageArguments arguments) {
		Matcher matcher = PLACEHOLDER.matcher(input);
		Component result = Component.empty();
		int cursor = 0;
		while (matcher.find()) {
			if (matcher.start() > cursor) {
				result = result.append(Component.text(input.substring(cursor, matcher.start())));
			}
			String key = matcher.group(1);
			result = result.append(arguments.find(key).orElseGet(() -> Component.text(matcher.group())));
			cursor = matcher.end();
		}
		if (cursor < input.length()) {
			result = result.append(Component.text(input.substring(cursor)));
		}
		return result;
	}

	private Component style(MessageCatalog catalog, MessageType type, Component content, MessageArguments arguments) {
		return miniMessage.deserialize(
			normalizePlaceholders(catalog.template(type)),
			Placeholder.component("message", content),
			arguments.resolver()
		);
	}

	private static String normalizePlaceholders(String input) {
		return PLACEHOLDER.matcher(input).replaceAll("<$1>");
	}
}
