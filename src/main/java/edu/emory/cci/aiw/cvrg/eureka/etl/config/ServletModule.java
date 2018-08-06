package edu.emory.cci.aiw.cvrg.eureka.etl.config;

import org.eurekaclinical.common.config.AbstractAuthorizingJerseyServletModuleWithPersist;
import edu.emory.cci.aiw.cvrg.eureka.etl.filter.AutoAuthorizationFilter;
import edu.emory.cci.aiw.cvrg.eureka.etl.props.ProtempaServiceProperties;

public class ServletModule extends AbstractAuthorizingJerseyServletModuleWithPersist {

    private static final String PACKAGE_NAMES = "edu.emory.cci.aiw.cvrg.eureka.etl.resource";

    public ServletModule(ProtempaServiceProperties inProperties) {
        super(inProperties, PACKAGE_NAMES, false);
    }

    
    @Override
    protected void setupFilters() {
        super.setupFilters();
        filter("/*").through(AutoAuthorizationFilter.class);
    }
}