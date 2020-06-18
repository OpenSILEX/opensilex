package org.opensilex.core.core;

import org.opensilex.sparql.model.SPARQLResourceModel;

import java.net.URI;

public interface SparqlDao<T extends SPARQLResourceModel> {

    T create(T instance) throws Exception;

    T update(T instance) throws Exception;

    void delete(URI instanceURI) throws Exception;

    T get(URI instanceURI) throws Exception;
}
