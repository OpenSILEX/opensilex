package org.opensilex.sparql.mapping;

import org.opensilex.server.rest.serialization.uri.UriFormater;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;

import java.lang.reflect.Constructor;
import java.net.URI;

public interface SparqlMapper<T extends SPARQLResourceModel> {

    /**
     * // #TODO all mapper should use formatted URI
     */
    default boolean useFormattedUri(){
        return false;
    }

    default T getInstance(SPARQLResult result, String lang) throws Exception{

        T instance = getConstructor().newInstance();

        setUriAndType(instance, result, lang);
        setLabel(instance,result,lang);
        setLabelProperties(instance,result,lang);
        setDataProperties(instance,result,lang);
        setObjectProperties(instance,result,lang);
        setDataListProperties(instance,result,lang);
        setObjectListProperties(instance,result,lang);

        return instance;
    }

    default void setUriAndType(T instance, SPARQLResult result, String lang){
        String uri = result.getStringValue(SPARQLResourceModel.URI_FIELD);
        String type = result.getStringValue(SPARQLResourceModel.TYPE_FIELD);

        instance.setUri(useFormattedUri() ? UriFormater.formatURI(uri) : URI.create(uri));
        instance.setType(useFormattedUri() ? UriFormater.formatURI(type) : URI.create(type));
    }

    default void setLabel(T instance, SPARQLResult result, String lang) throws Exception {

    }

    default void setLabelProperties(T instance, SPARQLResult result, String lang) throws Exception {
    }

    default void setDataProperties(T instance, SPARQLResult result, String lang) throws Exception {
    }

    default void setObjectProperties(T model, SPARQLResult result, String lang) throws Exception {
    }

    default void setDataListProperties(T instance, SPARQLResult result, String lang) throws Exception {
    }

    default void setObjectListProperties(T model, SPARQLResult result, String lang) throws Exception {
    }

    Constructor<T> getConstructor();

}
