package com.voluble.titanMC.regions.index;

import com.voluble.titanMC.regions.model.RegionDefinition;

@FunctionalInterface
public interface RegionVisitor {

	RegionVisitResult visit(RegionDefinition region);
}
