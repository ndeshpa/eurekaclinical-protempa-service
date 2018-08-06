package edu.emory.cci.aiw.cvrg.eureka.etl.props;


import org.eurekaclinical.standardapis.props.CasJerseyEurekaClinicalProperties;


public class ProtempaServiceProperties extends CasJerseyEurekaClinicalProperties {
    
    public ProtempaServiceProperties() {
        super("/etc/ec-protempa-service");
    }
    
    @Override
    public String getProxyCallbackServer() {
        return getValue("eureka.etl.callbackserver");
    }

    @Override
    public String getUrl() {
        return getValue("eureka.etl.url");
    }
    
}