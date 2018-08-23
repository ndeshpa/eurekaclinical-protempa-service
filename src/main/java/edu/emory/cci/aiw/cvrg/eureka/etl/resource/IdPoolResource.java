package edu.emory.cci.aiw.cvrg.eureka.etl.resource;

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
import edu.emory.cci.aiw.cvrg.eureka.etl.dao.RoleDao;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.IdPoolEntity;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eurekaclinical.common.resource.AbstractNamedReadWriteResource;
import org.eurekaclinical.protempa.client.comm.IdPool;

/**
 *
 * @author Andrew Post
 */
@Transactional
@Path("/protected/idpools")
@Produces(MediaType.APPLICATION_JSON)
public class IdPoolResource extends AbstractNamedReadWriteResource<IdPoolEntity, IdPool> {

    @Inject
    public IdPoolResource(RoleDao inRoleDao) {
        super(inRoleDao, true);
    }
    
    @Override
    protected IdPoolEntity toEntity(IdPool commObj) {
        IdPoolEntity idPoolEntity = new IdPoolEntity();
        idPoolEntity.setId(commObj.getId());
        idPoolEntity.setName(commObj.getName());
        idPoolEntity.setDescription(commObj.getDescription());
        return idPoolEntity;
    }

    @Override
    protected boolean isAuthorizedComm(IdPool commObj, HttpServletRequest req) {
        return req.isUserInRole("admin");
    }

    @Override
    protected IdPool toComm(IdPoolEntity entity, HttpServletRequest req) {
        return entity.toIdPool();
    }

    @Override
    protected boolean isAuthorizedEntity(IdPoolEntity entity, HttpServletRequest req) {
        return req.isUserInRole("admin");
    }
    
}
