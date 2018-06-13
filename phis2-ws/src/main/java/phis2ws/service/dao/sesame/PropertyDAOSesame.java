//******************************************************************************
//                                       PropertyDAOSesame.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 29 mai 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  29 mai 2018
// Subject: access and manipulation of the properties of the ontology in the triplestore
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.PropertyDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Cardinality;
import phis2ws.service.view.model.phis.Property;

//SILEX:todo
//use this DAO in the agronomical objects DAO
//\SILEX:todo

/**
 * CRUD methods for the properties stored in the triplestore (rdf4j)
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class PropertyDAOSesame extends DAOSesame<Property> {
    final static Logger LOGGER = LoggerFactory.getLogger(SensorDAOSesame.class);
    
    //The following attributes are used to search properties in the triplestore
    //the property relation name. 
    //the relation term is used because it only represents the "vocabulary:property" 
    //and it does not represents everything around such as domain, range, etc. 
    //which is represented by the "Property" label
    public String relation;
    
    //the domain label used to query triplestore
    private final String DOMAIN = "domain";
    //the cardinality between a property and a concept, used to query the triplestore
    private final String CARDINALITY = "cardinality";
    //the restriction between a property and a concept, used to query the triplestore
    private final String RESTRICTION = "restriction";
    //a blank node, used to query the triplestore
    private final String BLANCK_NODE = "_:x";
    //a property, used to query the triplestore
    private final String PROPERTY = "property";
    //a count result, used to query the triplestore (count properties)
    private final String COUNT = "count";
    //the rdf type, used to query the triplestore (cardinalities)
    private final String RDF_TYPE = "rdfType";
    //the relation, used to query the triplestore (cardinalities)
    private final String RELATION = "relation";
    
    
    //Triplestore relations and concepts
    private final static URINamespaces NAMESPACES = new URINamespaces();
    
    private final static String TRIPLESTORE_CONCEPT_RESTRICTION = NAMESPACES.getObjectsProperty("cRestriction");
    
    private final static String TRIPLESTORE_RELATION_DOMAIN = NAMESPACES.getRelationsProperty("domain");
    private final static String TRIPLESTORE_RELATION_UNION_OF = NAMESPACES.getRelationsProperty("unionOf");
    private final static String TRIPLESTORE_RELATION_REST = NAMESPACES.getRelationsProperty("rest");
    private final static String TRIPLESTORE_RELATION_FIRST = NAMESPACES.getRelationsProperty("first");
    private final static String TRIPLESTORE_RELATION_TYPE = NAMESPACES.getRelationsProperty("type");
    private final static String TRIPLESTORE_RELATION_ON_PROPERTY = NAMESPACES.getRelationsProperty("onProperty");
    private final static String TRIPLESTORE_RELATION_CARDINALITY = NAMESPACES.getRelationsProperty("cardinality");
    private final static String TRIPLESTORE_RELATION_MIN_CARDINALITY = NAMESPACES.getRelationsProperty("minCardinality");
    private final static String TRIPLESTORE_RELATION_MAX_CARDINALITY = NAMESPACES.getRelationsProperty("maxCardinality");
    private final static String TRIPLESTORE_RELATION_QUALIFIED_CARDINALITY = NAMESPACES.getRelationsProperty("qualifiedCardinality");
    private final static String TRIPLESTORE_RELATION_SUBCLASS_OF = NAMESPACES.getRelationsProperty("subClassOf");

    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * prepare the sparql query to get the domain of a relation
     * @return the builded query
     * e.g.
     * SELECT ?domain
     * WHERE {
     *      <http://www.phenome-fppn.fr/vocabulary/2017#wavelength> rdfs:domain ?domain
     * }
     */
    private SPARQLQueryBuilder prepareGetDomainQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendSelect("?" + DOMAIN);
        query.appendTriplet(relation, 
                TRIPLESTORE_RELATION_DOMAIN + "/(" + TRIPLESTORE_RELATION_UNION_OF + "/" + TRIPLESTORE_RELATION_REST + "*/" + TRIPLESTORE_RELATION_FIRST + ")*" , "?" + DOMAIN, null);
        
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        
        return query;
    }
    
    /**
     * get in the triplestore the domain of the property if it exist
     * @return the domain of the property (attributes relation)
     */
    public ArrayList<String> getPropertyDomain() {
        SPARQLQueryBuilder query = prepareGetDomainQuery();
        ArrayList<String> propertyDomains = new ArrayList<>();
        
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                propertyDomains.add(bindingSet.getValue(DOMAIN).toString());
            }
        }
        
        return propertyDomains;
    }
    
    /**
     * query to get cardinalities of a relation for a given type
     * @param rdfType
     * @return 
     */
    private SPARQLQueryBuilder prepareGetCardinality() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendSelect("?" + RDF_TYPE + " ?" + CARDINALITY + " ?" + RESTRICTION);
        
        query.appendTriplet(BLANCK_NODE, TRIPLESTORE_RELATION_TYPE, TRIPLESTORE_CONCEPT_RESTRICTION, null);
        query.appendTriplet(BLANCK_NODE, TRIPLESTORE_RELATION_ON_PROPERTY, relation, null);
        query.appendTriplet(BLANCK_NODE, "?" + RESTRICTION, "?_" + CARDINALITY, null);
        query.appendTriplet("?" + RDF_TYPE, TRIPLESTORE_RELATION_SUBCLASS_OF, "_:x", null);
        
        query.appendFilter("?" + RESTRICTION + " IN (" 
                                    + "<" + TRIPLESTORE_RELATION_CARDINALITY + ">, " 
                                    + "<" + TRIPLESTORE_RELATION_MIN_CARDINALITY + ">, " 
                                    + "<" + TRIPLESTORE_RELATION_MAX_CARDINALITY + ">, "
                                    + "<" + TRIPLESTORE_RELATION_QUALIFIED_CARDINALITY + ">)");
        query.appendToBody("bind( xsd:integer(?_" + CARDINALITY + ") as ?" + CARDINALITY + ")");
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }
    
    /**
     * query to get the cardinalities required for each property for a given concept
     * @param concept
     * @return the query
     * e.g. 
     * SELECT ?relation ?cardinality ?restriction 
     * WHERE {
     *      _:x  rdf:type  owl:Restriction  . 
     *      _:x  owl:onProperty  ?relation  . 
     *      _:x  ?restriction  ?_cardinality  . 
     *      <http://www.phenome-fppn.fr/vocabulary/2017#TIRCamera>  rdfs:subClassOf  _:x  . 
     *      bind( xsd:integer(?_cardinality) as ?cardinality) .
     *      FILTER ( ?restriction IN (<http://www.w3.org/2002/07/owl#cardinality>, 
     *                                <http://www.w3.org/2002/07/owl#minCardinality>, 
     *                                <http://www.w3.org/2002/07/owl#maxCardinality>, 
     *                                <http://www.w3.org/2002/07/owl#qualifiedCardinality>) ) 
     * }
     */
    private SPARQLQueryBuilder prepareGetPropertiesCardinalitiesByConcept(String concept) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendSelect("?" + RELATION + " ?" + CARDINALITY + " ?" + RESTRICTION);
        
        query.appendTriplet(BLANCK_NODE, TRIPLESTORE_RELATION_TYPE, TRIPLESTORE_CONCEPT_RESTRICTION, null);
        query.appendTriplet(BLANCK_NODE, TRIPLESTORE_RELATION_ON_PROPERTY, "?" + RELATION, null);
        query.appendTriplet(BLANCK_NODE, "?" + RESTRICTION, "?_" + CARDINALITY, null);
        query.appendTriplet(concept, TRIPLESTORE_RELATION_SUBCLASS_OF, "_:x", null);
        
        query.appendFilter("?" + RESTRICTION + " IN (" 
                                    + "<" + TRIPLESTORE_RELATION_CARDINALITY + ">, " 
                                    + "<" + TRIPLESTORE_RELATION_MIN_CARDINALITY + ">, " 
                                    + "<" + TRIPLESTORE_RELATION_MAX_CARDINALITY + ">, "
                                    + "<" + TRIPLESTORE_RELATION_QUALIFIED_CARDINALITY + ">)");
        query.appendToBody("bind( xsd:integer(?_" + CARDINALITY + ") as ?" + CARDINALITY + ")");
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }
    
    /**
     * get the cardinalities of a relation for each concerned concept
     * @return the list of the cardinalities founded in the triplestore
     * e.g of content : 
     * "owl:cardinality" : 1
     * other example of content : 
     * "owl:minCardinality" : 1
     * "owl:maxCardinality" : 5
     */
    public HashMap<String, Cardinality> getCardinalities() {
        SPARQLQueryBuilder query = prepareGetCardinality();
        HashMap<String, Cardinality> cardinalities = new HashMap<>();
        
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                
                Cardinality cardinality = new Cardinality();
                cardinality.setRdfType(bindingSet.getValue(RESTRICTION).toString());
                
                String card = bindingSet.getValue(CARDINALITY).toString();
                
                String[] splitCardinality = card.split("\"");
                
                cardinality.setCardinaity(Integer.parseInt(splitCardinality[1]));
                
                cardinalities.put(bindingSet.getValue(RDF_TYPE).toString(), cardinality);
            }
        }
        
        return cardinalities;
    }
    
    /**
     * get the cardinalities of each relations for a concept
     * @param concept
     *  @return the list of the cardinalities founded in the triplestore
     * e.g of content : 
     * "vocabulary:attenuatorFilter" : ["owl:cardinality" : 1]
     * other example of content : 
     * "vocabulary:wavelength" : ["owl:minCardinality" : 1, "owl:maxCardinality" : 6]
     */
    public HashMap<String, ArrayList<Cardinality>> getCardinalitiesForConcept(String concept) {
       SPARQLQueryBuilder query = prepareGetPropertiesCardinalitiesByConcept(concept);
        HashMap<String, ArrayList<Cardinality>> cardinalities = new HashMap<>();
        
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                
                Cardinality cardinality = new Cardinality();
                cardinality.setRdfType(bindingSet.getValue(RESTRICTION).toString());
                
                String card = bindingSet.getValue(CARDINALITY).toString();
                
                String[] splitCardinality = card.split("\"");
                
                cardinality.setCardinaity(Integer.parseInt(splitCardinality[1]));
                
                ArrayList<Cardinality> cardinalitiesForRelation = new ArrayList<>();
                cardinalitiesForRelation.add(cardinality);
                
                if (cardinalities.containsKey(bindingSet.getValue(RELATION).toString())) {
                    cardinalitiesForRelation.addAll(cardinalities.get(bindingSet.getValue(RELATION).toString()));
                }
                
                cardinalities.put(bindingSet.getValue(RELATION).toString(), cardinalitiesForRelation);
            }
        }
        
        return cardinalities; 
    }
    
    /**
     * SPARQL query to get the number of the property "relation" for the given object uri
     * @param objectUri
     * @return the query
     * e.g. 
     * SELECT DISTINCT (count(distinct ?property) as ?count) 
     * WHERE {
     *  <http://www.phenome-fppn.fr/diaphen/2018/s18523>  <http://www.phenome-fppn.fr/vocabulary/2017#hasLens>  ?property  . 
     * }
     */
    private SPARQLQueryBuilder prepareGetProperties(String objectUri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        query.appendSelect("(count(distinct ?" + PROPERTY + ") as ?" + COUNT + ")");
        
        query.appendTriplet("<" + objectUri + ">", "<" + relation + ">", "?" + PROPERTY, null);
        
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        
        return query;
    }
    
    /**
     * order the given properties by relation
     * @param properties
     * @return the ordered list
     */
    private HashMap<String, ArrayList<PropertyDTO>> orderPropertiesByRelation(ArrayList<PropertyDTO> properties) {
        HashMap<String, ArrayList<PropertyDTO>> propertiesByRelation = new HashMap<>();
        for (PropertyDTO propertyDTO : properties) {
            ArrayList<PropertyDTO> propertiesOfRelation = new ArrayList<>();
            if (propertiesByRelation.containsKey(propertyDTO.getRelation())) {
                propertiesOfRelation = propertiesByRelation.get(propertyDTO.getRelation());
            }
            propertiesOfRelation.add(propertyDTO);
            propertiesByRelation.put(propertyDTO.getRelation(), propertiesOfRelation);
        }
        
        return propertiesByRelation;
    }
    
    /**
     * check the cardinalities for a list of properties, for a concept
     * @param numberOfRelations
     * @param expectedCardinalities
     * @return 
     */
    private POSTResultsReturn checkPropertyCardinality(HashMap<String, Integer> numberOfRelations, HashMap<String, ArrayList<Cardinality>> expectedCardinalities) {
        POSTResultsReturn check = null;
        boolean dataOk = true;
        List<Status> checkStatus = new ArrayList<>();
        
        if (expectedCardinalities != null) {
            for (Map.Entry<String, ArrayList<Cardinality>> entry : expectedCardinalities.entrySet()) {
                for (Cardinality cardinality : entry.getValue()) {
                    if (cardinality.getRdfType().equals(TRIPLESTORE_RELATION_CARDINALITY)) {
                        if (!numberOfRelations.containsKey(entry.getKey())) { //missing property
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.BAD_CARDINALITY + " missing " + entry.getKey()));
                        } else if (numberOfRelations.get(entry.getKey()) != cardinality.getCardinaity()) { //there is not the required number of properties
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.BAD_CARDINALITY + " for " + entry.getKey()));
                        }
                    } else if (cardinality.getRdfType().equals(TRIPLESTORE_RELATION_MIN_CARDINALITY)) {
                        if (!numberOfRelations.containsKey(entry.getKey())) { //missing property
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.BAD_CARDINALITY + " not enought " + entry.getKey()));
                        } else if (numberOfRelations.get(entry.getKey()) < cardinality.getCardinaity()) {
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.BAD_CARDINALITY + " " + "not enought " + entry.getKey()));
                        }
                    } else if (cardinality.getRdfType().equals(TRIPLESTORE_RELATION_MAX_CARDINALITY)) {
                        if ((numberOfRelations.get(entry.getKey()) != null) 
                                && (numberOfRelations.get(entry.getKey()) > cardinality.getCardinaity())) {
                            //error, bad cardinality
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.BAD_CARDINALITY + " " + "too many " + relation));
                        }
                    } else if (cardinality.getRdfType().equals(TRIPLESTORE_RELATION_QUALIFIED_CARDINALITY)) {
                        if (!numberOfRelations.containsKey(entry.getKey())) { //missing property
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.BAD_CARDINALITY + " missing " + entry.getKey()));
                        } else if (numberOfRelations.get(entry.getKey()) != cardinality.getCardinaity()) { //there is not the required number of properties
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.BAD_CARDINALITY + " for " + entry.getKey() + " expected " + cardinality.getCardinaity()));
                        }
                    }
                }
            }
        }
        
        check = new POSTResultsReturn(dataOk, null, dataOk);
        check.statusList = checkStatus;
        return check;
    }
    
    /**
     * check the cardinalities of properties for a given object uri
     * @param properties
     * @param objectUri
     * @param objectRdfType
     * @return 
     */
    public POSTResultsReturn checkCardinalities(ArrayList<PropertyDTO> properties, String objectUri, String objectRdfType) {        
        POSTResultsReturn check = null;
        //list of the returned results
        List<Status> checkStatus = new ArrayList<>();
        boolean dataOk = true;
        HashMap<String, ArrayList<PropertyDTO>> propertiesByRelation = orderPropertiesByRelation(properties);
        
        HashMap<String, ArrayList<Cardinality>> cardinalities = getCardinalitiesForConcept(objectRdfType);
        HashMap<String, Integer> numberOfRelations = new HashMap<>();
        
        for (Map.Entry<String, ArrayList<PropertyDTO>> pair : propertiesByRelation.entrySet()) {
            //1. get the number of values already existing for the property
            int numberValues = 0;
            if (objectUri != null) {
                SPARQLQueryBuilder query = prepareGetProperties(objectUri);
                TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
                TupleQueryResult result = tupleQuery.evaluate();
                BindingSet bindingSet = result.next();
                numberValues = Integer.parseInt(bindingSet.getValue(COUNT).stringValue());
            }
            
            //2. get the total number of values for the property if the new properties are inserted
            int nbProp = numberValues + pair.getValue().size();
            numberOfRelations.put(pair.getKey(), nbProp);
        }
        
        //check all the cardinalities 
        POSTResultsReturn checkPropertyCardinality = checkPropertyCardinality(numberOfRelations, cardinalities);
        if (!checkPropertyCardinality.getDataState()) {
            dataOk = false;
            checkStatus.addAll(checkPropertyCardinality.getStatusList());
        }
        
        check = new POSTResultsReturn(dataOk, null, dataOk);
        check.statusList = checkStatus;
        
        return check;
    }
}
