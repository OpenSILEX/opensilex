package org.opensilex.sparql.mapping;

import org.apache.jena.graph.Node;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.lang.reflect.Method;
import java.net.URI;

/**
 * A specialisation of a {@link SPARQLProxyResource} for retrieving a {@link SparqlProxyNamedResource#name}
 *
 * @author Vincent Migot
 * @author Renaud COLIN
 */
class SparqlProxyNamedResource<T extends SPARQLNamedResourceModel<T>> extends SPARQLProxyResource<T> {

    protected final String name;

    SparqlProxyNamedResource(SPARQLClassObjectMapperIndex repository, Node graph, URI uri, Class<T> type, String name, String lang, boolean useDefaultGraph, SPARQLService service) throws Exception {
        super(repository, graph, uri, type, lang, useDefaultGraph, service);
        this.name = name;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("getName".equals(method.getName()) && method.getParameterCount() == 0 && name != null) {
            return name;
        } else {
            return super.invoke(proxy, method, args);
        }
    }

}
