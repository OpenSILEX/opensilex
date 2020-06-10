/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 * @author vince
 */
public interface ServiceDefinition {

    public Class<? extends Service> implementation();

    public JsonNode config();

    public static Class<? extends Service> getDefaultImplementation(Class<? extends Service> serviceClass) {
        ServiceDefaultDefinition defaultDefinition = serviceClass.getAnnotation(ServiceDefaultDefinition.class);
        if (defaultDefinition == null) {
            return null;
        }
        return defaultDefinition.implementation();
    }

    public static Class<? extends ServiceConfig> getDefaultConfigClass(Class<? extends Service> serviceClass) {
        ServiceDefaultDefinition defaultDefinition = serviceClass.getAnnotation(ServiceDefaultDefinition.class);
        if (defaultDefinition == null) {
            return null;
        }
        return defaultDefinition.config();
    }
}
