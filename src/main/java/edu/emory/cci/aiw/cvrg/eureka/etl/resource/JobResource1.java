/*
 * #%L
 * Eureka Protempa ETL
 * %%
 * Copyright (C) 2012 - 2013 Emory University
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
package edu.emory.cci.aiw.cvrg.eureka.etl.resource;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;


import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

import edu.emory.cci.aiw.cvrg.eureka.etl.dao.DestinationDao;
import edu.emory.cci.aiw.cvrg.eureka.etl.dao.AuthorizedUserDao;
import edu.emory.cci.aiw.cvrg.eureka.etl.config.EtlProperties;
import edu.emory.cci.aiw.cvrg.eureka.etl.dao.JobDao;
import edu.emory.cci.aiw.cvrg.eureka.etl.job.TaskManager;
import edu.emory.cci.aiw.cvrg.eureka.etl.dest.ProtempaDestinationFactory;
import edu.emory.cci.aiw.cvrg.eureka.etl.job.Task;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import org.eurekaclinical.common.comm.clients.ClientException;
import org.eurekaclinical.eureka.client.comm.Phenotype;
import org.eurekaclinical.eureka.client.comm.exception.PhenotypeHandlingException;
import org.eurekaclinical.phenotype.client.EurekaClinicalPhenotypeClient;
import edu.emory.cci.aiw.cvrg.eureka.etl.conversion.PropositionDefinitionCollector;
import edu.emory.cci.aiw.cvrg.eureka.etl.conversion.PropositionDefinitionConverterVisitor;
import org.eurekaclinical.standardapis.exception.HttpStatusException;
import org.protempa.PropositionDefinition;

@Path("/protected/jobs1")
@RolesAllowed({"researcher"})
@Consumes(MediaType.APPLICATION_JSON)
public class JobResource1 {

	private final JobDao jobDao;
	private final AuthorizedUserDao etlUserDao;
	private final TaskManager taskManager;
	private final DestinationDao destinationDao;
	private final ProtempaDestinationFactory protempaDestinationFactory;
	private final EtlProperties etlProperties;
	private final Provider<EntityManager> entityManagerProvider;
	private final Provider<Task> taskProvider;
        private final EurekaClinicalPhenotypeClient phenotypeClient;
        private final PropositionDefinitionConverterVisitor converterVisitor;
	@Inject
	public JobResource1(JobDao inJobDao, TaskManager inTaskManager,
			AuthorizedUserDao inEtlUserDao, DestinationDao inDestinationDao,
			EtlProperties inEtlProperties,
			ProtempaDestinationFactory inProtempaDestinationFactory,
			Provider<EntityManager> inEntityManagerProvider,
			Provider<Task> inTaskProvider,
                        EurekaClinicalPhenotypeClient inPhenotypeClient,
                        PropositionDefinitionConverterVisitor  inConverterVisitor 
                        ) {
		this.jobDao = inJobDao;
		this.taskManager = inTaskManager;
		this.etlUserDao = inEtlUserDao;
		this.destinationDao = inDestinationDao;
		this.etlProperties = inEtlProperties;
		this.protempaDestinationFactory = inProtempaDestinationFactory;
		this.entityManagerProvider = inEntityManagerProvider;
		this.taskProvider = inTaskProvider;
                this.phenotypeClient = inPhenotypeClient;
                this.converterVisitor = inConverterVisitor;
	}

	@Transactional
	//Finer grained transactions in the implementation
	@GET
	@Consumes({MediaType.APPLICATION_JSON})
        @Produces({MediaType.APPLICATION_JSON})
	public List<PropositionDefinition> submit(@Context HttpServletRequest request) {
                List<PropositionDefinition> propositionList;
                List<Phenotype> phenotypeList;
                System.out.println("Protempa /jobs proposition definitions");
                try{
                    phenotypeList = this.phenotypeClient.getUserPhenotypes(false);
                    this.converterVisitor.setAllCustomPhenotypes(phenotypeList);
                    PropositionDefinitionCollector collector
				= PropositionDefinitionCollector.getInstance(
						this.converterVisitor, phenotypeList);
                    propositionList = collector.getUserPropDefs();
                    
                }
                catch (ClientException ex) {
                    throw new HttpStatusException(Status.INTERNAL_SERVER_ERROR, ex);
		} 
                catch (PhenotypeHandlingException ex) {
                    
                    Logger.getLogger(JobResource1.class.getName()).log(Level.SEVERE, null, ex);
                    throw new HttpStatusException(Status.INTERNAL_SERVER_ERROR, ex);
                }
		return propositionList;
	}

}
