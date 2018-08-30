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
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.IdPoolEntity_;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.IdPoolIdEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.IdPoolIdEntity_;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.eurekaclinical.standardapis.dao.GenericDao;

/**
 *
 * @author Andrew Post
 */
public class JpaIdPoolIdDao extends GenericDao<IdPoolIdEntity, Long> implements IdPoolIdDao {

    private EntityTransaction transaction;

    @Inject
    public JpaIdPoolIdDao(Provider<EntityManager> inManagerProvider) {
        super(IdPoolIdEntity.class, inManagerProvider);
    }

    @Override
    public List<IdPoolIdEntity> getAllByPoolId(Long inPoolId) {
        EntityManager entityManager = this.getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<IdPoolIdEntity> criteriaQuery = builder.createQuery(getEntityClass());
        Root<IdPoolIdEntity> root = criteriaQuery.from(getEntityClass());
        criteriaQuery.where(builder
                .equal(root.join(IdPoolIdEntity_.idPool).get(IdPoolEntity_.id), inPoolId));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    @Override
    public List<IdPoolIdEntity> getAllByPoolId(Long inPoolId, int inFirstResult, int inMaxResults) {
        EntityManager entityManager = this.getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<IdPoolIdEntity> criteriaQuery = builder.createQuery(getEntityClass());
        Root<IdPoolIdEntity> root = criteriaQuery.from(getEntityClass());
        criteriaQuery.where(builder
                .equal(root.join(IdPoolIdEntity_.idPool).get(IdPoolEntity_.id), inPoolId));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    @Override
    public List<IdPoolIdEntity> getAllByPoolName(String inPoolName) {
        EntityManager entityManager = this.getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<IdPoolIdEntity> criteriaQuery = builder.createQuery(getEntityClass());
        Root<IdPoolIdEntity> root = criteriaQuery.from(getEntityClass());
        criteriaQuery.where(builder
                .equal(root.join(IdPoolIdEntity_.idPool).get(IdPoolEntity_.name), inPoolName));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    @Override
    public List<IdPoolIdEntity> getAllByPoolName(String inPoolName, int inFirstResult, int inMaxResults) {
        EntityManager entityManager = this.getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<IdPoolIdEntity> criteriaQuery = builder.createQuery(getEntityClass());
        Root<IdPoolIdEntity> root = criteriaQuery.from(getEntityClass());
        criteriaQuery.where(builder
                .equal(root.join(IdPoolIdEntity_.idPool).get(IdPoolEntity_.name), inPoolName));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    @Override
    public Long getByPoolNameAndFromId(String inPoolName, String inFromId) {
        EntityManager entityManager = this.getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
        Root<IdPoolIdEntity> root = criteriaQuery.from(getEntityClass());
        criteriaQuery.where(builder.and(
                builder.equal(root.join(IdPoolIdEntity_.idPool).get(IdPoolEntity_.name), inPoolName),
                builder.equal(root.get(IdPoolIdEntity_.fromId), inFromId)));
        criteriaQuery.select(root.get(IdPoolIdEntity_.id));
        try {
            return entityManager.createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public Long getByPoolIdAndFromId(Long inPoolId, String inFromId) {
        EntityManager entityManager = this.getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
        Root<IdPoolIdEntity> root = criteriaQuery.from(getEntityClass());
        criteriaQuery.where(builder.and(
                builder.equal(root.join(IdPoolIdEntity_.idPool).get(IdPoolEntity_.name), inPoolId),
                builder.equal(root.get(IdPoolIdEntity_.fromId), inFromId)));
        criteriaQuery.select(root.get(IdPoolIdEntity_.id));
        try {
            return entityManager.createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }
    
    @Override
    public void startTransaction() {
        this.transaction = getEntityManager().getTransaction();
        this.transaction.begin();
    }

    @Override
    public void commitTransaction() {
        this.transaction.commit();
        this.transaction = null;
    }

    @Override
    public void rollbackTransaction() {
        this.transaction.rollback();
        this.transaction = null;
    }
    
    @Override
    public boolean isInTransaction() {
        return this.transaction != null && this.transaction.isActive();
    }

}
