/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.rest;

import org.opensilex.OpenSilex;

/**
 *
 * @author vincent
 */
public interface RestApplicationAPI {

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
