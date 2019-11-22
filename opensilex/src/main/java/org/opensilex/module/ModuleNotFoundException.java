//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
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
