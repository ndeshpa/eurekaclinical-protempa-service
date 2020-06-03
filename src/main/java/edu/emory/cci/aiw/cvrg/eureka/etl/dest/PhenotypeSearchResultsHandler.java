package edu.emory.cci.aiw.cvrg.eureka.etl.dest;

/*-
 * #%L
 * Eureka! Clinical Protempa Service
 * %%
 * Copyright (C) 2012 - 2019 Emory University
 * %%
 * This program is dual licensed under the Apache 2 and GPLv3 licenses.
 * 
 * Apache License, Version 2.0:
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * GNU General Public License version 3:
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.arp.javautil.collections.Collections;
import org.arp.javautil.sql.ConnectionSpec;
import org.protempa.DataSource;
import org.protempa.KnowledgeSource;
import org.protempa.KnowledgeSourceCache;
import org.protempa.KnowledgeSourceCacheFactory;
import org.protempa.KnowledgeSourceReadException;
import org.protempa.PropositionDefinitionCache;
import org.protempa.QueryException;
import org.protempa.backend.dsb.DataSourceBackend;
import org.protempa.dest.AbstractQueryResultsHandler;
import org.protempa.dest.QueryResultsHandlerCloseException;
import org.protempa.dest.QueryResultsHandlerProcessingException;
import org.protempa.dest.QueryResultsHandlerValidationFailedException;
import org.protempa.dest.table.ConstantColumnSpec;
import org.protempa.dest.table.RelDbTabularWriter;
import org.protempa.dest.table.TabularWriterException;
import org.protempa.proposition.Proposition;
import org.protempa.proposition.UniqueId;
import org.protempa.query.Query;
import org.protempa.query.QueryMode;

import edu.emory.cci.aiw.cvrg.eureka.etl.config.EtlProperties;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.PhenotypeDestinationTableColumnEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.PhenotypeSearchDestinationEntity;
import edu.emory.cci.aiw.etl.dest.config.Database;
import edu.emory.cci.aiw.etl.dest.config.DatabaseSpec;
import edu.emory.cci.aiw.i2b2etl.dest.table.PhenotypeObsFactTableHandler;

/**
 *
 * @author Nita Deshpande
 */
public class PhenotypeSearchResultsHandler extends AbstractQueryResultsHandler {

	private static final Logger LOGGER = Logger.getLogger(PhenotypeSearchResultsHandler.class.getName());
	private static final String RES_TABLE_NAME = "PHENOTYPE_OBSERVATION_FACT";

	private final PhenotypeSearchDestinationEntity phenotypeSearchDestinationEntity;
	private final Map<String, String> writers;
	private final Map<String, Integer> sqlParamCounts;
	private final RelDbTabularWriter rdbWriter;
	private final Map<String, Map<Long, Set<String>>> rowPropositionIdMap;
	private final EtlProperties etlProperties;
	private final KnowledgeSource knowledgeSource;
	private KnowledgeSourceCache ksCache;
	private Map<String, Map<Long, List<RelDbTableColumnSpecWrapper>>> rowRankToColumnByTableName;
	private final Query query;

	private PhenotypeObsFactTableHandler phenObsFactTableHandler;
	private PropositionDefinitionCache myCache;
	private final Database database;
	private final EurekaPhenotypeSearchConfiguration configuration;
	private final ConnectionSpec dataConnectionSpec;
	private final Set<String> dataSourceBackendIds;
	private final String[] queryPropIds;

	PhenotypeSearchResultsHandler(Query query, DataSource dataSource, KnowledgeSource inKnowledgeSource,
			EurekaPhenotypeSearchConfiguration configuration) throws SQLException {

		this.query = query;
		this.knowledgeSource = inKnowledgeSource;
		this.configuration = configuration;
		this.writers = new HashMap<>();
		this.sqlParamCounts = new HashMap<>();
		this.rowPropositionIdMap = new HashMap<>();
		this.rowRankToColumnByTableName = new HashMap<>();
		this.etlProperties = configuration.getEtlProperties();
		this.phenotypeSearchDestinationEntity = configuration.getPhenotypeSearchDestinationEntity();
		this.queryPropIds = query.getPropositionIds();


		DataSourceBackend[] dsBackends = dataSource.getBackends();
		this.dataSourceBackendIds = new HashSet<>();
		for (int i = 0; i < dsBackends.length; i++) {
			String id = dsBackends[i].getId();
			if (id != null) {
				this.dataSourceBackendIds.add(id);
			}
		}

		if (dataSource == null) {
			throw new IllegalArgumentException("dataSource cannot be null");
		}
		if (knowledgeSource == null) {
			throw new IllegalArgumentException("knowledgeSource cannot be null");
		}

		this.database = this.configuration.getDatabase();
		DatabaseSpec dataSchemaSpec = this.database.getDataSpec();
		if (dataSchemaSpec != null) {
			this.dataConnectionSpec = dataSchemaSpec.toConnectionSpec();
		} else {
			this.dataConnectionSpec = null;
		}
		
		this.phenObsFactTableHandler = new PhenotypeObsFactTableHandler();
		LOGGER.log(Level.FINE, "Created PhenotypeObsFactTableHandler");
		this.rdbWriter = new RelDbTabularWriter(dataConnectionSpec, this.phenObsFactTableHandler.getInsertStatement(RES_TABLE_NAME));
		
	}

