package com.voluble.titanMC.regions.protection.service;

import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.WorldId;

import java.util.List;
import com.voluble.titanMC.regions.index.RegionReadView;

@FunctionalInterface
public interface RegionLookup {

	List<RegionDefinition> findAll(WorldId worldId, int x, int y, int z);

	default long version() {
		return -1L;
	}

	static RegionLookup from(RegionReadView view) {
		return new RegionLookup() {
			@Override
			public List<RegionDefinition> findAll(WorldId worldId, int x, int y, int z) {
				return view.findAll(worldId, x, y, z);
			}

			@Override
			public long version() {
				return view.version();
			}
		};
	}
}
