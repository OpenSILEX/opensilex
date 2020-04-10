/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.mapping;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.service.SPARQLService;

/**
 *
 * @author vmigot
 */
class SPARQLProxyLabel extends SPARQLProxy<SPARQLLabel> {

    SPARQLProxyLabel(SPARQLClassObjectMapperIndex repository, Node graph, String defaultValue, URI resourceURI, Property labelProperty, boolean reverseRelation, String lang, SPARQLService service) {
        super(repository,graph, SPARQLLabel.class, lang, service);
        this.defaultValue = defaultValue;
        this.resourceURI = resourceURI;
        this.labelProperty = labelProperty;
        this.reverseRelation = reverseRelation;
    }

    private final URI resourceURI;
    private final Property labelProperty;
    private final boolean reverseRelation;
    private final String defaultValue;

    @Override
    protected SPARQLLabel loadData() throws Exception {
        Map<String, String> translations = service.getTranslations(graph, resourceURI, labelProperty, reverseRelation);
        translations.remove(lang);
        SPARQLLabel label = new SPARQLLabel(defaultValue, lang);
        label.setTranslations(translations);

        return label;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        boolean noParameters = (method.getParameterCount() == 0);
        if (method.getName().equals("getDefaultLang") && noParameters) {
            return this.lang;
        } else if (method.getName().equals("getDefaultValue") && noParameters) {
            return defaultValue;
        } else if (method.getName().equals("toString") && noParameters) {
            return defaultValue;
        } else {
            return super.invoke(proxy, method, args);
        }
    }

}
