package com.voluble.titanMC.ranks.config;

import com.voluble.titanMC.ranks.model.PrisonRank;
import com.voluble.titanMC.ranks.model.RankId;
import com.voluble.titanMC.ranks.model.WardId;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RankConfigurationTest {
	@Test
	void loadsWardsAndRanksInConfiguredOrder() throws Exception {
		RankConfiguration configuration = RankConfiguration.load(yaml("""
			wards:
			  - id: E
			    display-name: E Ward
			    ranks: [E4, E3, E2, E1]
			  - id: D
			    ranks: [D4, D3]
			"""));

		assertEquals(List.of(WardId.of("e"), WardId.of("d")),
			configuration.catalog().wards().stream().map(ward -> ward.id()).toList());
		assertEquals(List.of("e4", "e3", "e2", "e1", "d4", "d3"),
			configuration.catalog().ranks().stream().map(PrisonRank::id).map(RankId::value).toList());
		assertEquals("D Ward", configuration.catalog().requireWard(WardId.of("d")).displayName());
	}

	@Test
	void rejectsDuplicateRanksAcrossWards() throws Exception {
		YamlConfiguration yaml = yaml("""
			wards:
			  - id: e
			    ranks: [e4]
			  - id: d
			    ranks: [e4]
			""");

		assertThrows(IllegalArgumentException.class, () -> RankConfiguration.load(yaml));
	}

	@Test
	void rejectsWardWithoutRanks() throws Exception {
		YamlConfiguration yaml = yaml("""
			wards:
			  - id: e
			    ranks: []
			""");

		assertThrows(IllegalArgumentException.class, () -> RankConfiguration.load(yaml));
	}

	@Test
	void rejectsMissingWardList() {
		assertThrows(IllegalArgumentException.class, () -> RankConfiguration.load(new YamlConfiguration()));
	}

	private static YamlConfiguration yaml(String source) throws Exception {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.loadFromString(source);
		return yaml;
	}
}
