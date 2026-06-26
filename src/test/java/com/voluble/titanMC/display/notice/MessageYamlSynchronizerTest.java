package com.voluble.titanMC.display.notice;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageYamlSynchronizerTest {

	@Test
	void syncAddsMissingMessages() {
		YamlConfiguration yaml = new YamlConfiguration();
		MessageDefinition definition = MessageDefinition.of("cells.unknown", MessageType.ERROR, "Unknown cell.");

		assertTrue(MessageYamlSynchronizer.sync(yaml, List.of(definition)));

		assertEquals("<color:#d43030>{{message}}</color>", yaml.getString("templates.error"));
		assertEquals(List.of("Unknown cell."), yaml.getStringList("messages.cells.unknown"));
	}

	@Test
	void syncDoesNotOverwriteExistingValues() {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("templates.error", "<red>Custom {{message}}</red>");
		yaml.set("messages.cells.unknown", List.of("No such cell."));

		MessageDefinition definition = MessageDefinition.of("cells.unknown", MessageType.ERROR, "Unknown cell.");
		MessageYamlSynchronizer.sync(yaml, List.of(definition));

		assertEquals("<red>Custom {{message}}</red>", yaml.getString("templates.error"));
		assertEquals(List.of("No such cell."), yaml.getStringList("messages.cells.unknown"));
	}

	@Test
	void syncReplacesScalarMessagesWithLists() {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("messages.cells.unknown", "No such cell.");

		MessageDefinition definition = MessageDefinition.of("cells.unknown", MessageType.ERROR, "Unknown cell.");
		assertTrue(MessageYamlSynchronizer.sync(yaml, List.of(definition)));

		assertEquals(List.of("Unknown cell."), yaml.getStringList("messages.cells.unknown"));
	}
}
