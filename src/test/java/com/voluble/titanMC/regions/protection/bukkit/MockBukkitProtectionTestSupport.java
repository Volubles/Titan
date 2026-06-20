package com.voluble.titanMC.regions.protection.bukkit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

abstract class MockBukkitProtectionTestSupport {

	protected ServerMock server;

	@BeforeEach
	void startMockServer() {
		server = MockBukkit.mock();
	}

	@AfterEach
	void stopMockServer() {
		MockBukkit.unmock();
	}
}
