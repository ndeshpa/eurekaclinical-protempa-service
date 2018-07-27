package edu.emory.cci.aiw.cvrg.eureka.etl.dest;

/*
 * #%L
 * Eureka Protempa ETL
 * %%
 * Copyright (C) 2012 - 2015 Emory University
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
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.TabularFileDestinationEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.TabularFileDestinationTableColumnEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.config.EtlProperties;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.protempa.KnowledgeSource;
import org.protempa.KnowledgeSourceCache;
import org.protempa.KnowledgeSourceCacheFactory;
import org.protempa.KnowledgeSourceReadException;
import org.protempa.PropositionDefinitionCache;
import org.protempa.QueryException;
import org.protempa.dest.AbstractQueryResultsHandler;
import org.protempa.dest.QueryResultsHandlerCloseException;
import org.protempa.dest.QueryResultsHandlerProcessingException;
import org.protempa.dest.QueryResultsHandlerValidationFailedException;
import org.protempa.dest.table.ConstantColumnSpec;
import org.protempa.dest.table.FileTabularWriter;
import org.protempa.dest.table.QuoteModel;
import org.protempa.dest.table.TableColumnSpec;
import org.protempa.dest.table.TabularWriterException;
import org.protempa.proposition.Proposition;
import org.protempa.proposition.UniqueId;
import org.protempa.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Andrew Post
 */
