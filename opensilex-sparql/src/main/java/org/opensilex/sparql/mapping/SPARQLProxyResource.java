//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import java.lang.reflect.Method;
import java.net.URI;
import org.apache.jena.graph.Node;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author vincent
 */
class SPARQLProxyResource<T extends SPARQLResourceModel> extends SPARQLProxy<T> {

    SPARQLProxyResource(Node graph, URI uri, Class<T> type, String lang, SPARQLService service) throws Exception {
        super(graph, type, lang, service);
        this.uri = uri;
        this.mapper = SPARQLClassObjectMapper.getForClass(type);
    }

    protected final SPARQLClassObjectMapper<T> mapper;
    protected final URI uri;

    @Override
    protected T loadData() throws Exception {
        return service.loadByURI(graph, type, uri, lang);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals(mapper.getURIMethod().getName())) {
            return uri;
        } else {
            return super.invoke(proxy, method, args);
        }
    }

}
