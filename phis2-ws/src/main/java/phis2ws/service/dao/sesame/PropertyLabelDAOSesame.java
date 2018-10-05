//******************************************************************************
//                                       PropertyLabelDAOSesame.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 13 sept. 2018
// Contact: vincent.migot@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import static phis2ws.service.dao.sesame.PropertyDAOSesame.LOGGER;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Skos;
import phis2ws.service.resources.dto.PropertiesDTO;
import phis2ws.service.resources.dto.PropertyLabelsDTO;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;

/**
 * This class is a DAO for an uri and associated properties 
 * with their labels in a given language.
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class PropertyLabelDAOSesame extends PropertyDAOSesame {
    
    // Language in which label will be filtrered, "en" by default
    public String language = "en";
    
    protected final String PROPERTY_TYPE = "propertyType";
    protected final String RELATION_LABEL = "relationLabel";    
    protected final String PROPERTY_LABEL = "propertyLabel";    
    protected final String PROPERTY_TYPE_LABEL = "propertyTypeLabel";   
    protected final String RELATION_PREF_LABEL = "relationPrefLabel";    
    protected final String PROPERTY_PREF_LABEL = "propertyPrefLabel";    
    protected final String PROPERTY_TYPE_PREF_LABEL = "propertyTypePrefLabel";   
    
    
    /**
     * Prepare the sparql query to get the list of properties and their relations
     * to the given uri. If subClassOf is specified, the object corresponding to the uri must be
     * a subclass of the given type.
     * @return the builded query
     * @example
     * SELECT DISTINCT  
     *     ?relation ?relationPrefLabel ?relationLabel 
     *     ?property ?propertyPrefLabel ?propertyLabel 
     *     ?propertyType ?propertyTypePrefLabel ?propertyTypeLabel 
     * WHERE {
     *     <http://www.phenome-fppn.fr>  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *     ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.phenome-fppn.fr/vocabulary/2017#Infrastructure>  . 
     *     OPTIONAL {
     *         <http://www.phenome-fppn.fr> ?relation ?property 
     *         OPTIONAL {
     *             ?property <http://www.w3.org/2000/01/rdf-schema#label> ?propertyLabel . 
     *             FILTER(LANG(?propertyLabel) = "" || LANGMATCHES(LANG(?propertyLabel), "en"))
     *         } OPTIONAL { 
     *             ?property <http://www.w3.org/2008/05/skos#prefLabel> ?propertyPrefLabel . 
     *             FILTER(LANG(?propertyPrefLabel) = "" || LANGMATCHES(LANG(?propertyPrefLabel), "en"))
     *         } OPTIONAL {
     *             ?relation <http://www.w3.org/2000/01/rdf-schema#label> ?relationLabel . 
     *             FILTER(LANG(?relationLabel) = "" || LANGMATCHES(LANG(?relationLabel), "en"))
     *         } OPTIONAL { 
     *             ?relation <http://www.w3.org/2008/05/skos#prefLabel> ?relationPrefLabel . 
     *             FILTER(LANG(?relationPrefLabel) = "" || LANGMATCHES(LANG(?relationPrefLabel), "en"))
     *         } OPTIONAL {
     *             ?property <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?propertyType . 
     *             OPTIONAL {
     *                 ?propertyType <http://www.w3.org/2000/01/rdf-schema#label> ?propertyTypeLabel . 
     *                 FILTER(LANG(?propertyTypeLabel) = "" || LANGMATCHES(LANG(?propertyTypeLabel), "en"))
     *             }
     *             OPTIONAL { 
     *                 ?propertyType <http://www.w3.org/2008/05/skos#prefLabel> ?propertyTypePrefLabel . 
     *                 FILTER(LANG(?propertyTypePrefLabel) = "" || LANGMATCHES(LANG(?propertyTypePrefLabel), "en"))
     *             }
     *         } 
     *     } 
     * }
     */
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        query.appendSelect("?" + RELATION);
        query.appendSelect("?" + RELATION_PREF_LABEL);
        query.appendSelect("?" + RELATION_LABEL);
        query.appendSelect("?" + PROPERTY);
        query.appendSelect("?" + PROPERTY_PREF_LABEL);
        query.appendSelect("?" + PROPERTY_LABEL);
        query.appendSelect("?" + PROPERTY_TYPE);
        query.appendSelect("?" + PROPERTY_TYPE_PREF_LABEL);
        query.appendSelect("?" + PROPERTY_TYPE_LABEL);
        
        // 1. Select every relation and property linked to the given uri
        String optional = "<" + uri + "> ?" + RELATION + " ?" + PROPERTY;
        // 2. Select property label in the requested language if exists
        optional +=" OPTIONAL {";
        optional += "?" + PROPERTY + " <" + Rdfs.RELATION_LABEL + "> ?" + PROPERTY_LABEL;
        optional += " . FILTER(LANG(?" + PROPERTY_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + PROPERTY_LABEL + "), \"" + language + "\"))";   
        optional += "}";
        // 3. Select property prefered label in the requested language if exists
        optional += " OPTIONAL {";
        optional += " ?" + PROPERTY + " <" + Skos.RELATION_PREF_LABEL + "> ?" + PROPERTY_PREF_LABEL;
        optional += " . FILTER(LANG(?" + PROPERTY_PREF_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + PROPERTY_PREF_LABEL + "), \"" + language + "\"))";
        optional += "}";
        // 4. Select relation label in the requested language if exists
        optional += " OPTIONAL {";
        optional += "?" + RELATION + " <" + Rdfs.RELATION_LABEL + "> ?" + RELATION_LABEL;
        optional += " . FILTER(LANG(?" + RELATION_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + RELATION_LABEL + "), \"" + language + "\"))";
        optional += "}"; 
        // 5. Select relation prefered label in the requested language if exists
        optional += " OPTIONAL {";
        optional += " ?" + RELATION + " <" + Skos.RELATION_PREF_LABEL + "> ?" + RELATION_PREF_LABEL;
        optional += " . FILTER(LANG(?" + RELATION_PREF_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + RELATION_PREF_LABEL + "), \"" + language + "\"))";        
        optional += "}";
        // 6. Select property type (rdf type of the property) if exists
        optional += " OPTIONAL {";
        optional += "?" + PROPERTY + " <" + Rdf.RELATION_TYPE + "> ?" + PROPERTY_TYPE;
        // 6. Select property type label in the requested language if exists
        optional += " . OPTIONAL {";
        optional += "?" + PROPERTY_TYPE + " <" + Rdfs.RELATION_LABEL + "> ?" + PROPERTY_TYPE_LABEL;
        optional += " . FILTER(LANG(?" + PROPERTY_TYPE_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + PROPERTY_TYPE_LABEL + "), \"" + language + "\"))";
        optional += "}";        
        // 7. Select property type prefered label in the requested language if exists
        optional += " . OPTIONAL {";
        optional += " ?" + PROPERTY_TYPE + " <" + Skos.RELATION_PREF_LABEL + "> ?" + PROPERTY_TYPE_PREF_LABEL;
        optional += " . FILTER(LANG(?" + PROPERTY_TYPE_PREF_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + PROPERTY_TYPE_PREF_LABEL + "), \"" + language + "\"))";
        optional += "}";
        optional += "}";
        
        query.appendOptional(optional);

        // 8. If subClassOf is specified, add filter on uri rdf:type
        if (subClassOf != null) {
            query.appendTriplet("<" + uri + ">", Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
            query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", "<" + subClassOf + ">", null);
        }
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        
        return query;
    }
    
     /**
     * Search all the properties corresponding to the given object uri.
     * @return the list of the properties which match the given uri.
     */
    public ArrayList<PropertiesDTO<PropertyLabelsDTO>> getAllProperties() {        
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        
        PropertiesDTO<PropertyLabelsDTO> properties = new PropertiesDTO<>();
        properties.setUri(uri);
        
        ArrayList<PropertiesDTO<PropertyLabelsDTO>> propertiesContainer = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                
                PropertyLabelsDTO property = new PropertyLabelsDTO();
        
                if (bindingSet.hasBinding(PROPERTY_TYPE)) {
                    property.setRdfType(bindingSet.getValue(PROPERTY_TYPE).stringValue());
                }
                property.setRelation(bindingSet.getValue(RELATION).stringValue());
                property.setValue(bindingSet.getValue(PROPERTY).stringValue());

                if (properties.hasProperty(property)) {
                    if (bindingSet.hasBinding(PROPERTY_TYPE_LABEL)) {
                        property.addLastRdfTypeLabel(bindingSet.getValue(PROPERTY_TYPE_LABEL).stringValue());
                    }

                    if (bindingSet.hasBinding(RELATION_LABEL)) {
                        property.addLastRelationLabel(bindingSet.getValue(RELATION_LABEL).stringValue());
                    }

                    if (bindingSet.hasBinding(PROPERTY_LABEL)) {
                        property.addLastValueLabel(bindingSet.getValue(PROPERTY_LABEL).stringValue());
                    }
                    
                    PropertyLabelsDTO existingProperty = properties.getProperty(property);

                    existingProperty.addRdfTypeLabels(property.getRdfTypeLabels());
                    existingProperty.addRelationLabels(property.getRelationLabels());
                    existingProperty.addValueLabels(property.getValueLabels());
                } else {
                    if (bindingSet.hasBinding(PROPERTY_TYPE_PREF_LABEL)) {
                        property.addFirstRdfTypeLabel(bindingSet.getValue(PROPERTY_TYPE_PREF_LABEL).stringValue());
                    }

                    if (bindingSet.hasBinding(RELATION_PREF_LABEL)) {
                        property.addFirstRelationLabel(bindingSet.getValue(RELATION_PREF_LABEL).stringValue());
                    }

                    if (bindingSet.hasBinding(PROPERTY_PREF_LABEL)) {
                        String v = bindingSet.getValue(PROPERTY_PREF_LABEL).stringValue();
                        property.addFirstValueLabel(v);
                    }

                    if (bindingSet.hasBinding(PROPERTY_TYPE_LABEL)) {
                        property.addLastRdfTypeLabel(bindingSet.getValue(PROPERTY_TYPE_LABEL).stringValue());
                    }

                    if (bindingSet.hasBinding(RELATION_LABEL)) {
                        property.addLastRelationLabel(bindingSet.getValue(RELATION_LABEL).stringValue());
                    }

                    if (bindingSet.hasBinding(PROPERTY_LABEL)) {
                        property.addLastValueLabel(bindingSet.getValue(PROPERTY_LABEL).stringValue());
                    }
                
                    properties.addProperty(property);                    
                }
            }
            
            if (properties.getUri() != null) {
                propertiesContainer.add(properties);
            }
        }
        
        return propertiesContainer;
    }
}
