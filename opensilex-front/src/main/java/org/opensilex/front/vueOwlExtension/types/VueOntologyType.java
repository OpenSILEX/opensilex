/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

/**
 *
 * @author vmigot
 */
public interface VueOntologyType {

    public String getTypeUri();

    public default List<String> getTypeUriAliases() {
        return new ArrayList<>();
    }

    public String getInputComponent();

    public String getViewComponent();


    public default boolean isDisabled() {
        return false;
    }

}
