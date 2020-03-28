/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.extensions;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

/**
 *
 * @author charlero
 */
public interface OntologyReferenceRessourceDAO {
    
    public default void updateIndividualOntologiesReferences(SPARQLService sparql, Class instanceClass, URI instanceURI, List<OntologyReference> relations) throws Exception {
        if (!sparql.uriExists(instanceClass, instanceURI)) {
            throw new IllegalArgumentException("Unknown "+ instanceClass.getName() +  "uri : " + instanceURI);
        }
       
        Map<URI,List<URI>> mapOntonologyReferences = new HashMap<>();
        for (OntologyReference relation : relations) {
            URI relationProperty = relation.getProperty();
            // Create properties lists
            if(!mapOntonologyReferences.containsKey(relationProperty)){
                // Check or not relationProperty ?
//                if (!sparql.uriExists(relationProperty)) {
//                    throw new IllegalArgumentException("Unknown instanceURI :" + relationProperty);
//                }
                List<URI> propertyUriList = new ArrayList<>();
                mapOntonologyReferences.put(relationProperty, propertyUriList);
            } 
            mapOntonologyReferences.get(relationProperty).add(relationProperty);
        }
        
        for (Map.Entry<URI, List<URI>> entry : mapOntonologyReferences.entrySet()) {
            URI relationProperty = entry.getKey();
            List<URI> propertyUriList = entry.getValue();
            sparql.updateSubjectRelations(SPARQLDeserializers.nodeURI(instanceURI), propertyUriList, Ontology.property(relationProperty.toString()), instanceURI);
        }
    }
    
//    # https://jena.apache.org/documentation/ontology/
    public default List<OntologyReference>  getIndividualOntologiesReferences(SPARQLService sparql, Class instanceClass, URI instanceURI) throws Exception {
        List<OntologyReference> relations = new ArrayList<>();

        if (!sparql.uriExists(instanceClass, instanceURI)) {
            throw new IllegalArgumentException("Unknown "+ instanceClass.getName() +  "uri : " + instanceURI);
        }
        OntModel skosOntologyModel = ModelFactory.createOntologyModel( SKOS.getURI() );
        SelectBuilder select = new SelectBuilder();
        
        
        Var referenceUriVar = makeVar("referenceUri");
        select.addVar(referenceUriVar);
        
        Var propertyVar = makeVar("property");
        List<URI> propertyValues = new ArrayList<>();
        Map<Var,List<URI>> propertyValuesVar = new HashMap<>();
        for (Iterator<OntProperty> iterator = skosOntologyModel.listAllOntProperties(); iterator.hasNext(); ) {
            OntProperty skosProperty = iterator.next(); 
            propertyValues.add(URI.create(skosProperty.getURI()));
        }
        propertyValuesVar.put(propertyVar, propertyValues);
        select.addValueVars(propertyValuesVar);

        sparql.executeSelectQuery(select, (SPARQLResult result) -> {
            String referenceUri = result.getStringValue("referenceUri");
            String property = result.getStringValue("property");
            
            OntologyReference ontologyReference = new OntologyReference();
            ontologyReference.setObject(instanceURI);
            ontologyReference.setProperty(URI.create(property));
            ontologyReference.setSeeAlso(URI.create(referenceUri));
            relations.add(ontologyReference);
        });
        
        return relations;
    }
}
