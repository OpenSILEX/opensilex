/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module;

/**
 *
 * @author Vincent Migot
 */
public class ModuleNotFoundException extends Exception {

    private final Class<? extends OpenSilexModule> moduleClass;
    
    public ModuleNotFoundException(Class<? extends OpenSilexModule> moduleClass) {
        this.moduleClass = moduleClass;
    }

    @Override
    public String getMessage() {
        return "Can't find module corresponding to class: " + moduleClass.getCanonicalName();
    }
    
}
