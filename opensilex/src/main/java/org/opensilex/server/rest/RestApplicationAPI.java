/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.rest;

import org.opensilex.OpenSilex;

/**
 *
 * @author Vincent Migot
 */
public interface RestApplicationAPI {
    
    public final static String DEFAULT_PAGE_SIZE = "20";
    public final static String DEFAULT_PAGE = "0";
    
    public static final String PARAM_PAGE_SIZE = "pageSize";
    public static final String PARAM_PAGE = "page";
    
    public static final String RESPONSE_MESSAGE_200 = "Retrieve a resource.";
    public static final String RESPONSE_MESSAGE_400 = "Bad informations send by user.";
    public static final String RESPONSE_MESSAGE_401 = "Access denied.";
    public static final String RESPONSE_MESSAGE_500 = "Server error.";

    public default boolean isTestEnabled() {
        return true;
    }
    
    public default boolean isDevEnabled() {
        return true;
    }
    
    public default boolean isProdEnabled() {
        return true;
    }
    
    public default boolean isProfileEnabled(String profileId) {
        switch(profileId) {
            case OpenSilex.PROD_PROFILE_ID:
                return isProdEnabled();
            case OpenSilex.TEST_PROFILE_ID:
                return isTestEnabled();
            case OpenSilex.DEV_PROFILE_ID:
                return isDevEnabled();
            default:
                return isProdEnabled();
        }
    }
}
