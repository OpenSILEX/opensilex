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
import org.apache.jena.vocabulary.RDFS;
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
import phis2ws.service.ontologies.Skos;
import phis2ws.service.ontologies.Oeso;
import phis2ws.service.resources.dto.rdfResourceDefinition.PropertyPostDTO;
import phis2ws.service.resources.dto.rdfResourceDefinition.RdfResourceDefinitionDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Cardinality;
import phis2ws.service.view.model.phis.RdfResourceDefinition;
import phis2ws.service.view.model.phis.Property;

//SILEX:todo
//use this DAO in the agronomical objects DAO
//\SILEX:todo

/**
 * CRUD methods for the properties stored in the triplestore (rdf4j)
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class PropertyDAOSesame extends DAOSesame<Property> {
    final static Logger LOGGER = LoggerFactory.getLogger(PropertyDAOSesame.class);
    
    // This attribute is used to search all properties of the given uri
    public String uri;
        
    // This attribute is used to restrict available uri to a specific set of subclass
    public Oeso subClassOf;

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
    
    protected final String PROPERTY_TYPE = "propertyType";
    protected final String RELATION_LABEL = "relationLabel";    
    protected final String PROPERTY_LABEL = "propertyLabel";    
    protected final String PROPERTY_TYPE_LABEL = "propertyTypeLabel";   
    protected final String RELATION_PREF_LABEL = "relationPrefLabel";    
    protected final String PROPERTY_PREF_LABEL = "propertyPrefLabel";    
    protected final String PROPERTY_TYPE_PREF_LABEL = "propertyTypePrefLabel";   

    public PropertyDAOSesame() {
        super();
    }
    
    public PropertyDAOSesame(String uri) {
        super();
        this.uri = uri;
    }
    
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
     *   ?rdfType  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#Infrastructure> . 
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
    public ArrayList<RdfResourceDefinitionDTO> allPaginate() {        
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<RdfResourceDefinitionDTO> propertiesContainer = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            RdfResourceDefinitionDTO properties = new RdfResourceDefinitionDTO();
            while (result.hasNext()) {
                if (properties.getUri() == null) {
                    properties.setUri(uri);
                }
                BindingSet bindingSet = result.next();
                PropertyPostDTO property = new PropertyPostDTO();
        
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
     *      <http://www.opensilex.org/vocabulary/oeso#wavelength> rdfs:domain ?domain
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
     * @param rdfType the rdf type. e.g. http://www.opensilex.org/vocabulary/oeso#RadiometricTarget
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
     *      <http://www.opensilex.org/vocabulary/oeso#TIRCamera>  rdfs:subClassOf  _:x  . 
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
     *  <http://www.phenome-fppn.fr/diaphen/2018/s18523>  <http://www.opensilex.org/vocabulary/oeso#hasLens>  ?property  . 
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
    private HashMap<String, ArrayList<PropertyPostDTO>> orderPropertiesByRelation(ArrayList<PropertyPostDTO> properties) {
        HashMap<String, ArrayList<PropertyPostDTO>> propertiesByRelation = new HashMap<>();
        for (PropertyPostDTO propertyDTO : properties) {
            ArrayList<PropertyPostDTO> propertiesOfRelation = new ArrayList<>();
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
    public POSTResultsReturn checkCardinalities(ArrayList<PropertyPostDTO> properties, String objectUri, String objectRdfType) {        
        POSTResultsReturn check = null;
        //list of the returned results
        List<Status> checkStatus = new ArrayList<>();
        boolean dataOk = true;
        HashMap<String, ArrayList<PropertyPostDTO>> propertiesByRelation = orderPropertiesByRelation(properties);
        
        HashMap<String, ArrayList<Cardinality>> cardinalities = getCardinalitiesForConcept(objectRdfType);
        HashMap<String, Integer> numberOfRelations = new HashMap<>();
        
        for (Map.Entry<String, ArrayList<PropertyPostDTO>> pair : propertiesByRelation.entrySet()) {
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
    
    /**
     * Prepare the sparql query to get the list of properties and their 
     * relations to the given uri with labels (skos:prefered and rdfs:label). 
     * If "subClassOf" property is defined, the request will also check
     * if the current uri correspond to this ontology type
     * 
     * By example this request will return this kind of results for Phenorch infrastructure:
     * ╔═════════════════════╦════════════╦═══════════════╦════════════════════════════╦═══════════════════════╦═════════════════╦════════════════════════════════╦════════════════╦═════════════════════════╗
     * ║ relation            ║ rPrefLabel ║ relationLabel ║ property                   ║ propertyPrefLabel     ║ propertyLabel   ║ propertyType                   ║ pTypePrefLabel ║ propertyTypeLabel       ║
     * ╠═════════════════════╬════════════╬═══════════════╬════════════════════════════╬═══════════════════════╬═════════════════╬════════════════════════════════╬════════════════╬═════════════════════════╣
     * ║ rdf:type            ║            ║               ║ vocabulary:Installation    ║                       ║ installation@en ║ owl:Class                      ║                ║                         ║
     * ╠═════════════════════╬════════════╬═══════════════╬════════════════════════════╬═══════════════════════╬═════════════════╬════════════════════════════════╬════════════════╬═════════════════════════╣
     * ║ rdfs:label          ║            ║               ║ PHENOARCH                  ║                       ║                 ║                                ║                ║                         ║
     * ╠═════════════════════╬════════════╬═══════════════╬════════════════════════════╬═══════════════════════╬═════════════════╬════════════════════════════════╬════════════════╬═════════════════════════╣
     * ║ vocabulary:hasPart  ║            ║ has part@en   ║ http://.../m3p/eo/es2/roof ║ PhenoArch: roof       ║ aria            ║ vocabulary:Greenhouse          ║                ║ greenhouse@en           ║
     * ╠═════════════════════╬════════════╬═══════════════╬════════════════════════════╬═══════════════════════╬═════════════════╬════════════════════════════════╬════════════════╬═════════════════════════╣
     * ║ vocabulary:hasPart  ║            ║ has part@en   ║ http://.../m3p/es2         ║ PhenoArch: greenhouse ║ es2             ║ vocabulary:Greenhouse          ║                ║ greenhouse@en           ║
     * ╠═════════════════════╬════════════╬═══════════════╬════════════════════════════╬═══════════════════════╬═════════════════╬════════════════════════════════╬════════════════╬═════════════════════════╣
     * ║ vocabulary:isPartOf ║            ║ is part of@en ║ http://.../m3p             ║                       ║ M3P             ║ vocabulary:LocalInfrastructure ║                ║ local infrastructure@en ║
     * ╚═════════════════════╩════════════╩═══════════════╩════════════════════════════╩═══════════════════════╩═════════════════╩════════════════════════════════╩════════════════╩═════════════════════════╝
     * 
     * @param language specify in which language labels should be returned
     * @param relationsToIgnore some relations sometimes must not be considered as properties so we ignore them
     * @return the builded query
     * @example
     * SELECT DISTINCT  
     *     ?relation ?relationPrefLabel ?relationLabel 
     *     ?property ?propertyPrefLabel ?propertyLabel 
     *     ?propertyType ?propertyTypePrefLabel ?propertyTypeLabel 
     * WHERE {
     *     <http://www.phenome-fppn.fr>  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  ?rdfType  . 
     *     ?rdfType  <http://www.w3.org/2000/01/rdf-schema#subClassOf>*  <http://www.opensilex.org/vocabulary/oeso#Infrastructure>  . 
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
    protected SPARQLQueryBuilder prepareSearchPropertiesQuery(String language, ArrayList<String> relationsToIgnore) {
        
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
        if (language != null) {
            optional += " . FILTER(LANG(?" + PROPERTY_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + PROPERTY_LABEL + "), \"" + language + "\"))";   
        }
        optional += "}";
        // 3. Select property prefered label in the requested language if exists
        optional += " OPTIONAL {";
        optional += " ?" + PROPERTY + " <" + Skos.RELATION_PREF_LABEL + "> ?" + PROPERTY_PREF_LABEL;
        if (language != null) {
            optional += " . FILTER(LANG(?" + PROPERTY_PREF_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + PROPERTY_PREF_LABEL + "), \"" + language + "\"))";
        }
        optional += "}";
        // 4. Select relation label in the requested language if exists
        optional += " OPTIONAL {";
        optional += "?" + RELATION + " <" + Rdfs.RELATION_LABEL + "> ?" + RELATION_LABEL;
        if (language != null) {
            optional += " . FILTER(LANG(?" + RELATION_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + RELATION_LABEL + "), \"" + language + "\"))";
        }
        optional += "}"; 
        // 5. Select relation prefered label in the requested language if exists
        optional += " OPTIONAL {";
        optional += " ?" + RELATION + " <" + Skos.RELATION_PREF_LABEL + "> ?" + RELATION_PREF_LABEL;
        if (language != null) {
            optional += " . FILTER(LANG(?" + RELATION_PREF_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + RELATION_PREF_LABEL + "), \"" + language + "\"))";        
        }
        optional += "}";
        // 6. Select property type (rdf type of the property) if exists
        optional += " OPTIONAL {";
        optional += "?" + PROPERTY + " <" + Rdf.RELATION_TYPE + "> ?" + PROPERTY_TYPE;
        // 6. Select property type label in the requested language if exists
        optional += " . OPTIONAL {";
        optional += "?" + PROPERTY_TYPE + " <" + Rdfs.RELATION_LABEL + "> ?" + PROPERTY_TYPE_LABEL;
        if (language != null) {
            optional += " . FILTER(LANG(?" + PROPERTY_TYPE_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + PROPERTY_TYPE_LABEL + "), \"" + language + "\"))";
        }
        optional += "}";        
        // 7. Select property type prefered label in the requested language if exists
        optional += " . OPTIONAL {";
        optional += " ?" + PROPERTY_TYPE + " <" + Skos.RELATION_PREF_LABEL + "> ?" + PROPERTY_TYPE_PREF_LABEL;
        if (language != null) {
            optional += " . FILTER(LANG(?" + PROPERTY_TYPE_PREF_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + PROPERTY_TYPE_PREF_LABEL + "), \"" + language + "\"))";
        }
        optional += "}";
        optional += "}";
        
        query.appendOptional(optional);

        // 8. If subClassOf is specified, add filter on uri rdf:type
        if (subClassOf != null) {
            query.appendTriplet("<" + uri + ">", Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
            query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", "<" + subClassOf + ">", null);
        }
        
        if (relationsToIgnore != null)
        {
            String relationToIgnoreQuery = "FILTER (?" + RELATION + " NOT IN (";
            
            boolean firstRelationToIgnore = true;
            for (String relationToIgnore : relationsToIgnore){
                
                if (!firstRelationToIgnore){
                    relationToIgnoreQuery += ", ";
                }
                else{
                    firstRelationToIgnore = false;
                }
                
                relationToIgnoreQuery += "<" + relationToIgnore + ">";
            }
            relationToIgnoreQuery += "))";
            
            query.appendToBody(relationToIgnoreQuery);
        }
        
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        
        return query;
    }
    
     /**
     * Search all the properties corresponding to the given object uri
     * and fill the RDF Resource defintion object with the values and labels
     * 
     * @param definition The definition object which will be filled
     * @param language specify in which language labels should be returned. The 
     * language can be null
     * @return true    if the definition object is correctly filled
     *          false   if the uri doesn't exists
     */
    public boolean getAllPropertiesWithLabels(RdfResourceDefinition definition, String language) {
        return getAllPropertiesWithLabelsExceptThoseSpecified(definition, language, null);
    }       
    
     /**
     * Search all the properties corresponding to the given object uri
     * and fill the RDF Resource defintion object with the values and labels
     * 
     * @param definition The definition object which will be filled
     * @param language specify in which language labels should be returned.
     * @param propertiesRelationsToIgnore some relations sometimes must not be 
     * considered as properties so we ignore them
     * @return true    if the definition object is correctly filled
     *          false   if the uri doesn't exists
     */
    public boolean getAllPropertiesWithLabelsExceptThoseSpecified(RdfResourceDefinition definition, String language, ArrayList<String> propertiesRelationsToIgnore) {
        if (this.existUri(uri)) {
            /* Prepare and execute the query to retrieve all the relations, 
             properties and properties type with their labels for the given 
            uri and language*/
            SPARQLQueryBuilder query = prepareSearchPropertiesQuery(language, propertiesRelationsToIgnore);
            TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        
            definition.setUri(uri);
        
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();

                    Property property = new Property();

                    // 1. Affect the property
                    property.setValue(bindingSet.getValue(PROPERTY).stringValue());

                    // 2. Affect the relation
                    property.setRelation(bindingSet.getValue(RELATION).stringValue());
                    if (property.getRelation().equals(RDFS.label.toString())) {
                        definition.setLabel(property.getValue());
                    }
                    
                    // 3. affect the RDF type of the property if exists
                    if (bindingSet.hasBinding(PROPERTY_TYPE)) {
                        property.setRdfType(bindingSet.getValue(PROPERTY_TYPE).stringValue());
                    }

                    // 4. Add property label if exists
                    if (bindingSet.hasBinding(PROPERTY_LABEL)) {
                        property.addLastValueLabel(bindingSet.getValue(PROPERTY_LABEL).stringValue());
                    }
                    
                    // 5. Add relation label if exists
                    if (bindingSet.hasBinding(RELATION_LABEL)) {
                        property.addLastRelationLabel(bindingSet.getValue(RELATION_LABEL).stringValue());
                    }
                    
                    // 6. Add property rdf type label if exists
                    if (bindingSet.hasBinding(PROPERTY_TYPE_LABEL)) {
                        property.addLastRdfTypeLabel(bindingSet.getValue(PROPERTY_TYPE_LABEL).stringValue());
                    }

                    // 7. If definition already own the property, add current property labels to the existing property otherwise define prefered labels and add property to definition
                    if (definition.hasProperty(property)) {
                        // Retrieve the existing property
                        Property existingProperty = definition.getProperty(property);

                        // Prefered label are ignored in this case because they already are defined in the existing property
                        
                        // Merge new labels with previous existing
                        existingProperty.addRdfTypeLabels(property.getRdfTypeLabels());
                        existingProperty.addRelationLabels(property.getRelationLabels());
                        existingProperty.addValueLabels(property.getValueLabels());
                        
                        // Set the property variable with the existing property to add prefered labels if exists
                    } else {
                        // If property prefered label exists add it at the begining of labels array
                        if (bindingSet.hasBinding(PROPERTY_PREF_LABEL)) {
                            property.addFirstValueLabel(bindingSet.getValue(PROPERTY_PREF_LABEL).stringValue());
                        }

                        // If relation prefered label exists add it at the begining of labels array
                        if (bindingSet.hasBinding(RELATION_PREF_LABEL)) {
                            property.addFirstRelationLabel(bindingSet.getValue(RELATION_PREF_LABEL).stringValue());
                        }

                        // If property type prefered label exists add it at the begining of labels array
                        if (bindingSet.hasBinding(PROPERTY_TYPE_PREF_LABEL)) {
                            property.addFirstRdfTypeLabel(bindingSet.getValue(PROPERTY_TYPE_PREF_LABEL).stringValue());
                        }
                        
                        // Add property to definition
                        definition.addProperty(property);                    
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
