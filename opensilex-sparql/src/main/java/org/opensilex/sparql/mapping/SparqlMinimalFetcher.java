package org.opensilex.sparql.mapping;

import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;

import java.lang.reflect.Constructor;
import java.net.URI;

/**
 * A minimal {@link SparqlMapper} which only retrieve {@link SPARQLResourceModel#URI_FIELD} and {@link SPARQLResourceModel#TYPE_FIELD}
 * @param <T> The type of {@link SPARQLResourceModel}
 *
 * @apiNote URI are formatted using the {@link URIDeserializer#formatURI(URI)} method
 * @author rcolin
 */
public class SparqlMinimalFetcher<T extends SPARQLResourceModel> implements SparqlMapper<T>{

    private final Constructor<T> constructor;

    public SparqlMinimalFetcher(Class<T> objectClass) throws Exception {
        this.constructor = objectClass.getConstructor();
    }
    @Override
    public T getInstance(SPARQLResult result, String lang) throws Exception {
        T instance = getConstructor().newInstance();
        instance.setUri(URIDeserializer.formatURI(result.getStringValue(SPARQLResourceModel.URI_FIELD)));
        instance.setType(URIDeserializer.formatURI(result.getStringValue(SPARQLResourceModel.TYPE_FIELD)));
        return instance;
    }

    @Override
    public Constructor<T> getConstructor() {
        return constructor;
    }
}
