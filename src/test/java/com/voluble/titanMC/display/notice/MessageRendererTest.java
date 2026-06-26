package com.voluble.titanMC.display.notice;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageRendererTest {
	private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

	@Test
	void rendersMessageThroughConfiguredTemplate() {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("templates.success", "<green>[OK] {{message}}</green>");
		yaml.set("messages.success.cells.created", "Created {{cell}}.");
		MessageCatalog catalog = MessageCatalog.load(yaml);
		MessageRenderer renderer = new MessageRenderer(MiniMessage.miniMessage());

		String plain = PLAIN.serialize(renderer.render(
			catalog,
			MessageDefinition.of("cells.created", MessageType.SUCCESS, "Created cell."),
			new MessageArguments().plain("cell", "A1")
		));

		assertEquals("[OK] Created A1.", plain);
	}

	@Test
	void fallsBackToDefinitionAndDefaultTemplateWhenKeyIsMissing() {
		YamlConfiguration yaml = new YamlConfiguration();
		MessageCatalog catalog = MessageCatalog.load(yaml);
		MessageRenderer renderer = new MessageRenderer(MiniMessage.miniMessage());

		String plain = PLAIN.serialize(renderer.render(
			catalog,
			MessageDefinition.of("cells.unknown", MessageType.ERROR, "Unknown cell."),
			new MessageArguments()
		));

		assertEquals("Unknown cell.", plain);
	}

	@Test
	void syncedCatalogLoadsNestedMessageKeys() {
		YamlConfiguration yaml = new YamlConfiguration();
		MessageDefinition definition = MessageDefinition.of("donator-tools.reload.success", MessageType.SUCCESS, "Reloaded.");
		MessageYamlSynchronizer.sync(yaml, List.of(definition));

		MessageCatalog catalog = MessageCatalog.load(yaml);

		assertEquals("Reloaded.", catalog.find(definition).orElseThrow().text());
	}

	@Test
	void escapedMiniMessageUsageTextRendersLiteralAngleBrackets() {
		YamlConfiguration yaml = new YamlConfiguration();
		MessageYamlSynchronizer.sync(yaml, List.of(MessageDefaults.DONATOR_TOOLS_HELP_GIVE));
		MessageCatalog catalog = MessageCatalog.load(yaml);
		MessageRenderer renderer = new MessageRenderer(MiniMessage.miniMessage());

		String plain = PLAIN.serialize(renderer.render(catalog, MessageDefaults.DONATOR_TOOLS_HELP_GIVE));

		assertEquals("/dtools <tool> [player]", plain);
	}
}
