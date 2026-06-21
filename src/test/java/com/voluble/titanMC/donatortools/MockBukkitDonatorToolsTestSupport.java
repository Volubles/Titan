package com.voluble.titanMC.donatortools;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

public abstract class MockBukkitDonatorToolsTestSupport {

	protected ServerMock server;

	@BeforeEach
	void startServer() {
		server = MockBukkit.mock();
	}

	@AfterEach
	void stopServer() {
		MockBukkit.unmock();
	}
}
