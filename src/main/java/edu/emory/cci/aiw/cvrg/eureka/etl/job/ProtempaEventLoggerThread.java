package edu.emory.cci.aiw.cvrg.eureka.etl.job;

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
import com.google.inject.persist.Transactional;
import com.google.inject.persist.UnitOfWork;
import edu.emory.cci.aiw.cvrg.eureka.etl.dao.JobDao;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.JobEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.JobEventEntity;
import java.util.concurrent.BlockingQueue;
import javax.inject.Inject;
import javax.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Andrew Post
 */
public class ProtempaEventLoggerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtempaEventLoggerThread.class);
    private final Provider<JobDao> jobDaoProvider;
    private BlockingQueue<JobEventEntity> jobEvents;
    private Long jobId;
    private final UnitOfWork unitOfWork;
    private JobEventEntity poisonPill;

    @Inject
    public ProtempaEventLoggerThread(UnitOfWork inUnitOfWork, Provider<JobDao> inJobDaoProvider) {
        super("protempa.ProtempaEventLoggerThread");
        this.unitOfWork = inUnitOfWork;
        this.jobDaoProvider = inJobDaoProvider;
    }

    Long getJobId() {
        return jobId;
    }

    void setJobId(Long inJobId) {
        this.jobId = inJobId;
    }

    BlockingQueue<JobEventEntity> getJobEvents() {
        return jobEvents;
    }

    void setJobEvents(BlockingQueue<JobEventEntity> jobEvents) {
        this.jobEvents = jobEvents;
    }

    public JobEventEntity getPoisonPill() {
        return poisonPill;
    }

    public void setPoisonPill(JobEventEntity poisonPill) {
        this.poisonPill = poisonPill;
    }
    
    @Override
    public void run() {
        this.unitOfWork.begin();
        try {
            JobDao jobDao = this.jobDaoProvider.get();
            JobEntity job = jobDao.retrieve(this.jobId);
            assert job != null : "job cannot be null";
            JobEventEntity jobEvent;
            while (!isInterrupted() && ((jobEvent = this.jobEvents.take()) != this.poisonPill)) {
                assert jobEvent != null : "jobEvent cannot be null";
                updateJob(jobDao, job, jobEvent);
            }
        } catch (InterruptedException ex) {
            LOGGER.info("ProtempaEventLoggerThread was interrupted", ex);
        } finally {
            this.unitOfWork.end();
        }
    }

    @Transactional
    public void updateJob(JobDao jobDao, JobEntity job, JobEventEntity jobEvent) {
        JobEntity refreshedJob = jobDao.refresh(job);
        assert refreshedJob != null : "refreshedJob cannot be null";
        refreshedJob.addJobEvent(jobEvent);
        jobDao.update(refreshedJob);
    }

}
