package com.voluble.titanMC.ranks.config;

import com.voluble.titanMC.ranks.model.PrisonRank;
import com.voluble.titanMC.ranks.model.RankId;
import com.voluble.titanMC.ranks.model.WardId;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankConfigurationTest {
	@Test
	void loadsWardsAndRanksInConfiguredOrder() throws Exception {
		RankConfiguration configuration = RankConfiguration.load(yaml("""
			wards:
			  - id: E
			    display-name: E Ward
			    ranks:
			      - id: E4
			      - id: E3
			        cost: 1000
			      - id: E2
			        cost: 2500
			      - id: E1
			        cost: 5000
			  - id: D
			    ranks:
			      - id: D4
			        cost: 10000
			      - id: D3
			        cost: 25000
			"""));

		assertEquals(List.of(WardId.of("e"), WardId.of("d")),
			configuration.catalog().wards().stream().map(ward -> ward.id()).toList());
		assertEquals(List.of("e4", "e3", "e2", "e1", "d4", "d3"),
			configuration.catalog().ranks().stream().map(PrisonRank::id).map(RankId::value).toList());
		assertEquals("D Ward", configuration.catalog().requireWard(WardId.of("d")).displayName());
	}

	@Test
	void starterRankHasNoRankupRequirement() throws Exception {
		RankConfiguration configuration = RankConfiguration.load(yaml("""
			wards:
			  - id: E
			    ranks:
			      - id: E4
			      - id: E3
			        cost: 500
			"""));

		assertTrue(configuration.catalog().requireRank(RankId.of("e4")).rankup().isEmpty());
		assertEquals(500L, configuration.catalog().requireRank(RankId.of("e3"))
			.rankup().orElseThrow().cost());
	}

	@Test
	void parsesOptionalRequiresField() throws Exception {
		RankConfiguration configuration = RankConfiguration.load(yaml("""
			wards:
			  - id: E
			    ranks:
			      - id: E4
			      - id: E3
			        cost: 500
			      - id: E2
			        cost: 1000
			        requires: E4
			"""));

		assertEquals(Optional.of(RankId.of("e4")),
			configuration.catalog().requireRank(RankId.of("e2")).rankup().orElseThrow().requires());
	}

	@Test
	void rejectsStarterWithCost() {
		assertThrows(IllegalArgumentException.class, () -> RankConfiguration.load(yaml("""
			wards:
			  - id: E
			    ranks:
			      - id: E4
			        cost: 100
			""")));
	}

	@Test
	void rejectsNonStarterWithoutCost() {
		assertThrows(IllegalArgumentException.class, () -> RankConfiguration.load(yaml("""
			wards:
			  - id: E
			    ranks:
			      - id: E4
			      - id: E3
			""")));
	}

	@Test
	void rejectsBareStringRankEntries() {
		assertThrows(IllegalArgumentException.class, () -> RankConfiguration.load(yaml("""
			wards:
			  - id: E
			    ranks: [E4, E3]
			""")));
	}

	@Test
	void rejectsNegativeCost() {
		assertThrows(IllegalArgumentException.class, () -> RankConfiguration.load(yaml("""
			wards:
			  - id: E
			    ranks:
			      - id: E4
			      - id: E3
			        cost: -100
			""")));
	}

	@Test
	void rejectsDuplicateRanksAcrossWards() {
		assertThrows(IllegalArgumentException.class, () -> RankConfiguration.load(yaml("""
			wards:
			  - id: e
			    ranks:
			      - id: e4
			  - id: d
			    ranks:
			      - id: e4
			        cost: 100
			""")));
	}

	@Test
	void rejectsWardWithoutRanks() {
		assertThrows(IllegalArgumentException.class, () -> RankConfiguration.load(yaml("""
			wards:
			  - id: e
			    ranks: []
			""")));
	}

	@Test
	void rejectsMissingWardList() {
		assertThrows(IllegalArgumentException.class, () -> RankConfiguration.load(new YamlConfiguration()));
	}

	private static YamlConfiguration yaml(String source) {
		YamlConfiguration yaml = new YamlConfiguration();
		try {
			yaml.loadFromString(source);
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
		return yaml;
	}
}
