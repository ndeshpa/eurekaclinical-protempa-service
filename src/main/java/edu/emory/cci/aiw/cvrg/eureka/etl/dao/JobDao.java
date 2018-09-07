package edu.emory.cci.aiw.cvrg.eureka.etl.dao;

/*-
 * #%L
 * Eureka! Clinical Protempa Service
 * %%
 * Copyright (C) 2012 - 2018 Emory University
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


import java.util.List;

import edu.emory.cci.aiw.cvrg.eureka.etl.entity.JobEntity;
import org.eurekaclinical.eureka.client.comm.JobFilter;
import org.eurekaclinical.standardapis.dao.Dao;

/**
 * A data access object interface to retrieve and store information about
 * Protempa ETL jobs.
 *
 * @author hrathod
 *
 */
public interface JobDao extends Dao<JobEntity, Long> {

    /**
     * Gets a list of jobs that meet the given filter criteria.
     *
     * @param jobFilter The filter criteria.
     * @return A list of jobs that meet the filter criteria.
     */
    List<JobEntity> getWithFilter(JobFilter jobFilter);

    List<JobEntity> getWithFilterDesc(JobFilter jobFilter);

    List<JobEntity> getLatestWithFilter(JobFilter jobFilter);
    
    void startTransaction();
    
    void commitTransaction();
    
    void rollbackTransaction();

}
