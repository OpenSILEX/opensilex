/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.types;

import java.util.*;

/**
 * Interface used in order to dynamically associate some VueJs component with a type.
 *
 * @author vmigot
 */
public interface VueOntologyType {

    /**
     * @return a String representation of the URI associated with this Vue
     */
    String getTypeUri();

    /**
     * @return the {@link List} of all type(s) which can be handled with this VueOntologyType
     * @apiNote This default implementation return {@link Collections#emptyList()}
     */
    default List<String> getTypeUriAliases() {
        return Collections.emptyList();
    }

    /**
     * @return the id of the Vue component used for input
     */
    String getInputComponent();

    /**
     * @return the id of the Vue component used for view
     */
    String getViewComponent();

    default boolean isDisabled() {
        return false;
    }

}
