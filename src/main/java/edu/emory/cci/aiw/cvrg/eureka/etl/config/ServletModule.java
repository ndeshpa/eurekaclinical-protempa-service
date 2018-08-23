package edu.emory.cci.aiw.cvrg.eureka.etl.config;

import org.eurekaclinical.common.config.AbstractAuthorizingJerseyServletModuleWithPersist;
import edu.emory.cci.aiw.cvrg.eureka.etl.filter.AutoAuthorizationFilter;

public class ServletModule extends AbstractAuthorizingJerseyServletModuleWithPersist {

    public ServletModule(EtlProperties inProperties, String inPackageNames) {
        super(inProperties, inPackageNames, false);
    }

    
    @Override
    protected void setupFilters() {
        super.setupFilters();
        filter("/*").through(AutoAuthorizationFilter.class);
    }
}