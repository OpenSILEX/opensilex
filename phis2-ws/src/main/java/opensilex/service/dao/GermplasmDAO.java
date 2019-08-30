//******************************************************************************
//                                GermplasmDAO.java
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
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.Germplasm;
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
 * Germplasm DAO
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class GermplasmDAO extends Rdf4jDAO<Germplasm> {
    final static Logger LOGGER = LoggerFactory.getLogger(GermplasmDAO.class);
    
    public String uri;

    private final String GERMPLASM = "germplasm";
    private final String GERMPLASM_LABEL = "germplasmLabel";
    private final String COMMON_CROP_NAME = "commonCropName";
    private final String GENUS = "genus";
    private final String SPECIES = "species";
    private final String SPECIES_LABEL = "speciesLabel";
    private final String VARIETY = "variety";
    private final String VARIETY_LABEL = "varietyLabel";
    private final String ACCESSION = "accession";
    private final String ACCESSION_NUMBER = "accessionNumber";
    private final String PLANT_MATERIAL_LOT = "plantMaterialLot";
    private final String INSTITUTE_CODE = "instituteCode";
    private final String INSTITUTE_NAME = "instituteName";
    
    private static final String MAX_ID = "maxID";

    public GermplasmDAO() {
    }       

    public GermplasmDAO(String uri) {
        this.uri = uri;
    }          
    
    @Override
    public List<Germplasm> create(List<Germplasm> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Germplasm> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Germplasm> update(List<Germplasm> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Germplasm find(Germplasm object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Germplasm> objects) throws DAOPersistenceException, DAODataErrorAggregateException, DAOPersistenceException, ResourceAccessDeniedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Checks and inserts the given germplasm in the triplestore.
     * @param germplasmList
     * @return the insertion result. Message error if errors founded in data
     *         the list of the generated URI of the sensors if the insertion has been done
     */
    public POSTResultsReturn checkAndInsert(List<Germplasm> germplasmList) {
        POSTResultsReturn checkResult = check(germplasmList);
        if (checkResult.getDataState()) {
            return insert(germplasmList);
        } else { //errors founded in data
            return checkResult;
                }
            }
    
    /**
     * Checks the given germplasm's metadata.
     * @param germplasmList
     * @return the result with the list of the errors founded (empty if no error founded)
     */
    public POSTResultsReturn check(List<Germplasm> germplasmList) {
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
        
            for (Germplasm germplasm:germplasmList) {                
                //Check if germplasm already exists
                if (existUriInGraph(germplasm.getGermplasmURI(), Contexts.GERMPLASM.toString())) {
                    dataOk = false;
                                checkStatus.add(new Status(
                                StatusCodeMsg.DATA_ERROR, 
                                StatusCodeMsg.ERR, 
                                "Germplasm already exists"));
                } else {
                    //Check if species is given
                    if (germplasm.getSpeciesURI() == null && germplasm.getSpeciesLabel() == null) {
                        dataOk = false;
                                checkStatus.add(new Status(
                                StatusCodeMsg.DATA_ERROR, 
                                StatusCodeMsg.ERR, 
                                "Species information missing (speciesLabel or speciesURI"));
                    } else {
                    
                        //Check if species correspond to the variety
                        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
                        query.appendGraph(Contexts.GERMPLASM.toString()); 
                        query.appendDistinct(Boolean.TRUE);

                        if (germplasm.getVarietyURI()!= null) {
                            query.appendSelect("?" + SPECIES);
                            query.appendTriplet("?" + GERMPLASM, Oeso.RELATION_HAS_VARIETY.toString(), "?" + VARIETY, null);
                            query.appendTriplet("?" + GERMPLASM, Oeso.RELATION_HAS_SPECIES.toString(), "?" + SPECIES, null);
                            query.appendAndFilter("REGEX ( str(?" + VARIETY + "),\".*" + germplasm.getVarietyURI() + ".*\",\"i\")");
                        }

                        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());

                        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());        
                        try (TupleQueryResult result = tupleQuery.evaluate()) {
                            while (result.hasNext()) {
                                BindingSet bindingSet = result.next();
                                if (bindingSet.getValue(SPECIES) != null) {
                                    if (!bindingSet.getValue(SPECIES).stringValue().equals(germplasm.getSpeciesURI())) {
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
     * Inserts the given germplasm in the triplestore.
     * @param germplasmList
     * @return the insertion result, with the errors list or the uri of the inserted
     *         germplasm
     */
    public POSTResultsReturn insert(List<Germplasm> germplasmList) {
        List<Status> insertStatus = new ArrayList<>();
        List<String> createdResourcesUri = new ArrayList<>();
        
        POSTResultsReturn results; 
        boolean resultState = false;
        boolean annotationInsert = true;

        //SILEX:test
        //Triplestore connection has to be checked (this is kind of an hot fix)
        this.getConnection().begin();
        //\SILEX:test
        
        for (Germplasm germplasm : germplasmList) {                     
           
            UpdateRequest query = prepareInsertQuery(germplasm);
            Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
            prepareUpdate.execute();
            
            createdResourcesUri.add(germplasm.getGermplasmURI());
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
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesUri.size() + " new germplasm(s) created"));
        }
        
        if (getConnection() != null) {
            getConnection().close();
        }
        
        return results;
    }
    
    /**
     * Generates an insert query for germplasm list.
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
     * @param germplasm
     * @return the query
     */
    private UpdateRequest prepareInsertQuery(Germplasm germplasm) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.GERMPLASM.toString());
        
        Resource germplasmURI = ResourceFactory.createResource(germplasm.getGermplasmURI());
        
        Node germplasmType = NodeFactory.createURI("http://www.opensilex.org/vocabulary/oeso#Germplasm");
        
        spql.addInsert(graph, germplasmURI, RDF.type, germplasmType);
        
//        if (germplasm.getGermplasmName() != null) {
//            spql.addInsert(graph, germplasmURI, RDFS.label, germplasm.getGermplasmName());
//        }
        
        if (germplasm.getSpeciesURI() != null) {
            Resource speciesURI = ResourceFactory.createResource(germplasm.getSpeciesURI());
            Property relationHasSpecies = ResourceFactory.createProperty(Oeso.RELATION_HAS_SPECIES.toString());
            spql.addInsert(graph, germplasmURI, relationHasSpecies, speciesURI);
        }
        
        if (germplasm.getVarietyURI()!= null) {
            Node varietyGraph = NodeFactory.createURI(Contexts.VARIETY.toString());
            Resource varietyURI = ResourceFactory.createResource(germplasm.getVarietyURI());
            spql.addInsert(varietyGraph, varietyURI, RDF.type, NodeFactory.createURI("http://www.opensilex.org/vocabulary/oeso#Variety"));
            Property relationHasVariety= ResourceFactory.createProperty(Oeso.RELATION_HAS_VARIETY.toString());
            spql.addInsert(graph, germplasmURI, relationHasVariety, varietyURI);
            if (germplasm.getVarietyLabel()!= null) {                
                spql.addInsert(varietyGraph, varietyURI, RDFS.label, germplasm.getVarietyLabel());
            }
        }
        
        if (germplasm.getAccessionURI() != null) {
            Node accessionGraph = NodeFactory.createURI(Contexts.ACCESSION.toString());
            Resource accessionURI = ResourceFactory.createResource(germplasm.getAccessionURI());
            spql.addInsert(accessionGraph, accessionURI, RDF.type, NodeFactory.createURI("http://www.opensilex.org/vocabulary/oeso#Accession"));
            Property relationHasAccessionNumber = ResourceFactory.createProperty(Oeso.RELATION_HAS_ACCESSION.toString());
            spql.addInsert(graph, germplasmURI, relationHasAccessionNumber, accessionURI);
            if (germplasm.getAccessionNumber()!= null) {
                spql.addInsert(accessionGraph, accessionURI, RDFS.label, germplasm.getAccessionNumber());
            }
        }
        
        if (germplasm.getSeedLots() != null) {
            Property relationHasSeedLot = ResourceFactory.createProperty(Oeso.RELATION_HAS_SEED_LOT.toString());
            ArrayList<String> lots = germplasm.getSeedLots();
            for (String lot:lots) {
                spql.addInsert(graph, germplasmURI, relationHasSeedLot, lot);
            }
        }
        
        if (germplasm.getInstituteCode()!= null) {
            Property relationHasInstituteCode = ResourceFactory.createProperty(Oeso.RELATION_HAS_INSTITUTE_CODE.toString());
            spql.addInsert(graph, germplasmURI, relationHasInstituteCode, germplasm.getInstituteCode());
        }
        
        if (germplasm.getInstituteName() != null) {
            Property relationHasInstituteName = ResourceFactory.createProperty(Oeso.RELATION_HAS_INSTITUTE_NAME.toString());
            spql.addInsert(graph, germplasmURI, relationHasInstituteName, germplasm.getInstituteName());
        }
             
        UpdateRequest query = spql.buildRequest();
        
        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());
        return query;
    }
    
    /**
     * Prepares a query to get the higher id of the germplasm.
     * @return 
     */
    private Query prepareGetLastId() {
        SelectBuilder query = new SelectBuilder();
        
        Var uri = makeVar(URI);
        Var maxID = makeVar(MAX_ID);
        
        // Select the highest identifier
        query.addVar(maxID);
        
        // Filter by variable
        Node methodConcept = NodeFactory.createURI(Oeso.CONCEPT_GERMPLASM.toString());
        query.addWhere(uri, RDF.type, methodConcept);
        
        // Binding to extract the last part of the URI as a MAX_ID integer
        ExprFactory expr = new ExprFactory();
        Expr indexBinding =  expr.function(
            XSD.integer.getURI(), 
            ExprList.create(Arrays.asList(
                expr.strafter(expr.str(uri), UriGenerator.PLATFORM_URI_ID_GERMPLASM))
            )
        );
        query.addBind(indexBinding, maxID);
        
        // Order MAX_ID integer from highest to lowest and select the first value
        query.addOrderBy(new SortCondition(maxID,  Query.ORDER_DESCENDING));
        query.setLimit(1);
        
        return query.build();

    }    
    
     /**
     * Gets the higher id of the variables.
     * @return the id
     */
    public int getLastId() {
        Query query = prepareGetLastId();
        //get last variable uri ID inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();

        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            return Integer.valueOf(bindingSet.getValue(MAX_ID).stringValue());
        } else {
            return 0;
        }
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
    private SPARQLQueryBuilder prepareSearchQuery(Integer page, Integer pageSize, String uri, String germplasmName, String commonCropName, String language) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        //query.appendGraph(Contexts.GERMPLASM.toString());
        
        //uri - germplasmDbId filter
        if (uri == null) {
            uri = "";
        }
        query.appendSelect("?" + URI);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_GERMPLASM.toString(), null);
        query.appendAndFilter("REGEX ( str(?" + URI + "),\".*" + uri + ".*\",\"i\")");
        
//        //germplasmName filter
//        query.appendSelect("?" + GERMPLASM_LABEL);
//        if (germplasmName == null) {
//            query.beginBodyOptional();
//            query.appendToBody("?" + URI + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + GERMPLASM_LABEL + " . ");
//            query.endBodyOptional();
//        } else {
//            query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
//            query.appendAndFilter("REGEX ( str(?" + LABEL + "),\".*" + germplasmName + ".*\",\"i\")");
//        }
        
        //genus
        query.appendSelect("?" + GENUS);
        query.beginBodyOptional();
        query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_GENUS.toString() + "> " + "?" + GENUS + " . ");
        query.endBodyOptional();
               
        //species
        query.appendSelect("?" + SPECIES_LABEL);
        query.beginBodyOptional();
        query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_SPECIES.toString() + "> " + "?" + SPECIES + " . ");
        query.appendToBody("?" + SPECIES + " <" + RDFS.label.toString() + "> " + "?" + SPECIES_LABEL + " . ");
        if (language == null) {
            language = "en"; //By default, we display the species in English
        }
        query.appendAndFilter("LANG(?" + SPECIES_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + SPECIES_LABEL + "), \"" + language + "\")");
        query.endBodyOptional();
        
        //variety
        query.appendSelect("?" + VARIETY_LABEL);
        query.beginBodyOptional();
        query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_VARIETY.toString() + "> " + "?" + VARIETY + " . ");
        query.appendToBody("?" + VARIETY + " <" + RDFS.label.toString() + "> " + "?" + VARIETY_LABEL + " . ");
        query.endBodyOptional();
        
        //accessionNumber
        query.appendSelect("?" + ACCESSION_NUMBER);        
        query.beginBodyOptional();
        query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_ACCESSION.toString() + "> " + "?" + ACCESSION + " . ");
        query.appendToBody("?" + ACCESSION + " <" + RDFS.label.toString() + "> " + "?" + ACCESSION_NUMBER + " . ");
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
    private SPARQLQueryBuilder prepareCount(String uri, String germplasmName, String commonCropName, String language) {
        SPARQLQueryBuilder query = this.prepareSearchQuery(null, null, uri, germplasmName, commonCropName, language);
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.appendSelect("(COUNT(DISTINCT ?" + URI + ") AS ?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        return query;
    }
    
    /**
     * Counts the number of germplasm by the given searched parameters.
     * @param uri
     * @param germplasmName
     * @param commonCropName
     * @return The number of germplasm
     * @inheritdoc
     */
    public Integer count(String uri, String germplasmName, String commonCropName, String language) {
        SPARQLQueryBuilder prepareCount = prepareCount(uri, germplasmName, commonCropName, language);
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
     * Search all the germplasm corresponding to the search params given.
     * @param page
     * @param pageSize
     * @param uri
     * @param germplasmName
     * @param commonCropName
     * @return the list of the germplasm.
     */
    public ArrayList<Germplasm> find(int page, int pageSize, String uri, String germplasmName, String commonCropName, String language) {
        SPARQLQueryBuilder query = prepareSearchQuery(page, pageSize, uri, germplasmName, commonCropName, language);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Germplasm> germplasmList = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Germplasm germplasm = getGermplasmFromBindingSet(bindingSet);
                germplasmList.add(germplasm);
            }
        }
        return germplasmList;
    }
    
    /**
     * Gets a germplasm from a given binding set.
     * Assume that the following attributes exist :
     *  uri, accessionName, accessionNumber, species, variety
     * @param bindingSet a bindingSet from a search query
     * @param uri
     * @param accessionName
     * @param accessionNumber
     * @param species
     * @param variety
     * @return a germplasm with data extracted from the given bindingSet
     */
    private Germplasm getGermplasmFromBindingSet(BindingSet bindingSet) {
        Germplasm germplasm = new Germplasm();
        
        if (bindingSet.getValue(URI) != null) {
            germplasm.setGermplasmURI(bindingSet.getValue(URI).stringValue());
        }
//        if (bindingSet.getValue(GERMPLASM_LABEL) != null) {
//            germplasm.setGermplasmName(bindingSet.getValue(GERMPLASM_LABEL).stringValue());
//        }
//        if (bindingSet.getValue(COMMON_CROP_NAME) != null) {
//            germplasm.setGermplasmURI(bindingSet.getValue(URI).stringValue());
//        }
//        if (bindingSet.getValue(GENUS) != null) {
//            germplasm.setGenus(bindingSet.getValue(GENUS).stringValue());
//        }
        if (bindingSet.getValue(SPECIES_LABEL) != null) {
            germplasm.setSpeciesLabel(bindingSet.getValue(SPECIES_LABEL).stringValue());
        }
        if (bindingSet.getValue(VARIETY_LABEL) != null) {
            germplasm.setVarietyLabel(bindingSet.getValue(VARIETY_LABEL).stringValue());
        }
        if (bindingSet.getValue(ACCESSION) != null) {
            germplasm.setAccessionURI(bindingSet.getValue(ACCESSION).stringValue());
        }
        if (bindingSet.getValue(ACCESSION_NUMBER) != null) {
            germplasm.setAccessionNumber(bindingSet.getValue(ACCESSION_NUMBER).stringValue());
        }
        if (bindingSet.getValue(INSTITUTE_CODE) != null) {
            germplasm.setInstituteCode(bindingSet.getValue(INSTITUTE_CODE).stringValue());
        }
        if (bindingSet.getValue(INSTITUTE_NAME) != null) {
            germplasm.setInstituteName(bindingSet.getValue(INSTITUTE_NAME).stringValue());
        }
        return germplasm;
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
    public Germplasm findById(String id) {
        SPARQLQueryBuilder query = prepareSearchByUri(id);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        
        Germplasm germplasm = new Germplasm();
        germplasm.setGermplasmURI(id);
        try(TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet row = result.next();

                if (germplasm.getSpeciesLabel()== null && row.getValue(SPECIES_LABEL) != null) {
                    germplasm.setSpeciesLabel(row.getValue(SPECIES_LABEL).stringValue());
                }
                
                if (germplasm.getVarietyLabel()== null && row.getValue(VARIETY_LABEL) != null) {
                    germplasm.setAccessionNumber(row.getValue(VARIETY_LABEL).stringValue());
                }
                
                if (germplasm.getAccessionNumber()== null && row.getValue(ACCESSION_NUMBER) != null) {
                    germplasm.setAccessionNumber(row.getValue(ACCESSION_NUMBER).stringValue());
                }                
            }
        }
        return germplasm;
    }

    private SPARQLQueryBuilder prepareSearchByUri(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        query.appendGraph(Contexts.GERMPLASM.toString());
        query.appendSelect(" ?" + URI + " ?" + SPECIES_LABEL + " ?" + VARIETY_LABEL + " ?" + ACCESSION_NUMBER);

        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_GERMPLASM.toString(), null);
        query.appendAndFilter("REGEX ( str(?" + URI + "),\".*" + uri + ".*\",\"i\")");
                
        //SPECIES
        query.appendTriplet("?" + URI, Oeso.RELATION_HAS_SPECIES.toString(), "?" + SPECIES_LABEL, null);
        
        //VARIETY
        query.beginBodyOptional();
        query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_VARIETY.toString() + "> " + "?" + VARIETY + " . ");
        query.appendToBody("?" + VARIETY + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + VARIETY_LABEL + " . ");
        query.endBodyOptional();
        
        //ACCESSION
        query.beginBodyOptional();
        query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_ACCESSION.toString() + "> " + "?" + ACCESSION + " . ");
        query.appendToBody("?" + ACCESSION + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + ACCESSION_NUMBER + " . ");
        query.endBodyOptional();
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        
        return query;}


    
}
