package org.opensilex.integration.test;

import java.lang.reflect.Method;

/**
 * Service description class for storing service method and path template.
 */
public class ServiceDescription {

    private final Method serviceMethod;
    private final String pathTemplate;

    public ServiceDescription(Method serviceMethod, String pathTemplate) {
        this.serviceMethod = serviceMethod;
        this.pathTemplate = pathTemplate;
    }

    public Method getServiceMethod() {
        return serviceMethod;
    }

    public String getPathTemplate() {
        return pathTemplate;
    }
}
