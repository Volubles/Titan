package com.voluble.titanMC.ranks.event;

import com.voluble.titanMC.ranks.model.PlayerRank;
import com.voluble.titanMC.ranks.model.RankId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerRankChangedEventTest {
	@Test
	void firstAssignmentHasNoPrevious() {
		UUID player = UUID.randomUUID();
		PlayerRank current = new PlayerRank(player, RankId.of("e4"), 1_000L);

		PlayerRankChangedEvent event = new PlayerRankChangedEvent(player, null, current);

		assertTrue(event.previous().isEmpty());
		assertEquals(current, event.current());
		assertEquals(player, event.playerId());
	}

	@Test
	void rankupCarriesBothRanks() {
		UUID player = UUID.randomUUID();
		PlayerRank before = new PlayerRank(player, RankId.of("e4"), 1_000L);
		PlayerRank after = new PlayerRank(player, RankId.of("e3"), 2_000L);

		PlayerRankChangedEvent event = new PlayerRankChangedEvent(player, before, after);

		assertEquals(before, event.previous().orElseThrow());
		assertEquals(after, event.current());
	}

	@Test
	void rejectsMismatchedPlayerIds() {
		UUID player = UUID.randomUUID();
		PlayerRank other = new PlayerRank(UUID.randomUUID(), RankId.of("e4"), 0L);

		assertThrows(IllegalArgumentException.class, () -> new PlayerRankChangedEvent(player, null, other));
	}
}
