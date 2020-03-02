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

    SPARQLProxyLabel(Node graph, String defaultValue,  URI resourceURI, Property labelProperty, boolean reverseRelation, String lang, SPARQLService service) {
        super(graph, SPARQLLabel.class, lang, service);
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
        Map<String, String> translations = service.getOtherTranslations(resourceURI, labelProperty, reverseRelation, lang);
        this.instance.setTranslations(translations);
        this.instance.setDefaultLang(this.lang);
        this.instance.setDefaultValue(defaultValue);
        
        return this.instance;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("getDefaultLang")) {
            return this.lang;
        } else if (method.getName().equals("getDefaultValue")) {
            return defaultValue;
        } else {
            return super.invoke(proxy, method, args);
        }
    }
    
}
