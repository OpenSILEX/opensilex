/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.extensions;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDFS;
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
    
    public static final List<URI> skosPropertyValues = Collections.unmodifiableList(
    new ArrayList<URI>() {{
        OntModel skosOntologyModel = SKOSOntology.MemModel;

        for (Iterator<OntProperty> iterator = skosOntologyModel.listAllOntProperties(); iterator.hasNext(); ) {
            OntProperty skosProperty = iterator.next(); 
            add(URI.create(skosProperty.getURI()));
            
        }
    }});
       
    
    public void updateInstanceOntologiesReferences(URI instanceURI, List<OntologyReference> relations) throws Exception;
    
    public default void updateIndividualOntologiesReferences(SPARQLService sparql, Class instanceClass, URI instanceURI, List<OntologyReference> relations) throws Exception {
        if (!sparql.uriExists(instanceClass, instanceURI)) {
            throw new IllegalArgumentException("Unknown "+ instanceClass.getName() +  "uri : " + instanceURI);
        }
       
        Map<URI,List<URI>> mapOntonologyReferences = new HashMap<>();
        Map<URI,List<URI>> mapOntonologyReferencesSeeAlso = new HashMap<>();
        for (OntologyReference relation : relations) {
            URI propertyURI = relation.getProperty();
            URI objectURI = relation.getObject();
            URI seeAlsoURI = relation.getSeeAlso();
            if(objectURI != null && propertyURI != null){
                // Check or not propertyURI ?
                if(!skosPropertyValues.contains(propertyURI)){
                    throw new IllegalArgumentException("Unknown instanceURI :" + propertyURI);
                }
                // Create properties lists
                if(!mapOntonologyReferences.containsKey(propertyURI)){
                    List<URI> propertyUriList = new ArrayList<>();
                    mapOntonologyReferences.put(propertyURI, propertyUriList);
                } 
                mapOntonologyReferences.get(propertyURI).add(objectURI);
                // See also
                if(seeAlsoURI != null){
                    if(!mapOntonologyReferencesSeeAlso.containsKey(objectURI)){
                        List<URI> seeAlsoList = new ArrayList<>();
                        mapOntonologyReferencesSeeAlso.put(objectURI, seeAlsoList);
                    }
                    mapOntonologyReferencesSeeAlso.get(objectURI).add(seeAlsoURI);

                }
            }
        }
        
        for (Map.Entry<URI, List<URI>> entry : mapOntonologyReferences.entrySet()) {
            URI relationProperty = entry.getKey();
            List<URI> objectUriList = entry.getValue();
            sparql.updateObjectRelations(SPARQLDeserializers.nodeURI(instanceURI), instanceURI, Ontology.property(relationProperty.toString()), objectUriList);
        }
        for (Map.Entry<URI, List<URI>> entry : mapOntonologyReferencesSeeAlso.entrySet()) {
            URI objectURI = entry.getKey();
            List<URI> seeAlsoList = entry.getValue();
            sparql.updateObjectRelations(SPARQLDeserializers.nodeURI(objectURI), objectURI, RDFS.seeAlso, seeAlsoList);
        }

    }
    
    public List<OntologyReference> getInstanceOntologiesReferences(URI instanceURI) throws Exception;

    
//    # https://jena.apache.org/documentation/ontology/
    public default List<OntologyReference>  getIndividualOntologiesReferences(SPARQLService sparql, Class instanceClass, URI instanceURI) throws Exception {
        List<OntologyReference> relations = new ArrayList<>();

        if (!sparql.uriExists(instanceClass, instanceURI)) {
            throw new IllegalArgumentException("Unknown "+ instanceClass.getName() +  "uri : " + instanceURI);
        }
        SelectBuilder select = new SelectBuilder();
        
   
        
        Var propertyUriVar = makeVar("property");
        select.addVar(propertyUriVar);
        
        Var objectUriVar = makeVar("objectUri");
        select.addVar(objectUriVar);
        
        Var seeAlsoVar = makeVar("seeAlso");
        select.addVar(seeAlsoVar);
        
        SPARQLQueryHelper.addWhereValues(select,"property",skosPropertyValues);
   
        select.addWhere(SPARQLDeserializers.nodeURI(instanceURI), propertyUriVar, objectUriVar);
        select.addOptional(objectUriVar, RDFS.seeAlso, seeAlsoVar);
        sparql.executeSelectQuery(select, (SPARQLResult result) -> {
            String objectUri = result.getStringValue("objectUri");
            String property = result.getStringValue("property");
            String seeAlso = result.getStringValue("seeAlso");
             
            OntologyReference ontologyReference = new OntologyReference();
            ontologyReference.setObject(URI.create(objectUri));
            ontologyReference.setProperty(URI.create(property));
            if(seeAlso != null){
                ontologyReference.setSeeAlso(URI.create(seeAlso));
            }
            relations.add(ontologyReference);
        });
        System.out.println("org.opensilex.core.ontology.extensions.OntologyReferenceRessourceDAO.getIndividualOntologiesReferences()" + relations.size());
        
        return relations;
    }
}
