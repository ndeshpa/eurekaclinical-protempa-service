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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.arp.javautil.sql.ConnectionSpec;
import org.arp.javautil.sql.DatabaseAPI;
import org.arp.javautil.sql.InvalidConnectionSpecArguments;
import org.protempa.DataSource;
import org.protempa.KnowledgeSource;
import org.protempa.PropositionDefinitionCache;
import org.protempa.dest.AbstractQueryResultsHandler;
import org.protempa.dest.QueryResultsHandlerCloseException;
import org.protempa.dest.QueryResultsHandlerProcessingException;
import org.protempa.dest.QueryResultsHandlerValidationFailedException;
import org.protempa.proposition.AbstractProposition;
import org.protempa.proposition.Proposition;
import org.protempa.proposition.UniqueId;
import org.protempa.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.emory.cci.aiw.i2b2etl.dest.config.Configuration;
import edu.emory.cci.aiw.i2b2etl.dest.config.Database;
import edu.emory.cci.aiw.i2b2etl.dest.config.DatabaseSpec;

/**
 *
 * @author Nita
 */
public class AOUParticipantQueryResultsHandler extends AbstractQueryResultsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AOUParticipantQueryResultsHandler.class);
    private final KnowledgeSource knowledgeSource;
    private Configuration eurekaAOUConfiguration;    
    private DatabaseAPI databaseAPI;
    private final Database database;
    private final ConnectionSpec dataConnectionSpec;
    private Connection dataSchemaConnection;
    private String dataSchemaName;

    AOUParticipantQueryResultsHandler(Query query, DataSource dataSource, Configuration configuration, KnowledgeSource inKnowledgeSource) {
        this.knowledgeSource = inKnowledgeSource;
        this.eurekaAOUConfiguration = configuration;
        LOGGER.info("Using configuration: " + this.eurekaAOUConfiguration.getName());
        this.databaseAPI = DatabaseAPI.DRIVERMANAGER;
        this.database = this.eurekaAOUConfiguration.getDatabase();      
        DatabaseSpec dataSchemaSpec = this.database.getDataSpec();
        LOGGER.info("Using database: " + this.database.getDataSpec().getConnect());
        if (dataSchemaSpec != null) {
            this.dataConnectionSpec = dataSchemaSpec.toConnectionSpec();
        } else {
            this.dataConnectionSpec = null;
        }
        LOGGER.info("dsb array:" + Arrays.toString(dataSource.getBackends()));
    }

    @Override
    public void validate() throws QueryResultsHandlerValidationFailedException {
    }

    @Override
    public void start(PropositionDefinitionCache cache) throws QueryResultsHandlerProcessingException {
        try {
            this.dataSchemaConnection = openDataDatabaseConnection();
            this.dataSchemaName = this.dataSchemaConnection.getSchema();
            LOGGER.info("dataSchemaName:" + this.dataSchemaName);
            this.clearTables();
        } catch (SQLException e) {
            throw new QueryResultsHandlerProcessingException(e);
        } catch (QueryResultsHandlerCloseException e) {
            throw new QueryResultsHandlerProcessingException(e);
        }
    }

    @Override
    public void handleQueryResult(String keyId, List<Proposition> propositions,
            Map<Proposition, Set<Proposition>> forwardDerivations,
            Map<Proposition, Set<Proposition>> backwardDerivations, Map<UniqueId, Proposition> references)
            throws QueryResultsHandlerProcessingException {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
        PreparedStatement ps;

        LOGGER.info("Populating db table aou_participants:props size " + propositions.size());
        if (propositions != null) {
            for (Proposition prop : propositions) {
                AbstractProposition myProp = (AbstractProposition) prop;
                try (Connection conn = openDataDatabaseConnection()) {
                    conn.setAutoCommit(true);
                    ps = conn.prepareStatement("INSERT INTO COHORT_UF.AOU_PARTICIPANTS (AOU_PARTICIPANT_ID, EMPI_NBR, PATIENT_BIRTH_DT) VALUES (?,?,?)"); 
                    ps.setString(1, myProp.getProperty("PMID (AoU Participant ID)").getFormatted().substring(1));
                    ps.setString(2, myProp.getProperty("Emory Master Patient Index 1 (EMPI1)").getFormatted());
                    ps.setDate(3, new java.sql.Date(df.parse(myProp.getProperty("Birthdate").getFormatted()).getTime()));
                    ps.execute();
                } catch (SQLException ex) {
                    throw new QueryResultsHandlerProcessingException(ex);
                }
                catch (ParseException e) {
                    throw new QueryResultsHandlerProcessingException(e);
                }                
            }
        }
    }

    @Override
    public void finish() throws QueryResultsHandlerProcessingException {
        
    }

    @Override
    public void close() throws QueryResultsHandlerCloseException {
        try (Connection conn = openDataDatabaseConnection()){
            PreparedStatement ps;
            conn.setAutoCommit(true);
            String sql = "INSERT INTO COHORT_UF.UF_PATIENT_KEY (AOU_PARTICIPANT_ID, PATIENT_ID, PATIENT_KEY) " + 
                    "SELECT participants.AOU_PARTICIPANT_ID, patients.PATIENT_ID, patients.PATIENT_KEY " + 
                    "FROM EHCVW.LKP_PATIENT patients " + 
                    "JOIN EHCVW.LKP_PATIENT patients_current ON patients_current.current_record_ind=1 AND patients.patient_id=patients_current.patient_id " + 
                    "JOIN COHORT_UF.AOU_PARTICIPANTS participants ON patients_current.empi_nbr=participants.empi_nbr AND patients_current.patient_birth_dt=participants.patient_birth_dt";
            ps = conn.prepareStatement(sql);
            ps.execute();
        } catch (SQLException e) {
            throw new QueryResultsHandlerCloseException(e);
        }
    }
    
    private void clearTables() throws QueryResultsHandlerCloseException {
        LOGGER.debug("Clearing tables");
        String truncSql;
        PreparedStatement ps;
        try (Connection conn = openDataDatabaseConnection()) {
            conn.setAutoCommit(true);
            truncSql = "TRUNCATE TABLE COHORT_UF.AOU_PARTICIPANTS";
            ps = conn.prepareStatement(truncSql);
            ps.execute();
            truncSql = "TRUNCATE TABLE COHORT_UF.UF_PATIENT_KEY";
            ps = conn.prepareStatement(truncSql);
            ps.execute();
        } catch (SQLException ex) {
            throw new QueryResultsHandlerCloseException(ex);
        }
    }
    
    protected ConnectionSpec getConnectionSpecInstance(String databaseId)
            throws InvalidConnectionSpecArguments {
        return this.databaseAPI.newConnectionSpecInstance(
                databaseId, "", "", false);
    }
    
    private Connection openDataDatabaseConnection() throws SQLException {
        return this.dataConnectionSpec.getOrCreate();
    }

}
