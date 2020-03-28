/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.extensions;

import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definitions from skos.rdf
 */
public interface SKOSOntology {
    /**
     * The ontology model that holds the vocabulary terms
     */
    OntModel MemModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM, null );
    
    /**
     * The namespace of the vocabulary as a string
     */
    String NS = "http://www.w3.org/2004/02/skos/core#";

    /**
     * The namespace of the vocabulary as a resource
     */
    Resource NAMESPACE = MemModel.createResource( NS );
    
    ObjectProperty broadMatch = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#broadMatch" );
    
    /**
     * Broader concepts are typically rendered as parents in a concept hierarchy(tree).
     */
    ObjectProperty broader = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#broader" );
    
//    ObjectProperty broaderTransitive = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#broaderTransitive" );
    
    ObjectProperty closeMatch = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#closeMatch" );
    
    /**
     * skos:exactMatch is disjoint with each of the properties skos:broadMatch and
     *  skos:relatedMatch.
     */
    ObjectProperty exactMatch = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#exactMatch" );
    
//    ObjectProperty hasTopConcept = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#hasTopConcept" );
    
//    ObjectProperty inScheme = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#inScheme" );
    
    /**
     * These concept mapping relations mirror semantic relations, and the data model
     * defined below is similar (with the exception of skos:exactMatch) to the data
     * model defined for semantic relations. A distinct vocabulary is provided for
     * concept mapping relations, to provide a convenient way to differentiate links
     * within a concept scheme from links between concept schemes. However, this
     * pattern of usage is not a formal requirement of the SKOS data model, and relies
     * on informal definitions of best practice.
     */
//    ObjectProperty mappingRelation = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#mappingRelation" );
    
//    ObjectProperty member = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#member" );
    
    /**
     * For any resource, every item in the list given as the value of the skos:memberList
     * property is also a value of the skos:member property.
     */
//    ObjectProperty memberList = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#memberList" );
    
    ObjectProperty narrowMatch = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#narrowMatch" );
    
    /**
     * Narrower concepts are typically rendered as children in a concept hierarchy (tree).
     */
    ObjectProperty narrower = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#narrower" );
    
//    ObjectProperty narrowerTransitive = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#narrowerTransitive" );
    
    /**
     * skos:related is disjoint with skos:broaderTransitive
     */
    ObjectProperty related = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#related" );
//    
//    ObjectProperty relatedMatch = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#relatedMatch" );
//    
//    ObjectProperty semanticRelation = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#semanticRelation" );
//    
//    ObjectProperty topConceptOf = MemModel.createObjectProperty( "http://www.w3.org/2004/02/skos/core#topConceptOf" );
//    
//    DatatypeProperty notation = MemModel.createDatatypeProperty( "http://www.w3.org/2004/02/skos/core#notation" );
    
    /**
     * The range of skos:altLabel is the class of RDF plain literals.skos:prefLabel,
     * skos:altLabel and skos:hiddenLabel are pairwise disjoint properties.
     */
    AnnotationProperty altLabel = MemModel.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#altLabel" );
//    
//    AnnotationProperty changeNote = MemModel.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#changeNote" );
//    
//    AnnotationProperty definition = MemModel.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#definition" );
//    
//    AnnotationProperty editorialNote = MemModel.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#editorialNote" );
//    
//    AnnotationProperty example = MemModel.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#example" );
//    
    /**
     * The range of skos:hiddenLabel is the class of RDF plain literals.skos:prefLabel,
     *  skos:altLabel and skos:hiddenLabel are pairwise disjoint properties.
     */
//    AnnotationProperty hiddenLabel = MemModel.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#hiddenLabel" );
//    
//    AnnotationProperty historyNote = MemModel.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#historyNote" );
//    
    AnnotationProperty note = MemModel.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#note" );
    
    /**
     * A resource has no more than one value of skos:prefLabel per language tag.skos:prefLabel,
     * skos:altLabel and skos:hiddenLabel are pairwise disjoint properties.The range
     * of skos:prefLabel is the class of RDF plain literals.
     */
    AnnotationProperty prefLabel = MemModel.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#prefLabel" );
    
//    AnnotationProperty scopeNote = MemModel.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#scopeNote" );
    
    OntClass Collection = MemModel.createClass( "http://www.w3.org/2004/02/skos/core#Collection" );
    
    OntClass Concept = MemModel.createClass( "http://www.w3.org/2004/02/skos/core#Concept" );
    
    OntClass ConceptScheme = MemModel.createClass( "http://www.w3.org/2004/02/skos/core#ConceptScheme" );
    
    OntClass OrderedCollection = MemModel.createClass( "http://www.w3.org/2004/02/skos/core#OrderedCollection" );

}
