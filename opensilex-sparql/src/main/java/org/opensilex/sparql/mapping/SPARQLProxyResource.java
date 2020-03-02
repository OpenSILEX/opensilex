//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import java.net.URI;
import org.apache.jena.graph.Node;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.model.SPARQLResourceModel;


/**
 *
 * @author vincent
 */
class SPARQLProxyResource<T extends SPARQLResourceModel> extends SPARQLProxy<T> {
    
    
    SPARQLProxyResource(Node graph, URI uri, Class<T> type, String lang, SPARQLService service) {
        super(graph, type, lang, service);
        this.uri = uri;
    }
    
    protected final URI uri;

    @Override
    protected T loadData() throws Exception {
        return service.loadByURI(type, uri, lang);
    }

}
