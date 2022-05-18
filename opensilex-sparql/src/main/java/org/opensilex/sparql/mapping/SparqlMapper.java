package org.opensilex.sparql.mapping;

import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;

import java.lang.reflect.Constructor;
import java.net.URI;

public interface SparqlMapper<T extends SPARQLResourceModel> {

    default T getInstance(SPARQLResult result, String lang) throws Exception{

        T instance = getConstructor().newInstance();

        instance.setUri(URI.create(result.getStringValue(SPARQLResourceModel.URI_FIELD)));
        instance.setType(URI.create(result.getStringValue(SPARQLResourceModel.TYPE_FIELD)));

        setLabel(instance,result,lang);
        setLabelProperties(instance,result,lang);
        setDataProperties(instance,result,lang);
        setObjectProperties(instance,result,lang);
        setDataListProperties(instance,result,lang);
        setObjectListProperties(instance,result,lang);

        return instance;
    }

    void setLabel(T instance, SPARQLResult result, String lang) throws Exception;

    void setLabelProperties(T instance, SPARQLResult result, String lang) throws Exception;

    void setDataProperties(T instance, SPARQLResult result, String lang) throws Exception;

    void setObjectProperties(T model, SPARQLResult result, String lang) throws Exception;

    void setDataListProperties(T instance, SPARQLResult result, String lang) throws Exception;

    void setObjectListProperties(T model, SPARQLResult result, String lang) throws Exception;

    Constructor<T> getConstructor();

}
