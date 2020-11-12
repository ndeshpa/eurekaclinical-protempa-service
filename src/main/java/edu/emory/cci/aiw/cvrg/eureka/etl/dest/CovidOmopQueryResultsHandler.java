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


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.arp.javautil.collections.Collections;
import org.arp.javautil.sql.ConnectionSpec;
import org.protempa.DataSource;
import org.protempa.KnowledgeSource;
import org.protempa.KnowledgeSourceCache;
import org.protempa.KnowledgeSourceCacheFactory;
import org.protempa.KnowledgeSourceReadException;
import org.protempa.PropositionDefinition;
import org.protempa.PropositionDefinitionCache;
import org.protempa.QueryException;
import org.protempa.backend.dsb.DataSourceBackend;
import org.protempa.dest.AbstractQueryResultsHandler;
import org.protempa.dest.QueryResultsHandlerCloseException;
import org.protempa.dest.QueryResultsHandlerProcessingException;
import org.protempa.dest.QueryResultsHandlerValidationFailedException;
import org.protempa.dest.table.ConstantColumnSpec;
import org.protempa.dest.table.FileTabularWriter;
import org.protempa.dest.table.RelDbTabularWriter;
import org.protempa.dest.table.TabularWriterException;
import org.protempa.proposition.Proposition;
import org.protempa.proposition.UniqueId;
import org.protempa.query.Query;
import org.protempa.query.QueryMode;

import java.util.logging.Level;
import java.util.logging.Logger;

import edu.emory.cci.aiw.cvrg.eureka.etl.config.EtlProperties;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.CovidOmopDestinationEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.CovidOmopDestinationTableColumnEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.OmopDestinationEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.OmopDestinationTableColumnEntity;
import edu.emory.cci.aiw.etl.dest.config.Database;
import edu.emory.cci.aiw.etl.dest.config.DatabaseSpec;
import edu.emory.cci.aiw.omopetl.dest.table.OmopTableHandler;
import edu.emory.cci.aiw.omopetl.util.OmopFileOutputHandler;

/**
 *
 * @author Nita Deshpande
 */
public class CovidOmopQueryResultsHandler extends AbstractQueryResultsHandler {

	private static final Logger LOGGER = Logger.getLogger(CovidOmopQueryResultsHandler.class.getName());

	private final CovidOmopDestinationEntity covidOmopDestinationEntity;
	private final Map<String, String> writers;
	private final Map<String, BufferedWriter> fileWriters;
	private final Map<String, FileTabularWriter> headerWriters;
	private final RelDbTabularWriter rdbWriter;
	private final Map<String, Map<Long, Set<String>>> rowPropositionIdMap;
	private final EtlProperties etlProperties;
	private final KnowledgeSource knowledgeSource;
	private KnowledgeSourceCache ksCache;
	private Map<String, Map<Long, List<RelDbTableColumnSpecWrapper>>> rowRankToColumnByTableName;
	private final Query query;

	private OmopTableHandler omopTableHandler;
	private OmopFileOutputHandler omopFileOutputHandler;
	private final Database database;
	private final CovidOmopConfiguration configuration;
	private final ConnectionSpec dataConnectionSpec;
	private final Set<String> dataSourceBackendIds;
	private final String[] queryPropIds;

	CovidOmopQueryResultsHandler(Query query, DataSource dataSource, KnowledgeSource inKnowledgeSource,
			CovidOmopConfiguration configuration) throws SQLException {

		this.query = query;
		this.knowledgeSource = inKnowledgeSource;
		this.configuration = configuration;
		this.writers = new HashMap<>();
		this.fileWriters = new HashMap<>();
		this.headerWriters = new HashMap<>();
		this.rowPropositionIdMap = new HashMap<>();
		this.rowRankToColumnByTableName = new HashMap<>();
		this.etlProperties = configuration.getEtlProperties();
		this.covidOmopDestinationEntity = configuration.getOmopDestinationEntity();
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
		
		this.rdbWriter = new RelDbTabularWriter(dataConnectionSpec, this.writers);
		
	}

	@Override
	public void validate() throws QueryResultsHandlerValidationFailedException {
	}

