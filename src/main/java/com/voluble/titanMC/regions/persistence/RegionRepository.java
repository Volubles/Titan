package com.voluble.titanMC.regions.persistence;

import com.voluble.titanMC.regions.model.RegionDefinition;
import com.voluble.titanMC.regions.model.RegionId;

import java.sql.SQLException;
import java.util.List;

public interface RegionRepository extends AutoCloseable {

	void initialize() throws SQLException;

	List<RegionDefinition> loadAll() throws SQLException;

	void save(RegionDefinition definition) throws SQLException;

	void delete(RegionId id) throws SQLException;

	@Override
	void close() throws SQLException;
}
