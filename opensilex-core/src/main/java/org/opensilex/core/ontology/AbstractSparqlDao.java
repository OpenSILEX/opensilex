package org.opensilex.core.ontology;

import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;

public class AbstractSparqlDao<T extends SPARQLResourceModel> {

    protected final SPARQLService sparql;

    protected final Class<T> objectClass;

    public AbstractSparqlDao(Class<T> objectClass, SPARQLService sparql) {
        this.sparql = sparql;
        this.objectClass = objectClass;
    }

    public T create(T instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public T update(T instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(objectClass, instanceURI);
    }

    public T get(URI instanceURI) throws Exception {
        return sparql.getByURI(objectClass, instanceURI, null);
    }

}