	@Override
	public void start(PropositionDefinitionCache cache) throws QueryResultsHandlerProcessingException {
		this.omopTableHandler = new OmopTableHandler();
		this.omopFileOutputHandler = new OmopFileOutputHandler();
		createWriters();
		createOutputFileWriters();
		LOGGER.log(Level.INFO,"Before Setting Columns");
//		this.rdbWriter.setStatements(this.writers);
//		this.rdbWriter.setColCounts(this.omopTableHandler.getTableColumnCount());
		LOGGER.log(Level.INFO,"Going to map columnspecs to column names");
		mapColumnSpecsToColumnNames(cache);
		LOGGER.log(Level.INFO,"Completed mapping of columnspecs to column names");
		try {
			this.ksCache = new KnowledgeSourceCacheFactory().getInstance(this.knowledgeSource, cache, true);
		} catch (KnowledgeSourceReadException ex) {
			throw new QueryResultsHandlerProcessingException(ex);
		}
		query.getPropositionIds();
	}

	@Override
	public void handleQueryResult(String keyId, List<Proposition> propositions,
			Map<Proposition, Set<Proposition>> forwardDerivations,
			Map<Proposition, Set<Proposition>> backwardDerivations, Map<UniqueId, Proposition> references)
			throws QueryResultsHandlerProcessingException {

		Map<String, List<Proposition>> collect = new HashMap<>();
		for (Proposition prop : propositions) {
			Collections.putList(collect, prop.getId(), prop);
		}

		// tablename->(rownum->List of colspecs)
		for (Map.Entry<String, Map<Long, List<RelDbTableColumnSpecWrapper>>> tableNameToRowNumToColumnSpecs : this.rowRankToColumnByTableName
				.entrySet()) {
			String tableName = tableNameToRowNumToColumnSpecs.getKey();
			// for the tablename above, we get the row nums and associated col details
			Map<Long, List<RelDbTableColumnSpecWrapper>> rowNumToColumnSpecs = tableNameToRowNumToColumnSpecs
					.getValue();
			populateEntityList(tableName, rowNumToColumnSpecs, collect, keyId, forwardDerivations, backwardDerivations,
					references);
		}
	}

	private void populateEntityList(String tableName, Map<Long, List<RelDbTableColumnSpecWrapper>> rowNumToColumnSpecs,
			Map<String, List<Proposition>> collect, String keyId, Map<Proposition, Set<Proposition>> forwardDerivations,
			Map<Proposition, Set<Proposition>> backwardDerivations, Map<UniqueId, Proposition> references)
			throws QueryResultsHandlerProcessingException {
		for (Map.Entry<Long, List<RelDbTableColumnSpecWrapper>> rowNumToColumnSpec : rowNumToColumnSpecs.entrySet()) {
			List<RelDbTableColumnSpecWrapper> columnSpecs = rowNumToColumnSpec.getValue();
			int n = columnSpecs.size(); // one spec for each column.
			this.rdbWriter.setTableName(tableName);
			this.rdbWriter.setRecordHandler(this.rdbWriter.getHandlerList().get(tableName));
			Map<Long, Set<String>> rowPropIdValue = this.rowPropositionIdMap.get(tableName);
			if (rowPropIdValue != null) {
				Set<String> rowPropIds = rowPropIdValue.get(rowNumToColumnSpec.getKey());
				if (rowPropIds != null) {
					Iterator<Map.Entry<String, List<Proposition>>> itr = collect.entrySet().stream()
							.filter(me -> rowPropIds.contains(me.getKey())).iterator();
					for (; itr.hasNext();) {
						for (Proposition prop : itr.next().getValue()) {
							try {
								for (int i = 0; i < n; i++) {
									RelDbTableColumnSpecWrapper columnSpecWrapper = columnSpecs.get(i);
									columnSpecWrapper.getTableColumnSpec().columnValues(keyId, prop, forwardDerivations,
											backwardDerivations, references, this.ksCache, this.rdbWriter);
								}
								rdbWriter.newRow();
							} catch (TabularWriterException ex) {
								throw new QueryResultsHandlerProcessingException("Could not write row", ex);
							}
						}
					}
				}
			}
		}
	}

	
	@Override
	public void finish() throws QueryResultsHandlerProcessingException {

	}

