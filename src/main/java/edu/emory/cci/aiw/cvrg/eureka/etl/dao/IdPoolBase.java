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
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.IdPoolEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.IdPoolIdEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.pool.Pool;
import edu.emory.cci.aiw.cvrg.eureka.etl.pool.PoolException;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import org.protempa.proposition.value.NumberValue;
import org.protempa.proposition.value.Value;

/**
 *
 * @author Andrew Post
 */
public class IdPoolBase implements Pool {

    private static final int BATCH_SIZE = 10000;

    private final IdPoolIdDao idPoolIdDao;
    private IdPoolEntity idPool;
    private int recordsCreated;

    @Inject
    public IdPoolBase(Provider<IdPoolIdDao> inIdPoolIdDaoProvider) {
        assert inIdPoolIdDaoProvider != null : "inIdPoolIdDao cannot be null";
        this.idPoolIdDao = inIdPoolIdDaoProvider.get();
    }

    IdPoolEntity getIdPool() {
        return idPool;
    }

    void setIdPool(IdPoolEntity idPool) {
        this.idPool = idPool;
    }

    @Override
    public void start() throws PoolException {
        this.recordsCreated = 0;
        this.idPoolIdDao.startTransaction();
    }

    @Override
    public Value valueFor(Value inValue) throws PoolException {
        String valueStr;
        if (inValue != null) {
            valueStr = inValue.getFormatted();
        } else {
            valueStr = null;
        }
        try {
            Long result = this.idPoolIdDao.getByPoolIdAndFromId(this.idPool.getId(), valueStr);
            if (result == null) {
                IdPoolIdEntity newEntity = new IdPoolIdEntity();
                newEntity.setFromId(valueStr);
                newEntity.setIdPool(this.idPool);
                result = this.idPoolIdDao.create(newEntity).getId();
                this.idPoolIdDao.flush();
                this.recordsCreated++;
                if (this.recordsCreated % BATCH_SIZE == 0) {
                    this.idPoolIdDao.commitTransaction();
                    this.idPoolIdDao.startTransaction();
                }
            }
            return NumberValue.getInstance(result);
        } catch (PersistenceException ex) {
            PoolException pe = new PoolException("Error reading/writing id pool", ex);
            try {
                this.idPoolIdDao.rollbackTransaction();
            } catch (PersistenceException ex2) {
                pe.addSuppressed(ex2);
            }
            throw pe;
        }
    }

    @Override
    public void finish() throws PoolException {
        boolean active;
        try {
            active = this.idPoolIdDao.isInTransaction();
        } catch (PersistenceException ex) {
            throw new PoolException("Error finishing writes to the pool", ex);
        }
        if (active) {
            try {
                this.idPoolIdDao.commitTransaction();
            } catch (RollbackException ex) {
                PoolException pe = new PoolException("Error finishing writes to the pool", ex);
                try {
                    this.idPoolIdDao.rollbackTransaction();
                } catch (PersistenceException ex2) {
                    pe.addSuppressed(ex2);
                }
                throw pe;
            }
        }
    }

    @Override
    public int getRecordsCreated() {
        return this.recordsCreated;
    }

    @Override
    public void close() throws Exception {
        try {
            if (this.idPoolIdDao.isInTransaction()) {
                this.idPoolIdDao.rollbackTransaction();
            }
        } catch (PersistenceException ex) {
            throw new PoolException("Error closing the pool", ex);
        }
    }

}
