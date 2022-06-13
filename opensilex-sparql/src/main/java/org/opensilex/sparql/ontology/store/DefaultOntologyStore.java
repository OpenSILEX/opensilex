/*******************************************************************************
 *                         DefaultOntologyStore.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.store;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;

/**
 * @author rcolin
 * Default implementation with {@link PatriciaTrie} index and {@link SimpleDirectedGraph} graph
 */
public class DefaultOntologyStore extends AbstractOntologyStore {

    public DefaultOntologyStore(SPARQLService sparql, OpenSilex openSilex) throws OpenSilexModuleNotFoundException, SPARQLException {
        super(sparql, openSilex, new PatriciaTrie<>(), new SimpleDirectedGraph<>(DefaultEdge.class));
    }
}
