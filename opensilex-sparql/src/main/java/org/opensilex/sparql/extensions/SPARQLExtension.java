/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.extensions;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vince
 */
public interface SPARQLExtension {

    public default List<OntologyFileDefinition> getOntologiesFiles() throws Exception{
        List<OntologyFileDefinition> list = new ArrayList<>();
        return list;
    }

}
