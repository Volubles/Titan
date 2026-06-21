package com.voluble.titanMC.donatortools.tool;

import java.util.Optional;

@FunctionalInterface
public interface SingleBlockDropStrategy {

	Optional<SingleBlockDropResolution> resolve(SingleBlockDropContext context);
}