	@Override
	public void close() throws QueryResultsHandlerCloseException {
		QueryResultsHandlerCloseException exception = null;
		exception = closeWriters(exception);
		String selectStmt;
		String header;
		try (Connection conn = openDataDatabaseConnection();
                CallableStatement mappingCall = conn.prepareCall("{ call AOU_DATA_VALIDATE() }")) {
            conn.setAutoCommit(true);
            mappingCall.execute();
        } catch (SQLException ex) {
        	throw new QueryResultsHandlerCloseException("Error running stored procedures", ex);
        }
		
		for(String tableName : this.fileWriters.keySet()) {
			try {
				BufferedWriter bw = this.fileWriters.get(tableName);
				selectStmt = this.omopFileOutputHandler.getSelectStatement(tableName);
				header = this.omopFileOutputHandler.getHeader(tableName);
				bw.append(header).append('\n');
				try(Connection conn = openDataDatabaseConnection()){
					PreparedStatement ps = conn.prepareStatement(selectStmt);
					ResultSet rs = ps.executeQuery();
					ResultSetMetaData rsmd = rs.getMetaData();
					int columnsNumber = rsmd.getColumnCount();
					String data;
					while(rs.next()) {
						for(int i=1; i<=columnsNumber; i++) {
							data = rs.getString(i);
							if((data==null) || (data.equalsIgnoreCase("null")))
									data = "";
							if(i<=(columnsNumber-1))
								bw.append('"').append(data).append('"').append('\t');
							else
								bw.append('"').append(data).append('"').append('\n');
						}
					}
				} catch (SQLException e) {
					throw new QueryResultsHandlerCloseException("Error running sql selects", e);
				}
				bw.flush();
				bw.close();
			} catch (IOException e1) {
				throw new QueryResultsHandlerCloseException("Error writing results: ", e1);
			}
		}
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

	private void mapColumnSpecsToColumnNames(PropositionDefinitionCache cache)
			throws QueryResultsHandlerProcessingException {
		this.rowRankToColumnByTableName = new HashMap<>();
		for (CovidOmopDestinationTableColumnEntity tableColumn : this.covidOmopDestinationEntity.getTableColumns()) {
			String tableName = tableColumn.getTableName();
			Map<Long, List<RelDbTableColumnSpecWrapper>> rowRankToTableColumnSpecs = this.rowRankToColumnByTableName
					.get(tableName);
			if (rowRankToTableColumnSpecs == null) {
				rowRankToTableColumnSpecs = new HashMap<>();
				this.rowRankToColumnByTableName.put(tableName, rowRankToTableColumnSpecs);
			}
			RelDbColumnSpecFormat linksFormat = new RelDbColumnSpecFormat(tableColumn.getColumnName(),
					tableColumn.getFormat(), this.dataConnectionSpec); //, idPool
			try {
				RelDbTableColumnSpecWrapper tableColumnSpecWrapper = newTableColumnSpec(tableColumn, linksFormat);//,idPool
				String pid = tableColumnSpecWrapper.getPropId();
				if (pid != null) {
					Long rowRank = tableColumn.getRowRank();
					Map<Long, Set<String>> rowToPropIds = this.rowPropositionIdMap.get(tableName);
					if (rowToPropIds == null) {
						rowToPropIds = new HashMap<>();
						this.rowPropositionIdMap.put(tableName, rowToPropIds);
					}
					StringBuilder sb = new StringBuilder();
					for (String propId : cache.collectPropIdDescendantsUsingInverseIsA(pid)) {
						org.arp.javautil.collections.Collections.putSet(rowToPropIds, rowRank, propId);
						sb.append(rowRank).append(":").append(propId).append(";");
					}
					 LOGGER.log(Level.FINE, "tablename:{2} :: pid:{0} :: details:{1}", new Object[] {pid, sb.toString(), tableName});
				}
				org.arp.javautil.collections.Collections.putList(rowRankToTableColumnSpecs, tableColumn.getRowRank(),
						tableColumnSpecWrapper);
			} catch (QueryException | ParseException | SQLException ex) {
				throw new QueryResultsHandlerProcessingException(ex);
			}
		}
		LOGGER.log(Level.WARNING, "Row concepts: {0}", this.rowPropositionIdMap);
	}

	private void createWriters() throws QueryResultsHandlerProcessingException {
		List<String> tableNames = this.covidOmopDestinationEntity.getTableColumns().stream()
				.map(CovidOmopDestinationTableColumnEntity::getTableName).distinct()
				.collect(Collectors.toCollection(ArrayList::new));
		LOGGER.log(Level.INFO, "Got table names: {0}", StringUtils.join(tableNames, ","));
		String nullValue = this.covidOmopDestinationEntity.getNullValue();
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
			String inStatement = this.omopTableHandler.getInsertStatement(tableName);
			this.writers.put(tableName, inStatement);
			LOGGER.log(Level.INFO, "Got Insert statement: {0}", inStatement);
		}
		//this.rdbWriter.setStatements(this.writers);
		//LOGGER.log(Level.INFO, "Set Writers: {0}", this.rdbWriter.getStatements().size());
	}
	
