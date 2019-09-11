//******************************************************************************
//                                AccessionDAO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 juil. 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.Accession;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Oeso;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.view.brapi.Status;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accession DAO
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class AccessionDAO extends Rdf4jDAO<Accession> {
    final static Logger LOGGER = LoggerFactory.getLogger(AccessionDAO.class);
    
    public String uri;
    
    private final String GENUS = "genus";
    private final String GENUS_LABEL = "genusLabel";
    private final String SPECIES = "species";
    private final String SPECIES_LABEL = "speciesLabel";
    private final String VARIETY = "variety";
    private final String VARIETY_LABEL = "varietyLabel";
    private final String ACCESSION_NUMBER = "accessionNumber";
    private final String ACCESSION_NAME = "accessionName";
    private final String INSTITUTE_CODE = "instituteCode";
    private final String INSTITUTE_NAME = "instituteName";
    
    private static final String MAX_ID = "maxID";

    public AccessionDAO() {
    }       

    public AccessionDAO(String uri) {
        this.uri = uri;
    }          
    
    @Override
    public List<Accession> create(List<Accession> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Accession> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Accession> update(List<Accession> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Accession find(Accession object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Accession> objects) throws DAOPersistenceException, DAODataErrorAggregateException, DAOPersistenceException, ResourceAccessDeniedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Checks and inserts the given accession in the triplestore.
     * @param accessions
     * @return the insertion result. Message error if errors founded in data
     *         the list of the generated URI of the sensors if the insertion has been done
     */
    public POSTResultsReturn checkAndInsert(List<Accession> accessions) throws Exception {
        POSTResultsReturn checkResult = check(accessions);
        if (checkResult.getDataState()) {
            return insert(accessions);
        } else { //errors founded in data
            return checkResult;
                }
            }
    
    /**
     * Checks the given accessions' metadata.
     * @param accessions
     * @return the result with the list of the errors founded (empty if no error founded)
     */
    public POSTResultsReturn check(List<Accession> accessions) {
        POSTResultsReturn check = null;
        //list of the returned results
        List<Status> checkStatus = new ArrayList<>();
        boolean dataOk = true;
        //1. check if user is an admin
        UserDAO userDao = new UserDAO();
        if (!userDao.isAdmin(user)) {
            dataOk = false;
            checkStatus.add(new Status(StatusCodeMsg.ACCESS_DENIED, StatusCodeMsg.ERR, StatusCodeMsg.ADMINISTRATOR_ONLY));
        } else {
        
            for (Accession accession:accessions) {                
                //Check if accession already exists
                if (existUriInGraph(accession.getAccessionURI(), Contexts.GENETIC_RESOURCE.toString())) {
                    dataOk = false;
                                checkStatus.add(new Status(
                                StatusCodeMsg.DATA_ERROR, 
                                StatusCodeMsg.ERR, 
                                "Germplasm already exists"));
                } else {
                    //Check if species is given
                    if (accession.getSpeciesURI() == null && accession.getSpeciesLabel() == null) {
                        dataOk = false;
                                checkStatus.add(new Status(
                                StatusCodeMsg.DATA_ERROR, 
                                StatusCodeMsg.ERR, 
                                "Species information missing (speciesLabel or speciesURI"));
                    } else {
                    
                        //Check if species correspond to the variety
                        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
                        query.appendGraph(Contexts.GENETIC_RESOURCE.toString()); 
                        query.appendDistinct(Boolean.TRUE);

                        if (accession.getVarietyURI()!= null) {
                            query.appendSelect("?" + SPECIES);
                            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_VARIETY.toString(), "?" + VARIETY, null);
                            query.appendTriplet("?" + VARIETY, Oeso.RELATION_HAS_SPECIES.toString(), "?" + SPECIES, null);
                            query.appendAndFilter("REGEX ( str(?" + VARIETY + "),\".*" + accession.getVarietyURI() + ".*\",\"i\")");
                        }

                        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());

                        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());        
                        try (TupleQueryResult result = tupleQuery.evaluate()) {
                            while (result.hasNext()) {
                                BindingSet bindingSet = result.next();
                                if (bindingSet.getValue(SPECIES) != null) {
                                    if (!bindingSet.getValue(SPECIES).stringValue().equals(accession.getSpeciesURI())) {
                                        dataOk = false;
                                        checkStatus.add(new Status(
                                        StatusCodeMsg.DATA_ERROR, 
                                        StatusCodeMsg.ERR, 
                                        "Bad species given, it does not correspond to the variety"));
                                    } 

                                }                        
                            }
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
     * Inserts the given accession in the triplestore.
     * @param accessions
     * @return the insertion result, with the errors list or the uri of the inserted
         accession
     */
    public POSTResultsReturn insert(List<Accession> accessions) throws Exception {
        List<Status> insertStatus = new ArrayList<>();
        List<String> createdResourcesUri = new ArrayList<>();
        
        POSTResultsReturn results; 
        boolean resultState = false;
        boolean annotationInsert = true;

        //SILEX:test
        //Triplestore connection has to be checked (this is kind of an hot fix)
        this.getConnection().begin();
        //\SILEX:test
        
        for (Accession accession : accessions) {                     
           
            UpdateRequest query = prepareInsertQuery(accession);
            Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
            prepareUpdate.execute();
            
            createdResourcesUri.add(accession.getAccessionURI());
        }
        
        if (annotationInsert) {
            resultState = true;
            getConnection().commit();
        } else {
            getConnection().rollback();
        }
        
        results = new POSTResultsReturn(resultState, annotationInsert, true);
        results.statusList = insertStatus;
        results.setCreatedResources(createdResourcesUri);
        if (resultState && !createdResourcesUri.isEmpty()) {
            results.createdResources = createdResourcesUri;
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesUri.size() + " new accession(s) created"));
        }
        
        if (getConnection() != null) {
            getConnection().close();
        }
        
        return results;
    }
    
    /**
     * Generates an insert query for accession list.
     * @example
     * INSERT DATA {
     *  GRAPH <http://www.phenome-fppn.fr/diaphen/sensors> { 
     *      <http://www.phenome-fppn.fr/platform/id/germplasm/g001>  rdf:type  <http://www.opensilex.org/vocabulary/oeso#Germplasm> . 
     *      <http://www.phenome-fppn.fr/platform/id/germplasm/g001>  rdfs:label  "B73_INRA"  . 
     *      <http://www.phenome-fppn.fr/platform/id/germplasm/g001>  <http://www.opensilex.org/vocabulary/oeso#hasGenus>  "Zea"  . 
     *      <http://www.phenome-fppn.fr/platform/id/germplasm/g001>  <http://www.opensilex.org/vocabulary/oeso#hasSpecies>  "http://aims.fao.org/aos/agrovoc/c_12332"  . 
     *      <http://www.phenome-fppn.fr/platform/id/germplasm/g001>  <http://www.opensilex.org/vocabulary/oeso#hasVariety>  "B73"  . 
     *      <http://www.phenome-fppn.fr/platform/id/germplasm/g001>  <http://www.opensilex.org/vocabulary/oeso#hasAccessionNumber>  "B73_INRA"  . 
     *  }
     * }
     * @param accession
     * @return the query
     */
    private UpdateRequest prepareInsertQuery(Accession accession) throws Exception {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.GENETIC_RESOURCE.toString());
        
        Resource accessionURI = ResourceFactory.createResource(accession.getAccessionURI());
        
        Node accessionType = NodeFactory.createURI(Oeso.CONCEPT_ACCESSION.toString());
        
        //insert accession
        spql.addInsert(graph, accessionURI, RDF.type, accessionType);
        
        //insert variety        
        Node varietyGraph = NodeFactory.createURI(Contexts.VARIETY.toString());
        Resource varietyURI = ResourceFactory.createResource(accession.getVarietyURI());
        spql.addInsert(varietyGraph, varietyURI, RDF.type, NodeFactory.createURI(Oeso.CONCEPT_VARIETY.toString()));
        Property relationHasAccession = ResourceFactory.createProperty(Oeso.RELATION_HAS_ACCESSION.toString());
        spql.addInsert(graph, varietyURI, relationHasAccession, accessionURI);
        if (accession.getVarietyLabel()!= null) {                
            spql.addInsert(varietyGraph, varietyURI, RDFS.label, accession.getVarietyLabel());
        }
        
        //add Link to species
        Resource speciesURI = ResourceFactory.createResource(accession.getSpeciesURI());
        Property relationHasVariety = ResourceFactory.createProperty(Oeso.RELATION_HAS_VARIETY.toString());
        spql.addInsert(graph, speciesURI, relationHasVariety, varietyURI);
        
        //add Link to genus (déjà en base ?)
        if (accession.getGenusURI()!= null) {
            Resource genusURI = ResourceFactory.createResource(accession.getSpeciesURI());
            Property relationHasSpecies = ResourceFactory.createProperty(Oeso.RELATION_HAS_SPECIES.toString());
            spql.addInsert(graph, genusURI, relationHasSpecies, speciesURI);                
        }
        
        //add accession properties
        Node accessionGraph = NodeFactory.createURI(Contexts.ACCESSION.toString());
        if (accession.getAccessionNumber()!= null) {
            Property relationHasAccessionNumber = ResourceFactory.createProperty(Oeso.RELATION_HAS_ACCESSION_NUMBER.toString());
            spql.addInsert(accessionGraph, accessionURI, relationHasAccessionNumber, accession.getAccessionNumber());
        }
       
        if (accession.getAccessionName()!= null) {
            spql.addInsert(accessionGraph, accessionURI, RDFS.label, accession.getAccessionName());
        }
        
        //add seedLots
        if (accession.getSeedLots() != null) {
            Property relationHasSeedLot = ResourceFactory.createProperty(Oeso.RELATION_HAS_PLANT_MATERIAL_LOT.toString());
            ArrayList<String> lots = accession.getSeedLots();
            Resource lotsGraph = ResourceFactory.createResource(Contexts.PLANT_MATERIAL_LOT.toString());
            for (String lot:lots) {
                String lotURI = UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_PLANT_MATERIAL_LOT.toString(), null, lot);
                Resource lotUri = ResourceFactory.createResource(lotURI);
                spql.addInsert(accessionGraph, accessionURI, relationHasSeedLot, lotUri);
                spql.addInsert(lotsGraph, lotUri, RDFS.label, lot);
                spql.addInsert(accessionGraph, lotUri, RDF.type, NodeFactory.createURI(Oeso.CONCEPT_PLANT_MATERIAL_LOT.toString()));
            }
        }
        
        if (accession.getInstituteCode()!= null) {
            Property relationHasInstituteCode = ResourceFactory.createProperty(Oeso.RELATION_HAS_INSTITUTE_CODE.toString());
            spql.addInsert(accessionGraph, accessionURI, relationHasInstituteCode, accession.getInstituteCode());
        }
        
        if (accession.getInstituteName() != null) {
            Property relationHasInstituteName = ResourceFactory.createProperty(Oeso.RELATION_HAS_INSTITUTE_NAME.toString());
            spql.addInsert(accessionGraph, accessionURI, relationHasInstituteName, accession.getInstituteName());
        }
             
        UpdateRequest query = spql.buildRequest();
        
        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());
        return query;
    }
    
//    /**
//     * Prepares a query to get the higher id of the accession.
//     * @return 
//     */
//    private Query prepareGetLastId() {
//        SelectBuilder query = new SelectBuilder();
//        
//        Var uri = makeVar(URI);
//        Var maxID = makeVar(MAX_ID);
//        
//        // Select the highest identifier
//        query.addVar(maxID);
//        
//        // Filter by Accession
//        Node methodConcept = NodeFactory.createURI(Oeso.CONCEPT_ACCESSION.toString());
//        query.addWhere(uri, RDF.type, methodConcept);
//        
//        // Binding to extract the last part of the URI as a MAX_ID integer
//        ExprFactory expr = new ExprFactory();
//        Expr indexBinding =  expr.function(
//            XSD.integer.getURI(), 
//            ExprList.create(Arrays.asList(
//                expr.strafter(expr.str(uri), UriGenerator.PLATFORM_URI_ID_GERMPLASM))
//            )
//        );
//        query.addBind(indexBinding, maxID);
//        
//        // Order MAX_ID integer from highest to lowest and select the first value
//        query.addOrderBy(new SortCondition(maxID,  Query.ORDER_DESCENDING));
//        query.setLimit(1);
//        
//        LOGGER.debug(SPARQL_QUERY + query.toString());
//        
//        return query.build();
//
//    }    
//    
//     /**
//     * Gets the higher id of the variables.
//     * @return the id
//     */
//    public int getLastId() {
//        Query query = prepareGetLastId();
//        //get last accession uri ID inserted
//        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
//        TupleQueryResult result = tupleQuery.evaluate();
//
//        if (result.hasNext()) {
//            BindingSet bindingSet = result.next();
//            return Integer.valueOf(bindingSet.getValue(MAX_ID).stringValue());
//        } else {
//            return 0;
//        }
//    }
    
    /**
     * Count query generated by the searched parameters given.
     * @param uri
     * @param accessionName
     * @param accessionNumber
     * @param species
     * @param variety
     * @example 
     * SELECT DISTINCT  (count(distinct ?uri) as ?count) 
     * WHERE {
     *      ?uri  ?0  ?rdfType  . 
     *      ?rdfType  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#SensingDevice> . 
     *      OPTIONAL {
     *          ?uri rdfs:label ?label . 
     *      }
     *      ?uri  <http://www.opensilex.org/vocabulary/oeso#hasBrand>  ?brand  . 
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#hasSerialNumber> ?serialNumber . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#inServiceDate> ?inServiceDate . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#dateOfPurchase> ?dateOfPurchase . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#dateOfLastCalibration> ?dateOfLastCalibration . 
     *      }
     *      ?uri  <http://www.opensilex.org/vocabulary/oeso#personInCharge>  ?personInCharge  . 
     * }
     * @return Query generated to count the elements, with the searched parameters
     */
    private SPARQLQueryBuilder prepareSearchQuery(Integer page, Integer pageSize, String uri, String accessionName, String species, String language) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        //uri - germplasmDbId filter
        if (uri == null) {
            uri = "";
        }
        query.appendSelect("?" + URI);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_ACCESSION.toString(), null);
        query.appendAndFilter("REGEX ( str(?" + URI + "),\".*" + uri + ".*\",\"i\")");
                       
        //variety
        query.appendSelect("?" + VARIETY_LABEL);
        query.appendToBody("?" + VARIETY + " <" + Oeso.RELATION_HAS_ACCESSION.toString() + "> " + "?" + URI + " . ");
        query.appendToBody("?" + VARIETY + " <" + RDFS.label.toString() + "> " + "?" + VARIETY_LABEL + " . ");
        
        //species
        if (language == null) {
            language = "en"; //By default, we display the species in English        
        }
        
        query.appendSelect("?" + SPECIES_LABEL);                    
        if (species == null) {
            query.appendToBody("?" + SPECIES + " <" + Oeso.RELATION_HAS_VARIETY.toString() + "> " + "?" + VARIETY + " . ");
            query.appendToBody("?" + SPECIES + " <" + RDFS.label.toString() + "> " + "?" + SPECIES_LABEL + " . ");
            query.appendAndFilter("LANG(?" + SPECIES_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + SPECIES_LABEL + "), \"" + language + "\")");
        } else {
            query.appendTriplet("?" + SPECIES, Oeso.RELATION_HAS_VARIETY.toString(), "?" + VARIETY, null);
            query.appendToBody("?" + SPECIES + " <" + RDFS.label.toString() + "> " + "?" + SPECIES_LABEL + " . ");
            query.appendAndFilter("REGEX ( str(?" + SPECIES_LABEL + "),\".*" + species + ".*\",\"i\")");
            query.appendAndFilter("LANG(?" + SPECIES_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + SPECIES_LABEL + "), \"" + language + "\")");
        }
        
        //genus
        query.appendSelect("?" + GENUS_LABEL);
        query.beginBodyOptional();
        query.appendToBody("?" + GENUS + " <" + Oeso.RELATION_HAS_SPECIES.toString() + "> " + "?" + SPECIES + " . ");
        query.appendToBody("?" + GENUS + " <" + RDFS.label.toString() + "> " + "?" + GENUS_LABEL + " . ");
        query.endBodyOptional();
        
        //accessionName filter
        query.appendSelect("?" + ACCESSION_NAME);
        if (accessionName == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + ACCESSION_NAME + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + ACCESSION_NAME, null);
            query.appendAndFilter("REGEX ( str(?" + LABEL + "),\".*" + accessionName + ".*\",\"i\")");
        }
        
        //accessionNumber
        query.appendSelect("?" + ACCESSION_NUMBER);        
        query.beginBodyOptional();
        query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_ACCESSION_NUMBER + "> " + "?" + ACCESSION_NUMBER + " . ");
        query.endBodyOptional();      
                      
        //query.appendSelect("?" + PLANT_MATERIAL_LOT);
        //query.appendTriplet("?" + URI, Oeso.RELATION_HAS_PLANT_MATERIAL_LOT.toString(), "?" + PLANT_MATERIAL_LOT, null);
        
        //instituteCode
        query.appendSelect("?" + INSTITUTE_CODE);        
        query.beginBodyOptional();
        query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_INSTITUTE_CODE.toString() + "> " + "?" + INSTITUTE_CODE + " . ");
        query.endBodyOptional();  
        
        //instituteName
        query.appendSelect("?" + INSTITUTE_NAME);        
        query.beginBodyOptional();
        query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_INSTITUTE_NAME.toString() + "> " + "?" + INSTITUTE_NAME + " . ");
        query.endBodyOptional();  

        if (page != null && pageSize != null) {
            query.appendLimit(pageSize);
            query.appendOffset(page * pageSize);
        }
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }
    /**
     * Count query generated by the searched parameters given.
     * @param uri
     * @param accessionName
     * @param accessionNumber
     * @param species
     * @param variety
     * @example 
     * SELECT DISTINCT  (count(distinct ?uri) as ?count) 
     * WHERE {
     *      ?uri  ?0  ?rdfType  . 
     *      ?rdfType  rdfs:subClassOf*  <http://www.opensilex.org/vocabulary/oeso#SensingDevice> . 
     *      OPTIONAL {
     *          ?uri rdfs:label ?label . 
     *      }
     *      ?uri  <http://www.opensilex.org/vocabulary/oeso#hasBrand>  ?brand  . 
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#hasSerialNumber> ?serialNumber . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#inServiceDate> ?inServiceDate . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#dateOfPurchase> ?dateOfPurchase . 
     *      }
     *      OPTIONAL {
     *          ?uri <http://www.opensilex.org/vocabulary/oeso#dateOfLastCalibration> ?dateOfLastCalibration . 
     *      }
     *      ?uri  <http://www.opensilex.org/vocabulary/oeso#personInCharge>  ?personInCharge  . 
     * }
     * @return Query generated to count the elements, with the searched parameters
     */
    private SPARQLQueryBuilder prepareCount(String uri, String accessionName, String species, String language) {
        SPARQLQueryBuilder query = this.prepareSearchQuery(null, null, uri, accessionName, species, language);
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.appendSelect("(COUNT(DISTINCT ?" + URI + ") AS ?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        return query;
    }
    
    /**
     * Counts the number of accession by the given searched parameters.
     * @param uri
     * @param accessionName
     * @param species
     * @return The number of accession
     * @inheritdoc
     */
    public Integer count(String uri, String accessionName, String species, String language) {
        SPARQLQueryBuilder prepareCount = prepareCount(uri, accessionName, species, language);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, prepareCount.toString());
        Integer count = 0;
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                count = Integer.parseInt(bindingSet.getValue(COUNT_ELEMENT_QUERY).stringValue());
            }
        }
        return count;
    }
    
    /**
     * Search all the accession corresponding to the search params given.
     * @param page
     * @param pageSize
     * @param uri
     * @param accessionName
     * @param species
     * @return the list of the accession.
     */
    public ArrayList<Accession> find(int page, int pageSize, String uri, String accessionName, String species, String language) {
        SPARQLQueryBuilder query = prepareSearchQuery(page, pageSize, uri, accessionName, species, language);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Accession> germplasmList = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Accession germplasm = getAccessionFromBindingSet(bindingSet);
                germplasmList.add(germplasm);
            }
        }
        return germplasmList;
    }
    
    /**
     * Gets a accession from a given binding set.
     * Assume that the following attributes exist :
     *  uri, accessionName, accessionNumber, species, variety
     * @param bindingSet a bindingSet from a search query
     * @param uri
     * @param accessionName
     * @param accessionNumber
     * @param species
     * @param variety
     * @return a accession with data extracted from the given bindingSet
     */
    private Accession getAccessionFromBindingSet(BindingSet bindingSet) {
        Accession accession = new Accession();
        
        if (bindingSet.getValue(URI) != null) {
            accession.setAccessionURI(bindingSet.getValue(URI).stringValue());
        }      
        if (bindingSet.getValue(GENUS) != null) {
            accession.setGenusLabel(bindingSet.getValue(GENUS).stringValue());
        }
        if (bindingSet.getValue(SPECIES_LABEL) != null) {
            accession.setSpeciesLabel(bindingSet.getValue(SPECIES_LABEL).stringValue());
        }
        if (bindingSet.getValue(VARIETY_LABEL) != null) {
            accession.setVarietyLabel(bindingSet.getValue(VARIETY_LABEL).stringValue());
        }        
        if (bindingSet.getValue(ACCESSION_NAME) != null) {
            accession.setAccessionName(bindingSet.getValue(ACCESSION_NAME).stringValue());
        }
        if (bindingSet.getValue(ACCESSION_NUMBER) != null) {
            accession.setAccessionNumber(bindingSet.getValue(ACCESSION_NUMBER).stringValue());
        }
        if (bindingSet.getValue(INSTITUTE_CODE) != null) {
            accession.setInstituteCode(bindingSet.getValue(INSTITUTE_CODE).stringValue());
        }
        if (bindingSet.getValue(INSTITUTE_NAME) != null) {
            accession.setInstituteName(bindingSet.getValue(INSTITUTE_NAME).stringValue());
        }
        return accession;
    }
    
    /**
     * Generates the SPARQL ask query to know if a given alias already 
     * exists in a given context
     * @param label
     * @param context
     * @return the query
     */
    public boolean askExistLabelInContext(String label, String context) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        query.appendGraph(context);
        query.appendAsk("");
        query.appendToBody("?x <" + Rdfs.RELATION_LABEL.toString() + "> \"" + label + "\"");
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        
        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query.toString());
                        boolean result = booleanQuery.evaluate();

        return result;
        
        
    }

    public String getURIFromLabel(String label) {
        String uri = null;
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        query.appendSelect("?" + URI);
        query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
        query.appendAndFilter("REGEX ( str(?" + LABEL + "),\".*" + label + ".*\",\"i\")");
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());        
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    if (bindingSet.getValue(URI) != null) {
                        uri = bindingSet.getValue(URI).stringValue();
                    } 
                }
            }                

        return uri;
    }
    
    @Override
    public Accession findById(String id) {
        SPARQLQueryBuilder query = prepareSearchByUri(id);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        
        Accession accession = new Accession();
        accession.setAccessionURI(id);
        try(TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet row = result.next();
                
                if (accession.getSpeciesLabel()== null && row.getValue(GENUS_LABEL) != null) {
                    accession.setSpeciesLabel(row.getValue(GENUS_LABEL).stringValue());
                }

                if (accession.getSpeciesLabel()== null && row.getValue(SPECIES_LABEL) != null) {
                    accession.setSpeciesLabel(row.getValue(SPECIES_LABEL).stringValue());
                }
                
                if (accession.getVarietyLabel()== null && row.getValue(VARIETY_LABEL) != null) {
                    accession.setAccessionNumber(row.getValue(VARIETY_LABEL).stringValue());
                }
                
                if (accession.getAccessionNumber()== null && row.getValue(ACCESSION_NUMBER) != null) {
                    accession.setAccessionNumber(row.getValue(ACCESSION_NUMBER).stringValue());
                }                
            }
        }
        return accession;
    }

    private SPARQLQueryBuilder prepareSearchByUri(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        query.appendSelect(" ?" + URI + " ?" + SPECIES_LABEL + " ?" + VARIETY_LABEL + " ?" + ACCESSION_NUMBER);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_ACCESSION.toString(), null);
        query.appendAndFilter("REGEX ( str(?" + URI + "),\".*" + uri + ".*\",\"i\")");
        
        //VARIETY
        query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_VARIETY.toString() + "> " + "?" + VARIETY + " . ");
        query.appendToBody("?" + VARIETY + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + VARIETY_LABEL + " . ");
                
        //SPECIES
        query.appendTriplet("?" + VARIETY, Oeso.RELATION_HAS_SPECIES.toString(), "?" + SPECIES, null);
        query.appendToBody("?" + SPECIES + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + SPECIES_LABEL + " . ");
        
        //GENUS
        query.appendTriplet("?" + SPECIES, Oeso.RELATION_HAS_GENUS.toString(), "?" + GENUS, null);
        query.appendToBody("?" + GENUS + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + GENUS_LABEL + " . ");        

        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        
        return query;}


    
}
