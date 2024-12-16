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
 * @apiNote this Fetcher set {@link #useFormattedUri()} to true
 * @author rcolin
 */
public class SparqlMinimalFetcher<T extends SPARQLResourceModel> implements SparqlMapper<T>{

    private final Constructor<T> constructor;

    public SparqlMinimalFetcher(Class<T> objectClass) {
        Objects.requireNonNull(objectClass);
        try {
            this.constructor = objectClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public T getInstance(SPARQLResult result, String lang){
        try {
            T instance = getConstructor().newInstance();
            setUriAndType(instance, result, lang);
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean useFormattedUri() {
        return true;
    }

    @Override
    public Constructor<T> getConstructor() {
        return constructor;
    }


}
