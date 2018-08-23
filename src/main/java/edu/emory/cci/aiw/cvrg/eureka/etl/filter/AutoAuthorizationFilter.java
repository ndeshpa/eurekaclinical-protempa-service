package edu.emory.cci.aiw.cvrg.eureka.etl.filter;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eurekaclinical.common.filter.AbstractAutoAuthorizationFilter;
import org.eurekaclinical.standardapis.dao.UserDao;
import org.eurekaclinical.standardapis.dao.UserTemplateDao;

import edu.emory.cci.aiw.cvrg.eureka.etl.entity.AuthorizedRoleEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.AuthorizedUserEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.UserTemplateEntity;

@Singleton
public class AutoAuthorizationFilter extends AbstractAutoAuthorizationFilter<AuthorizedRoleEntity, AuthorizedUserEntity, UserTemplateEntity> {

    @Inject
    public AutoAuthorizationFilter(UserTemplateDao<UserTemplateEntity> inUserTemplateDao,
            UserDao<AuthorizedUserEntity> inUserDao) {
        super(inUserTemplateDao, inUserDao);
    }

    @Override
    protected AuthorizedUserEntity toUserEntity(UserTemplateEntity userTemplate, String username) {
        AuthorizedUserEntity user = new AuthorizedUserEntity();
        user.setUsername(username);
        user.setRoles(userTemplate.getRoles());
        return user;

    }

}