	private void createOutputFileWriters() throws QueryResultsHandlerProcessingException {
        try {
            File outputFileDirectory = this.etlProperties.outputFileDirectory(this.covidOmopDestinationEntity.getName());
            List<String> tableNames = this.covidOmopDestinationEntity.getTableColumns()
                    .stream()
                    .map(CovidOmopDestinationTableColumnEntity::getTableName)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
            String nullValue = this.covidOmopDestinationEntity.getNullValue();
            boolean doAppend = this.query.getQueryMode() != QueryMode.REPLACE;
            if (!doAppend) {
                for (File f : outputFileDirectory.listFiles()) {
                    f.delete();
                }
            }
            for (int i = 0, n = tableNames.size(); i < n; i++) {
                String tableName = tableNames.get(i);
                File file = new File(outputFileDirectory, tableName);
                this.fileWriters.put(tableName, new BufferedWriter(new FileWriter(file, doAppend)));
            }
        } catch (IOException ex) {
            throw new QueryResultsHandlerProcessingException(ex);
        }
    }


	private RelDbTableColumnSpecWrapper newTableColumnSpec(CovidOmopDestinationTableColumnEntity tableColumn,
			RelDbColumnSpecFormat linksFormat) throws ParseException, SQLException { //, IdPool pool
		String path = tableColumn.getPath();
		this.rdbWriter.setRecordHandler(this.rdbWriter.getHandlerList()
				.get(this.omopTableHandler.getInsertStatement(tableColumn.getTableName())));
		if (path != null) {
			linksFormat.setRelDbTabularWriter(this.rdbWriter);
			return (RelDbTableColumnSpecWrapper) linksFormat.parseObject(path);
		} else {
			return new RelDbTableColumnSpecWrapper(null, new ConstantColumnSpec(tableColumn.getColumnName(), null),
					this.rdbWriter);
		}
	}

	private Connection openDataDatabaseConnection() throws SQLException {
		return this.dataConnectionSpec.getOrCreate();
	}
	
    private void truncateTables() throws SQLException {
        try (final Connection conn = openDataDatabaseConnection()) {
            conn.setAutoCommit(true);
            String[] dataschemaTables = {"address_temp", "care_site_temp", "condition_occurrence_temp", "death_temp", "drug_exposure_temp", "email_temp", "location_temp", "measurement_temp", 
            		"mrn_temp", "name_temp","person_temp", "phone_number_temp", "procedure_occurrence_temp", "provider_temp", "visit_occurrence_temp"};
            if(this.queryPropIds!= null) {
            	if(this.queryPropIds.length>1 ) {
            		for (String tableName : dataschemaTables) {
            			truncateTable(conn, tableName);
            		}
            	}
            	else if(this.queryPropIds.length ==1) {
            		if(this.queryPropIds[0].equals("CareSite"))
            			truncateTable(conn, "care_site_temp");
            		if(this.queryPropIds[0].equals("Provider"))
            			truncateTable(conn, "provider_temp");
            	}
            }
            LOGGER.log(Level.INFO, "Done truncating temp data tables for query:" +  this.query.getName());
        }
    }

    private void truncateTable(Connection conn, String tableName) throws SQLException {
        String queryId = query.getName();
        String sql = "TRUNCATE TABLE " + tableName;
        try (final Statement st = conn.createStatement()) {
            st.execute(sql);
            LOGGER.log(Level.INFO,"Done executing SQL for query {0}", queryId);
        } catch (SQLException ex) {
        	LOGGER.log(Level.INFO, "An error occurred truncating the tables for query " + queryId, ex);
            throw ex;
        }
    }
    

}
