package org.opensilex.sparql.mapping;

import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Objects;

/**
 * A minimal {@link SparqlMapper} which only retrieve {@link SPARQLResourceModel#URI_FIELD} and {@link SPARQLResourceModel#TYPE_FIELD}
 * @param <T> The type of {@link SPARQLResourceModel}
 *
 * @apiNote URI are formatted using the {@link URIDeserializer#formatURI(URI)} method
 * @author rcolin
 */
public class SparqlMinimalFetcher<T extends SPARQLResourceModel> implements SparqlMapper<T>{

    private final Constructor<T> constructor;

    public SparqlMinimalFetcher(Class<T> objectClass) throws NoSuchMethodException {
        Objects.requireNonNull(objectClass);
        this.constructor = objectClass.getConstructor();

    }
    @Override
    public T getInstance(SPARQLResult result, String lang){
        try {
            T instance = getConstructor().newInstance();
            instance.setUri(URIDeserializer.formatURI(result.getStringValue(SPARQLResourceModel.URI_FIELD)));
            instance.setType(URIDeserializer.formatURI(result.getStringValue(SPARQLResourceModel.TYPE_FIELD)));
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Constructor<T> getConstructor() {
        return constructor;
    }


}
