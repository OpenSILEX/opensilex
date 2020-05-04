/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.extensions;

import java.util.ArrayList;
import java.util.List;
import org.opensilex.OpenSilexExtension;

/**
 *
 * @author vince
 */
public interface SPARQLExtension extends OpenSilexExtension {

    public default List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        return new ArrayList<>();
    }

    public default void inMemoryInitialization() throws Exception {
    }

}
