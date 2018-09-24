//******************************************************************************
//                                       PropertyDAOSesame.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 29 may 2018
// Contact: morgane.vidal@inra.fr vincent.migot@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
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
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.ontologies.Owl;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Vocabulary;
import phis2ws.service.resources.dto.PropertiesDTO;
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
    
    // This attribute is used to search all properties of the given uri
    public String uri;
        
    // This attribute is used to restrict available uri to a specific set of subclass
    public Vocabulary subClassOf;

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
    protected final String PROPERTY = "property";
    //a count result, used to query the triplestore (count properties)
    private final String COUNT = "count";
    //the relation, used to query the triplestore (cardinalities)
    protected final String RELATION = "relation";
    
    /**
     * prepare the sparql query to get the list of properties and their relations
     * to the given uri. If subClassOf is specified, the object corresponding to the uri must be
     * a subclass of the given type.
     * @return the builded query
     * eg.
     * SELECT DISTINCT  ?relation ?property 
     * WHERE {
     *   <http://www.phenome-fppn.fr/diaphen>  ?relation  ?property  . 
     *   <http://www.phenome-fppn.fr/diaphen>  rdf:type  ?rdfType  . 
     *   ?rdfType  rdfs:subClassOf*  <http://www.phenome-fppn.fr/vocabulary/2017#Infrastructure> . 
     * }
     */
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        query.appendSelect("?" + RELATION + " ?" + PROPERTY);
        query.appendTriplet("<" + uri + ">", "?" + RELATION, "?" + PROPERTY, null);
        query.appendTriplet("<" + uri + ">", "<" + Rdf.RELATION_TYPE.toString() + ">", "?" + RDF_TYPE, null);
        
        if (subClassOf != null) {
            query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", "<" + subClassOf + ">", null);
        }
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        
        return query;
    }
    
     /**
     * search all the properties corresponding to the given object uri
     * @return the list of the properties which match the given uri.
     */
    public ArrayList<PropertiesDTO> allPaginate() {        
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<PropertiesDTO> propertiesContainer = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            PropertiesDTO properties = new PropertiesDTO();
            while (result.hasNext()) {
                if (properties.getUri() == null) {
                    properties.setUri(uri);
                }
                BindingSet bindingSet = result.next();
                PropertyDTO property = new PropertyDTO();
        
                property.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
                property.setRelation(bindingSet.getValue(RELATION).stringValue());
                property.setValue(bindingSet.getValue(PROPERTY).stringValue());
        
                properties.addProperty(property);
            }
            
            if (properties.getUri() != null) {
                propertiesContainer.add(properties);
            }
        }
        
        return propertiesContainer;
    }
    
    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Prepare the sparql query to get the domain of a relation
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
                "<" + Rdfs.RELATION_DOMAIN.toString() + "> "
                        + "/( <" + Owl.RELATION_UNION_OF.toString() + "> / <" + Rdf.RELATION_REST.toString() + ">*/ <" + Rdf.RELATION_FIRST.toString() + ">)*" , "?" + DOMAIN, null);
        
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        
        return query;
    }
    
    /**
     * Get in the triplestore the domain of the property if it exist
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
     * Check if a given relation can be linked to a given rdfType. 
     * Check if there is a domain and if the rdfType corresponds to the domain.
     * /!\ The PropertyDAOSesame#relation must contain the relation which domain is checked
     * @param rdfType the rdf type. e.g. http://www.phenome-fppn.fr/vocabulary/2017#RadiometricTarget
     * @return true if the given property can be linked to the given rdfType
     *         false if the given rdfType is not part of the domain of the property.
     */
    public boolean isRelationDomainCompatibleWithRdfType(String rdfType) {
        ArrayList<String> propertyDomains = getPropertyDomain();
        UriDaoSesame uriDao = new UriDaoSesame();
        boolean domainOk = false;
        if (propertyDomains != null && propertyDomains.size() > 0) { //the property has a specific domain
            for (String propertyDomain : propertyDomains) {
                if (uriDao.isSubClassOf(rdfType, propertyDomain)) {
                    domainOk = true;
                }
            }
        } else { //the property has no specific domain
            domainOk = true;
        }
        
        return domainOk;
    }
    
    /**
     * Query to get cardinalities of a relation for a given type
     * @param rdfType
     * @return 
     */
    private SPARQLQueryBuilder prepareGetCardinality() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendSelect("?" + RDF_TYPE + " ?" + CARDINALITY + " ?" + RESTRICTION);
        
        query.appendTriplet(BLANCK_NODE, Rdf.RELATION_TYPE.toString(), Owl.CONCEPT_RESTRICTION.toString(), null);
        query.appendTriplet(BLANCK_NODE, Owl.RELATION_ON_PROPERTY.toString(), relation, null);
        query.appendTriplet(BLANCK_NODE, "?" + RESTRICTION, "?_" + CARDINALITY, null);
        query.appendTriplet("?" + RDF_TYPE, Rdfs.RELATION_SUBCLASS_OF.toString(), "_:x", null);
        
        query.appendFilter("?" + RESTRICTION + " IN (" 
                                    + "<" + Owl.RELATION_CARDINALITY.toString() + ">, " 
                                    + "<" + Owl.RELATION_MIN_CARDINALITY.toString() + ">, " 
                                    + "<" + Owl.RELATION_MAX_CARDINALITY.toString() + ">, "
                                    + "<" + Owl.RELATION_QUALIFIED_CARDINALITY.toString() + ">)");
        query.appendToBody("bind( xsd:integer(?_" + CARDINALITY + ") as ?" + CARDINALITY + ")");
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }
    
    /**
     * Query to get the cardinalities required for each property for a given concept
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
        
        query.appendTriplet(BLANCK_NODE, Rdf.RELATION_TYPE.toString(), Owl.CONCEPT_RESTRICTION.toString(), null);
        query.appendTriplet(BLANCK_NODE, Owl.RELATION_ON_PROPERTY.toString(), "?" + RELATION, null);
        query.appendTriplet(BLANCK_NODE, "?" + RESTRICTION, "?_" + CARDINALITY, null);
        query.appendTriplet(concept, Rdfs.RELATION_SUBCLASS_OF.toString(), "_:x", null);
        
        query.appendFilter("?" + RESTRICTION + " IN (" 
                                    + "<" + Owl.RELATION_CARDINALITY.toString() + ">, " 
                                    + "<" + Owl.RELATION_MIN_CARDINALITY.toString() + ">, " 
                                    + "<" + Owl.RELATION_MAX_CARDINALITY.toString() + ">, "
                                    + "<" + Owl.RELATION_QUALIFIED_CARDINALITY.toString() + ">)");
        query.appendToBody("bind( xsd:integer(?_" + CARDINALITY + ") as ?" + CARDINALITY + ")");
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;
    }
    
    /**
     * Get the cardinalities of a relation for each concerned concept
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
     * Get the cardinalities of each relations for a concept
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
     * Order the given properties by relation
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
     * Check the cardinalities for a list of properties, for a concept
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
                    if (cardinality.getRdfType().equals(Owl.RELATION_CARDINALITY.toString())) {
                        if (!numberOfRelations.containsKey(entry.getKey())) { //missing property
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.BAD_CARDINALITY + " missing " + entry.getKey()));
                        } else if (numberOfRelations.get(entry.getKey()) != cardinality.getCardinaity()) { //there is not the required number of properties
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.BAD_CARDINALITY + " for " + entry.getKey()));
                        }
                    } else if (cardinality.getRdfType().equals(Owl.RELATION_MIN_CARDINALITY.toString())) {
                        if (!numberOfRelations.containsKey(entry.getKey())) { //missing property
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.BAD_CARDINALITY + " not enought " + entry.getKey()));
                        } else if (numberOfRelations.get(entry.getKey()) < cardinality.getCardinaity()) {
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.BAD_CARDINALITY + " " + "not enought " + entry.getKey()));
                        }
                    } else if (cardinality.getRdfType().equals(Owl.RELATION_MAX_CARDINALITY.toString())) {
                        if ((numberOfRelations.get(entry.getKey()) != null) 
                                && (numberOfRelations.get(entry.getKey()) > cardinality.getCardinaity())) {
                            //error, bad cardinality
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.BAD_CARDINALITY + " " + "too many " + relation));
                        }
                    } else if (cardinality.getRdfType().equals(Owl.RELATION_QUALIFIED_CARDINALITY.toString())) {
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
     * Check the cardinalities of properties for a given object uri
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
            //SILEX:refactor
            //some database calls must be avoided 
            //(e.g if there is a few wavelength associated, one call is necessary)
            //\SILEX:refactor
            int numberValues = 0;
            if (objectUri != null) {
                SPARQLQueryBuilder query = prepareGetProperties(objectUri);
                TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
                try (TupleQueryResult result = tupleQuery.evaluate()) {
                    BindingSet bindingSet = result.next();
                    numberValues = Integer.parseInt(bindingSet.getValue(COUNT).stringValue());
                }
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
