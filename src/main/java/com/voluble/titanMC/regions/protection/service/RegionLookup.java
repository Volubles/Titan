package com.voluble.titanMC.regions.protection.service;

import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.WorldId;

import java.util.List;

@FunctionalInterface
public interface RegionLookup {

	List<RegionDefinition> findAll(WorldId worldId, int x, int y, int z);
}
