//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import java.lang.reflect.Method;
import java.net.URI;
import org.apache.jena.graph.Node;
import org.opensilex.sparql.exceptions.SPARQLMultipleObjectException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author vincent
 */
class SPARQLProxyResource<T extends SPARQLResourceModel> extends SPARQLProxy<T> {

    SPARQLProxyResource(SPARQLClassObjectMapperIndex repository, Node graph, URI uri, Class<T> type, String lang, SPARQLService service) throws Exception {
        super(repository, graph, type, lang, service);
        this.uri = uri;
        this.mapper = repository.getForClass(type);
    }

    protected final SPARQLClassObjectMapper<T> mapper;
    protected final URI uri;

    @Override
    protected T loadData() throws Exception {
        T data = null;
        if (graph != null) {
            data = service.loadByURI(graph, type, uri, lang);
            if (data == null) {
                try {
                    data = service.loadByURI(type, uri, lang);
                } catch (SPARQLMultipleObjectException ex) {
                }
            }
        } else {
            data = service.loadByURI(type, uri, lang);
        }

//        if (data == null) {
//            data = service.loadByURI(graph, type, uri, lang);
//        }
        return data;
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
