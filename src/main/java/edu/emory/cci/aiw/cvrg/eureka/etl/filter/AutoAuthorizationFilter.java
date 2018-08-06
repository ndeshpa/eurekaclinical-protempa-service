package edu.emory.cci.aiw.cvrg.eureka.etl.filter;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eurekaclinical.common.filter.AbstractAutoAuthorizationFilter;
import org.eurekaclinical.standardapis.dao.UserDao;
import org.eurekaclinical.standardapis.dao.UserTemplateDao;

import edu.emory.cci.aiw.cvrg.eureka.etl.entity.RoleEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.UserEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.UserTemplateEntity;


@Singleton
public class AutoAuthorizationFilter extends AbstractAutoAuthorizationFilter<RoleEntity, UserEntity, UserTemplateEntity>{
 
@Inject
    public AutoAuthorizationFilter(UserTemplateDao<UserTemplateEntity> inUserTemplateDao,
            UserDao<UserEntity> inUserDao) {
        super(inUserTemplateDao, inUserDao);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected UserEntity toUserEntity(UserTemplateEntity userTemplate, String username) {
        UserEntity user = new UserEntity();
        user.setUsername(username); 
        user.setRoles(userTemplate.getRoles());
        return user;

    } 

}