public class TabularFileQueryResultsHandler extends AbstractQueryResultsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TabularFileQueryResultsHandler.class);

    private final TabularFileDestinationEntity config;
    private Map<String, FileTabularWriter> writers;
    private Map<String, List<TableColumnSpec>> tableColumnSpecs;
    private final Map<String, Set<String>> rowPropositionIdMap;
    private final EtlProperties etlProperties;
    private KnowledgeSource knowledgeSource;
    private KnowledgeSourceCache ksCache;
    private final char delimiter;

    TabularFileQueryResultsHandler(Query query, TabularFileDestinationEntity inTabularFileDestinationEntity, EtlProperties inEtlProperties, KnowledgeSource inKnowledgeSource) {
        assert inTabularFileDestinationEntity != null : "inTabularFileDestinationEntity cannot be null";
        this.etlProperties = inEtlProperties;
        this.config = inTabularFileDestinationEntity;
        this.knowledgeSource = inKnowledgeSource;
        Character delim = inTabularFileDestinationEntity.getDelimiter();
        if (delim != null) {
            this.delimiter = delim;
        } else {
            this.delimiter = '\t';
        }
        this.rowPropositionIdMap = new HashMap<>();
    }

    @Override
    public void validate() throws QueryResultsHandlerValidationFailedException {
    }

    @Override
    public void start(PropositionDefinitionCache cache) throws QueryResultsHandlerProcessingException {
        try {
            File outputFileDirectory = this.etlProperties.outputFileDirectory(this.config.getName());
            List<String> tableNames = this.config.getTableColumns()
                    .stream()
                    .map(TabularFileDestinationTableColumnEntity::getTableName)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
            this.writers = new HashMap<>();
            String nullValue = this.config.getNullValue();
            for (int i = 0, n = tableNames.size(); i < n; i++) {
                String tableName = tableNames.get(i);
                File file = new File(outputFileDirectory, tableName);
                this.writers.put(tableName, new FileTabularWriter(
                        new BufferedWriter(new FileWriter(file)),
                        this.delimiter,
                        this.config.isAlwaysQuoted() ? QuoteModel.ALWAYS : QuoteModel.WHEN_QUOTE_EMBEDDED,
                        nullValue == null ? "" : nullValue));
            }
        } catch (IOException ex) {
            throw new QueryResultsHandlerProcessingException(ex);
        }

        List<TabularFileDestinationTableColumnEntity> tableColumns = this.config.getTableColumns();
        Collections.sort(tableColumns,
                (TabularFileDestinationTableColumnEntity o1, TabularFileDestinationTableColumnEntity o2) -> o1.getRank().compareTo(o2.getRank()));
        this.tableColumnSpecs = new HashMap<>();
        for (TabularFileDestinationTableColumnEntity tableColumn : tableColumns) {
            String format = tableColumn.getFormat();
            TableColumnSpecFormat linksFormat = new TableColumnSpecFormat(tableColumn.getColumnName(), format != null ? new SimpleDateFormat(format) : null);
            TableColumnSpecWrapper tableColumnSpecWrapper = toTableColumnSpec(tableColumn, linksFormat);
            String pid = tableColumnSpecWrapper.getPropId();
            if (pid != null) {
                try {
                    for (String propId : cache.collectPropIdDescendantsUsingInverseIsA(pid)) {
                        org.arp.javautil.collections.Collections.putSet(this.rowPropositionIdMap, tableColumn.getTableName(), propId);
                    }
                } catch (QueryException ex) {
                    throw new QueryResultsHandlerProcessingException(ex);
                }
            }
            org.arp.javautil.collections.Collections.putList(this.tableColumnSpecs, tableColumn.getTableName(), tableColumnSpecWrapper.getTableColumnSpec());
        }

        LOGGER.debug("Row concepts: {}", this.rowPropositionIdMap);

        for (Map.Entry<String, List<TableColumnSpec>> me : this.tableColumnSpecs.entrySet()) {
            List<String> columnNames = new ArrayList<>();
            for (TableColumnSpec columnSpec : me.getValue()) {
                String[] colNames;
                try {
                    colNames = columnSpec.columnNames(this.knowledgeSource);
                } catch (KnowledgeSourceReadException ex) {
                    throw new AssertionError("Should never happen");
                }
                for (String colName : colNames) {
                    columnNames.add(colName);
                }
            }
            FileTabularWriter writer = this.writers.get(me.getKey());
            try {
                for (String columnName : columnNames) {
                    writer.writeString(columnName);
                }
                writer.newRow();
            } catch (TabularWriterException ex) {
                throw new QueryResultsHandlerProcessingException(ex);
            }
        }

        try {
            this.ksCache = new KnowledgeSourceCacheFactory().getInstance(this.knowledgeSource, cache, true);
        } catch (KnowledgeSourceReadException ex) {
            throw new QueryResultsHandlerProcessingException(ex);
        }
    }

    @Override
    public void handleQueryResult(String keyId, List<Proposition> propositions,
            Map<Proposition, Set<Proposition>> forwardDerivations,
            Map<Proposition, Set<Proposition>> backwardDerivations,
            Map<UniqueId, Proposition> references) throws QueryResultsHandlerProcessingException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Data for keyId {}: {}", new Object[]{keyId, propositions});
        }

        for (Map.Entry<String, List<TableColumnSpec>> me : this.tableColumnSpecs.entrySet()) {
            String tableName = me.getKey();
            List<TableColumnSpec> columnSpecs = me.getValue();
            int n = columnSpecs.size();
            FileTabularWriter writer = this.writers.get(tableName);
            Set<String> rowPropIds = this.rowPropositionIdMap.get(tableName);
            if (rowPropIds != null) {
                for (Proposition prop : propositions) {
                    if (rowPropIds.contains(prop.getId())) {
                        try {
                            for (int i = 0; i < n; i++) {
                                TableColumnSpec columnSpec = columnSpecs.get(i);
                                columnSpec.columnValues(keyId, prop, forwardDerivations, backwardDerivations, references, this.ksCache, writer);
                            }
                            writer.newRow();
                        } catch (TabularWriterException ex) {
                            throw new QueryResultsHandlerProcessingException("Could not write row" + ex);
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
        if (this.writers != null) {
            for (FileTabularWriter writer : this.writers.values()) {
                try {
                    writer.close();
                } catch (TabularWriterException ex) {
                    if (exception != null) {
                        exception.addSuppressed(ex);
                    } else {
                        exception = new QueryResultsHandlerCloseException(ex);
                    }
                }
                this.writers = null;
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    private static TableColumnSpecWrapper toTableColumnSpec(
            TabularFileDestinationTableColumnEntity tableColumn,
            TableColumnSpecFormat linksFormat) throws QueryResultsHandlerProcessingException {
        try {
            String path = tableColumn.getPath();
            if (path != null) {
                return (TableColumnSpecWrapper) linksFormat.parseObject(path);
            } else {
                return new TableColumnSpecWrapper(new ConstantColumnSpec(tableColumn.getColumnName(), null));
            }
        } catch (ParseException ex) {
            throw new QueryResultsHandlerProcessingException(ex);
        }
    }

}