	@Override
	public void validate() throws QueryResultsHandlerValidationFailedException {
	}

	@Override
	public void start(PropositionDefinitionCache cache) throws QueryResultsHandlerProcessingException {
		for (PhenotypeDestinationTableColumnEntity pte: this.phenotypeSearchDestinationEntity.getTableColumns()) {
			LOGGER.log(Level.FINE, "Getting tablenames: {1}: column: {0}", new Object[] {pte.getColumnName(), pte.getTableName()});
		}		
		List<String> tableNames = this.phenotypeSearchDestinationEntity.getTableColumns().stream()
				.map(PhenotypeDestinationTableColumnEntity::getTableName).distinct()
				.collect(Collectors.toCollection(ArrayList::new));
		myCache = cache;
		createWriters();
		try {
			this.ksCache = new KnowledgeSourceCacheFactory().getInstance(this.knowledgeSource, cache, true);
		} catch (KnowledgeSourceReadException ex) {
			throw new QueryResultsHandlerProcessingException(ex);
		}
	}

	@Override
	public void handleQueryResult(String keyId, List<Proposition> propositions,
			Map<Proposition, Set<Proposition>> forwardDerivations,
			Map<Proposition, Set<Proposition>> backwardDerivations, Map<UniqueId, Proposition> references)
			throws QueryResultsHandlerProcessingException {

		Map<String, List<Proposition>> collect = new HashMap<>();
		this.rowRankToColumnByTableName = new HashMap<>();
		for (Proposition prop : propositions) {
			LOGGER.log(Level.FINE, "Proposition: {0}", prop.getId());
			Collections.putList(collect, prop.getId(), prop);
			String uniqueId = prop.getUniqueId().getLocalUniqueId().getId();
			int pos = uniqueId.indexOf('^');
			LOGGER.log(Level.FINE, "Handling prop: {0} for id: {1}", new Object[] {prop.getId(), uniqueId.substring(0, pos)});
			mapColumnSpecsToColumnNames(uniqueId.substring(0, pos));
		}
		// tablename->(rownum->List of colspecs)
		for (Map.Entry<String, Map<Long, List<RelDbTableColumnSpecWrapper>>> tableNameToRowNumToColumnSpecs : this.rowRankToColumnByTableName
				.entrySet()) {
			String tableName = tableNameToRowNumToColumnSpecs.getKey();
			LOGGER.log(Level.FINE, "TableName: {0}", tableName);
			// for the tablename above, we get the row nums and associated col details
			Map<Long, List<RelDbTableColumnSpecWrapper>> rowNumToColumnSpecs = tableNameToRowNumToColumnSpecs
					.getValue();
			LOGGER.log(Level.FINE, "ColumnSpecs: {0}", rowNumToColumnSpecs.size());
			populateEntityList(tableName, rowNumToColumnSpecs, collect, propositions, keyId, forwardDerivations, backwardDerivations,
					references);
		}
	}

