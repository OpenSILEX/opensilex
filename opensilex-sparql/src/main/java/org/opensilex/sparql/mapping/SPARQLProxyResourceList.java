//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import org.apache.jena.graph.Node;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author vincent
 */
class SPARQLProxyResourceList<T extends SPARQLResourceModel> extends SPARQLProxy<List> {

    SPARQLProxyResourceList(SPARQLClassObjectMapperIndex repository, Node graph, Collection<URI> uris, Class<T> genericType, String lang, SPARQLService service) {
        super(repository,graph, List.class, lang, service);
        this.uris = uris;
        this.genericType = genericType;
    }

    protected final Class<T> genericType;
    protected final Collection<URI> uris;

    @Override
    protected List<T> loadData() throws Exception {
        return service.loadListByURIs(graph, genericType, uris, lang,null,null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> getInstance() {
        return super.getInstance();
    }

}
