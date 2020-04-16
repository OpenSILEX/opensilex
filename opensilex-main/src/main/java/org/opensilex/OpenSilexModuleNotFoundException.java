//******************************************************************************
//                       OpenSilexModuleNotFoundException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex;

/**
 * Exception thrown when a module is not found based on it's class.
 *
 * @author Vincent Migot
 */
public class OpenSilexModuleNotFoundException extends Exception {

    /**
     * Class of the not found module.
     */
    private final Class<? extends OpenSilexModule> moduleClass;

    /**
     * Constructor based on module class.
     *
     * @param moduleClass Not found module class
     */
    public OpenSilexModuleNotFoundException(Class<? extends OpenSilexModule> moduleClass) {
        this.moduleClass = moduleClass;
    }

    /**
     * Return error message.
     *
     * @return error message
     */
    @Override
    public String getMessage() {
        return "Can't find module corresponding to class: " + moduleClass.getCanonicalName();
    }

}