	private void populateEntityList(String tableName, Map<Long, List<RelDbTableColumnSpecWrapper>> rowNumToColumnSpecs,
			Map<String, List<Proposition>> collect, List<Proposition> propositions, String keyId, Map<Proposition, Set<Proposition>> forwardDerivations,
			Map<Proposition, Set<Proposition>> backwardDerivations, Map<UniqueId, Proposition> references)
			throws QueryResultsHandlerProcessingException {
		LOGGER.log(Level.FINE, "Populating table for table: {1} for key: {0}", new Object[] {keyId, tableName});
		for (Map.Entry<Long, List<RelDbTableColumnSpecWrapper>> rowNumToColumnSpec : rowNumToColumnSpecs.entrySet()) {
			List<RelDbTableColumnSpecWrapper> columnSpecs = rowNumToColumnSpec.getValue();
			this.rdbWriter.setTableName(tableName);
			Map<Long, Set<String>> rowPropIdValue = this.rowPropositionIdMap.get(tableName);
			LOGGER.log(Level.FINE, "rowPropIdValue size: {0}", new Object[] {rowPropIdValue == null? 0:rowPropIdValue.size()});
			if (rowPropIdValue != null) {
				Set<String> rowPropIds = rowPropIdValue.get(rowNumToColumnSpec.getKey());
				LOGGER.log(Level.FINE, "rowPropIds: {0}", new Object[] {rowPropIds == null? "null":StringUtils.join(rowPropIds, ", ")});
				LOGGER.log(Level.FINE, "collect.entrySet(): {0}", new Object[] {collect.entrySet() == null? "null":StringUtils.join(collect.entrySet(), ", ")});
				if (rowPropIds != null) {
					Set<String> propIds = collect.keySet();
					int n = this.sqlParamCounts.get(tableName); // one spec for each column.
					LOGGER.log(Level.FINE, "collect propIds: {0}", new Object[] {StringUtils.join(propIds, ", ")});
						for (String myPropId : propIds) {
							List<Proposition> propsForKeyId = collect.get(myPropId);
							for (Proposition myProp: propsForKeyId) {
								try {
									for (int i = 0; i < n; i++) {
										RelDbTableColumnSpecWrapper columnSpecWrapper = columnSpecs.get(i);
										columnSpecWrapper.getTableColumnSpec().columnValues(keyId, myProp, forwardDerivations,
												backwardDerivations, references, this.ksCache, this.rdbWriter);
										if(i==(n-1)) {
											rdbWriter.newRow();
											break;
										}
									}
								} catch (TabularWriterException ex) {
									throw new QueryResultsHandlerProcessingException("Could not write row", ex);
								}
								break;
							}
						}
//					}
				}
			}
		}
	}

	@Override
	public void finish() throws QueryResultsHandlerProcessingException {
		LOGGER.log(Level.FINE, "In finish, populating final results");
		String selectStmt = "INSERT INTO phenotypedata.phenotype_observation_fact_final (SELECT DISTINCT * FROM phenotypedata.phenotype_observation_fact) ";
		try (Connection conn = openDatabaseConnection();
                PreparedStatement finalTable = conn.prepareStatement(selectStmt);) {
			LOGGER.log(Level.FINE, "stmt:{0}", selectStmt);
			LOGGER.log(Level.FINE, "conn:{0}", conn.getSchema());
            finalTable.execute();
            conn.commit();
        } catch (SQLException ex) {
        	LOGGER.log(Level.FINE, "result:{0}", "Error running SQL statement");
        	throw new QueryResultsHandlerProcessingException("Error running SQL statement", ex);
        }		
	}

	@Override
	public void close() throws QueryResultsHandlerCloseException {
		QueryResultsHandlerCloseException exception = null;
		exception = closeWriters(exception);
		if (exception != null) {
			throw exception;
		}
	}

	private QueryResultsHandlerCloseException closeWriters(QueryResultsHandlerCloseException exception) {
		if (this.rdbWriter != null) {
			try {
				rdbWriter.close();
			} catch (TabularWriterException ex) {
				if (exception != null) {
					exception.addSuppressed(ex);
				} else {
					exception = new QueryResultsHandlerCloseException(ex);
				}
			}
		}
		return exception;
	}

