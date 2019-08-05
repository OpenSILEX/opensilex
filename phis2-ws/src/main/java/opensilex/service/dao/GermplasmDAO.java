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

    private final String GENUS = "genus";
    private final String SPECIES = "species";
    private final String VARIETY = "variety";
    private final String VARIETY_LABEL = "varietyLabel";
    private final String ACCESSION = "accession";
    private final String ACCESSION_NUMBER = "accessionNumber";
    //private final String PLANT_MATERIAL_LOT = "plantMaterialLot";    
    private final String INSTITUTE_CODE = "instituteCode";    
    private final String INSTITUTE_NAME = "instituteName"; 
    
    private static final String MAX_ID = "maxID";
    
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
    public Germplasm findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Germplasm> objects) throws DAOPersistenceException, DAODataErrorAggregateException, DAOPersistenceException, ResourceAccessDeniedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

/**
     * Checks and inserts the given germplasm in the triplestore.
     * @param germplasm List
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
     * @param germplasm
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
        }
     
        // todo : check consistency between species variety accession
        
        
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
    private SPARQLQueryBuilder prepareSearchQuery(Integer page, Integer pageSize, String uri, String accessionNumber, String germplasmName, String genus, String species, String variety, String instituteCode, String instituteName) {
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
        
        //germplasmName filter
        query.appendSelect("?" + LABEL);
        if (germplasmName == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + LABEL + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
            query.appendAndFilter("REGEX ( str(?" + LABEL + "),\".*" + germplasmName + ".*\",\"i\")");
        }
        
        //genus filter
        query.appendSelect("?" + GENUS);
        if (genus == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_GENUS.toString() + "> " + "?" + GENUS + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_GENUS.toString(), "?" + GENUS, null);
            query.appendAndFilter("REGEX ( str(?" + GENUS + "),\".*" + genus + ".*\",\"i\")");
        }
               
        //species filter
        query.appendSelect("?" + SPECIES);
        if (species == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_SPECIES.toString() + "> " + "?" + SPECIES + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_SPECIES.toString(), "?" + SPECIES, null);
            query.appendAndFilter("REGEX ( str(?" + SPECIES + "),\".*" + species + ".*\",\"i\")");
        }
        
        //variety filter
        query.appendSelect("?" + VARIETY);
        query.appendSelect("?" + VARIETY_LABEL);
        if (variety == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_VARIETY.toString() + "> " + "?" + VARIETY + " . ");
            query.appendToBody("?" + VARIETY + " <" + RDFS.label.toString() + "> " + "?" + VARIETY_LABEL + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_VARIETY.toString(), "?" + VARIETY, null);
            query.appendTriplet("?" + VARIETY, RDFS.label.toString(), "?" + VARIETY_LABEL, null);
            query.appendAndFilter("REGEX ( str(?" + VARIETY_LABEL + "),\".*" + variety + ".*\",\"i\")");
        }
        
        //accessionNumber filter
        query.appendSelect("?" + ACCESSION);
        query.appendSelect("?" + ACCESSION_NUMBER);
        if (accessionNumber == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_ACCESSION.toString() + "> " + "?" + ACCESSION + " . ");
            query.appendToBody("?" + ACCESSION + " <" + RDFS.label.toString() + "> " + "?" + ACCESSION_NUMBER + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_ACCESSION.toString(), "?" + ACCESSION, null);
            query.appendTriplet("?" + ACCESSION, RDFS.label.toString(), "?" + ACCESSION_NUMBER, null);
            query.appendAndFilter("REGEX ( str(?" + ACCESSION_NUMBER + "),\".*" + accessionNumber + ".*\",\"i\")");
        }
        
//        //instituteCode filter
//        query.appendSelect("?" + INSTITUTE_CODE);
//        if (instituteCode == null) {
//            query.beginBodyOptional();
//            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_INSTITUTE_CODE.toString() + "> " + "?" + INSTITUTE_CODE + " . ");
//            query.endBodyOptional();
//        } else {
//            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_INSTITUTE_CODE.toString(), "?" + INSTITUTE_CODE, null);
//            query.appendAndFilter("REGEX ( str(?" + INSTITUTE_CODE + "),\".*" + instituteCode + ".*\",\"i\")");
//        }    
//        
//        //instituteName filter
//        query.appendSelect("?" + INSTITUTE_NAME);
//        if (instituteCode == null) {
//            query.beginBodyOptional();
//            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_INSTITUTE_NAME.toString() + "> " + "?" + INSTITUTE_NAME + " . ");
//            query.endBodyOptional();
//        } else {
//            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_INSTITUTE_NAME.toString(), "?" + INSTITUTE_NAME, null);
//            query.appendAndFilter("REGEX ( str(?" + INSTITUTE_NAME + "),\".*" + instituteName + ".*\",\"i\")");
//        }    
               
        //query.appendSelect("?" + PLANT_MATERIAL_LOT);
        //query.appendTriplet("?" + URI, Oeso.RELATION_HAS_PLANT_MATERIAL_LOT.toString(), "?" + PLANT_MATERIAL_LOT, null);

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
    private SPARQLQueryBuilder prepareCount(String uri, String accessionName, String accessionNumber, String genus, String species, String variety, String instituteCode, String instituteName) {
        SPARQLQueryBuilder query = this.prepareSearchQuery(null, null, uri, accessionName, accessionNumber,genus, species, variety, instituteCode, instituteName);
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
     * @param accessionName
     * @param accessionNumber
     * @param species
     * @param variety
     * @return The number of germplasm
     * @inheritdoc
     */
    public Integer count(String uri, String accessionName, String accessionNumber, String genus, String species, String variety, String instituteCode, String instituteName) {
        SPARQLQueryBuilder prepareCount = prepareCount(uri, accessionName, accessionNumber, genus, species, variety, instituteCode, instituteName);
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
     * @param accessionName
     * @param accessionNumber
     * @param species
     * @param variety
     * @return the list of the germplasm.
     */
    public ArrayList<Germplasm> find(int page, int pageSize, String uri, String germplasmName, String accessionNumber, String genus, String species, String variety, String instituteCode, String instituteName) {
        SPARQLQueryBuilder query = prepareSearchQuery(page, pageSize, uri, accessionNumber, germplasmName, genus, species, variety, instituteCode, instituteName);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Germplasm> germplasmList = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Germplasm germplasm = getGermplasmFromBindingSet(bindingSet, uri, germplasmName, instituteName, genus, species, variety, instituteCode, instituteName);
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
    private Germplasm getGermplasmFromBindingSet(BindingSet bindingSet, String uri, String accessionName, String germplasmName, String genus, String species, String variety, String instituteCode, String instituteName) {
        Germplasm germplasm = new Germplasm();
        
        if (bindingSet.getValue(URI) != null) {
            germplasm.setGermplasmURI(bindingSet.getValue(URI).stringValue());
        }
        if (bindingSet.getValue(SPECIES) != null) {
            germplasm.setSpeciesURI(bindingSet.getValue(SPECIES).stringValue());
        }
        if (bindingSet.getValue(VARIETY) != null) {
            germplasm.setVarietyURI(bindingSet.getValue(VARIETY).stringValue());
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
        
        return germplasm;
    }
    
}
