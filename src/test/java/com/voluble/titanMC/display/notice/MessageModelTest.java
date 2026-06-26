package com.voluble.titanMC.display.notice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageModelTest {

	@Test
	void keyNormalizesAndValidates() {
		assertEquals("cells.unknown", MessageKey.of(" Cells.Unknown ").value());
		assertThrows(IllegalArgumentException.class, () -> MessageKey.of("bad key"));
		assertThrows(IllegalArgumentException.class, () -> MessageKey.of("-bad"));
	}

	@Test
	void typeParsesConfigKeys() {
		assertEquals(MessageType.SUCCESS, MessageType.parse(" success "));
		assertThrows(IllegalArgumentException.class, () -> MessageType.parse("warning"));
	}
}