	private void mapColumnSpecsToColumnNames(String entityName)
			throws QueryResultsHandlerProcessingException {
		for (PhenotypeDestinationTableColumnEntity tableColumn : this.phenotypeSearchDestinationEntity.getTableColumnsByEntitySpecName(entityName)) {
			String tableName = tableColumn.getTableName();
			LOGGER.log(Level.FINE, "Mapping Tablecolumns in table: {0}", tableName);
			Map<Long, List<RelDbTableColumnSpecWrapper>> rowRankToTableColumnSpecs = this.rowRankToColumnByTableName.get(tableName);
			LOGGER.log(Level.FINE, "rowRankToTableColumnSpecs: {0}", (rowRankToTableColumnSpecs == null? 0:rowRankToTableColumnSpecs.size()));
			if (rowRankToTableColumnSpecs == null || (rowRankToTableColumnSpecs.size() == 0)) {
				rowRankToTableColumnSpecs = new HashMap<>();
				this.rowRankToColumnByTableName.put(tableName, rowRankToTableColumnSpecs);
			}
			RelDbColumnSpecFormat linksFormat = new RelDbColumnSpecFormat(tableColumn.getColumnName(),tableColumn.getFormat(), this.dataConnectionSpec); 
			try {
				RelDbTableColumnSpecWrapper tableColumnSpecWrapper = newTableColumnSpec(tableColumn, linksFormat);
				String pid = tableColumnSpecWrapper.getPropId();
				if (pid != null) {
					Long rowRank = tableColumn.getRowRank();
					Map<Long, Set<String>> rowToPropIds = this.rowPropositionIdMap.get(tableName);
					if (rowToPropIds == null) {
						rowToPropIds = new HashMap<>();
						this.rowPropositionIdMap.put(tableName, rowToPropIds);
	
					}
					for (String propId : myCache.collectPropIdDescendantsUsingInverseIsA(pid)) {
						org.arp.javautil.collections.Collections.putSet(rowToPropIds, rowRank, propId);
					}
				}
				org.arp.javautil.collections.Collections.putList(rowRankToTableColumnSpecs, tableColumn.getRowRank(),
						tableColumnSpecWrapper);
			} catch (QueryException | ParseException | SQLException ex) {
				throw new QueryResultsHandlerProcessingException(ex);
			}
		}
	}

	private void createWriters() throws QueryResultsHandlerProcessingException {
		List<String> tableNames = this.phenotypeSearchDestinationEntity.getTableColumns().stream()
				.map(PhenotypeDestinationTableColumnEntity::getTableName).distinct()
				.collect(Collectors.toCollection(ArrayList::new));
		LOGGER.log(Level.FINE, "Tables: {0}", StringUtils.join(tableNames, ", "));
		String nullValue = this.phenotypeSearchDestinationEntity.getNullValue();
		boolean doAppend = this.query.getQueryMode() != QueryMode.REPLACE;
		if (!doAppend) {
			// truncate all tables
			try {
				truncateTables();
			} catch (SQLException e) {
				throw new QueryResultsHandlerProcessingException(e);
			}
		}
		for (int i = 0, n = tableNames.size(); i < n; i++) {
			String tableName = tableNames.get(i);
			this.writers.put(tableName, this.phenObsFactTableHandler.getInsertStatement(tableName));
			this.sqlParamCounts.put(tableName, this.phenObsFactTableHandler.getNumCols(tableName));
			LOGGER.log(Level.FINE, "SQL Statement for {0}: {1}", new Object[] {tableName, this.writers.get(tableName)});
		}
	}
	
	private RelDbTableColumnSpecWrapper newTableColumnSpec(PhenotypeDestinationTableColumnEntity tableColumn,
			RelDbColumnSpecFormat linksFormat) throws ParseException, SQLException { 
		String path = tableColumn.getPath();
		if (path != null) {
			linksFormat.setRelDbTabularWriter(this.rdbWriter);
			return (RelDbTableColumnSpecWrapper) linksFormat.parseObject(path);
		} else {
			return new RelDbTableColumnSpecWrapper(null, new ConstantColumnSpec(tableColumn.getColumnName(), null),
					this.rdbWriter);
		}
	}

	private Connection openDatabaseConnection() throws SQLException {
		return this.dataConnectionSpec.getOrCreate();
	}
	
    private void truncateTables() throws SQLException {
        try (final Connection conn = openDatabaseConnection();) {
            conn.setAutoCommit(true);
            String[] dataschemaTables = {"PHENOTYPE_OBSERVATION_FACT", "PHENOTYPE_OBSERVATION_FACT_FINAL"};
            if(this.queryPropIds!= null) {           	
            	for (String tableName : dataschemaTables) {
            		truncateTable(conn, tableName);
            	}            	
            }
            LOGGER.log(Level.FINE, "Done truncating temp data tables for query {0}", this.query.getName());
        }
    }

    private void truncateTable(Connection conn, String tableName) throws SQLException {
        String queryId = query.getName();
        String sql = "TRUNCATE TABLE " + tableName;
        try (final Statement st = conn.createStatement()) {
            st.execute(sql);
            LOGGER.log(Level.FINE, "Done executing SQL for query {0}", queryId);
        } catch (SQLException ex) {
        	LOGGER.log(Level.FINE, "An error occurred truncating the tables for query " + queryId, ex);
            throw ex;
        }
    }
    

}
