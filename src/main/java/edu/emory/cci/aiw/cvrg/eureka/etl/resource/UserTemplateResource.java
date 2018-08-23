package edu.emory.cci.aiw.cvrg.eureka.etl.resource;

/*-
 * #%L
 * Eureka! Clinical User Agreement Service
 * %%
 * Copyright (C) 2016 - 2018 Emory University
 * %%
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
 * #L%
 */


import com.google.inject.persist.Transactional;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

import org.eurekaclinical.common.resource.AbstractUserTemplateResource;
import org.eurekaclinical.standardapis.dao.RoleDao;
import org.eurekaclinical.standardapis.dao.UserTemplateDao;
import org.eurekaclinical.protempa.client.comm.ProtempaUserTemplate;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.UserTemplateEntity;

import edu.emory.cci.aiw.cvrg.eureka.etl.entity.AuthorizedRoleEntity;

/**
 *
 * @author Dileep Gunda
 */
@Path("/protected/usertemplates")
@Transactional
public class UserTemplateResource extends AbstractUserTemplateResource<ProtempaUserTemplate, AuthorizedRoleEntity, UserTemplateEntity> {

    private final RoleDao<AuthorizedRoleEntity> roleDao;

    @Inject
    public UserTemplateResource(UserTemplateDao<UserTemplateEntity> inUserDao, RoleDao<AuthorizedRoleEntity> inRoleDao) {
        super(inUserDao);
        this.roleDao = inRoleDao;
    }

    @Override
    protected ProtempaUserTemplate toComm(UserTemplateEntity templateEntity, HttpServletRequest req) {
        ProtempaUserTemplate template = new ProtempaUserTemplate();
        template.setId(templateEntity.getId());
        template.setName(templateEntity.getName());
        List<Long> roles = new ArrayList<>();
        for (AuthorizedRoleEntity roleEntity : templateEntity.getRoles()) {
            roles.add(roleEntity.getId());
        }
        template.setRoles(roles);
        
        template.setAutoAuthorize(templateEntity.isAutoAuthorize());
        template.setCriteria(templateEntity.getCriteria());
        return template;
    }

    @Override
    protected UserTemplateEntity toEntity(ProtempaUserTemplate template) {
        UserTemplateEntity templateEntity = new UserTemplateEntity();
        templateEntity.setId(template.getId());
        templateEntity.setName(template.getName());
        List<AuthorizedRoleEntity> roleEntities = this.roleDao.getAll();
        for (Long roleId : template.getRoles()) {
            for (AuthorizedRoleEntity roleEntity : roleEntities) {
                if (roleEntity.getId().equals(roleId)) {
                    templateEntity.addRole(roleEntity);
                }
            }
        }
        
        templateEntity.setAutoAuthorize(template.isAutoAuthorize());
        templateEntity.setCriteria(template.getCriteria());
        return templateEntity;
    }

}
