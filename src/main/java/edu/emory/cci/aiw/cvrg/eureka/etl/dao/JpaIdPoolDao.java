/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.IdPoolEntity_;
import edu.emory.cci.aiw.cvrg.eureka.etl.pool.PoolException;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import org.eurekaclinical.standardapis.dao.GenericDao;

/**
 *
 * @author Andrew Post
 */
public class JpaIdPoolDao extends GenericDao<IdPoolEntity, Long> implements IdPoolDao {

    private final Provider<IdPoolIdDao> idPoolIdDaoProvider;
    private final IdPoolBase idPoolBase;

    @Inject
    public JpaIdPoolDao(Provider<EntityManager> inManagerProvider, Provider<IdPoolIdDao> inIdPoolIdDaoProvider) {
        super(IdPoolEntity.class, inManagerProvider);
        this.idPoolIdDaoProvider = inIdPoolIdDaoProvider;
        this.idPoolBase = new IdPoolBase(this.idPoolIdDaoProvider);
    }
    
    @Override
    public void start() throws PoolException {
        this.idPoolBase.start();
    }

    @Override
    public IdPoolEntity getByName(String inName) {
        return getUniqueByAttribute(IdPoolEntity_.name, inName);
    }
    
    public IdPool getIdPool(Long inId) {
        IdPoolEntity idPoolEntity = retrieve(inId);
        if (idPoolEntity != null) {
            return new IdPool(idPoolEntity, this.idPoolBase, this.idPoolIdDaoProvider);
        } else {
            return null;
        }
    }

    public IdPool getIdPoolByName(String inName) {
        IdPoolEntity idPoolEntity = getByName(inName);
        if (idPoolEntity != null) {
            return new IdPool(idPoolEntity, this.idPoolBase, this.idPoolIdDaoProvider);
        } else {
            return null;
        }
    }
    
    @Override
    public IdPool toIdPool(IdPoolEntity inIdPoolEntity) {
        if (inIdPoolEntity != null) {
            return new IdPool(inIdPoolEntity, this.idPoolBase, this.idPoolIdDaoProvider);
        } else {
            return null;
        }
    }
    
    @Override
    public void finish() throws PoolException {
        this.idPoolBase.finish();
    }
    
    @Override
    public void close() throws Exception {
        this.idPoolBase.close();
    }
    
}
