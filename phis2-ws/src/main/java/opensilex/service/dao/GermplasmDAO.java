//******************************************************************************
//                                GermplasmDAO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 juil. 2019
// Contact: Expression userEmail is undefined on line 6, column 15 in file:///home/boizetal/OpenSilex/phis-ws/phis2-ws/licenseheader.txt., anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import opensilex.service.PropertiesFileManager;
import static opensilex.service.dao.SensorDAO.LOGGER;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.Germplasm;
import opensilex.service.model.Sensor;
import opensilex.service.model.User;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Oeso;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.view.brapi.Status;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
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
    private final String ACCESSION_NUMBER = "accessionNumber";
    private final String PLANT_MATERIAL_LOT = "plantMaterialLot";    
    private final String INSTITUTE_CODE = "instituteCode";    
    private final String INSTITUTE_NAME = "instituteName"; 
    
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
        
        check = new POSTResultsReturn(dataOk, null, dataOk);
        check.statusList = checkStatus;
        return check;
    }
    
     /**
     * Inserts the given germplasm in the triplestore.
     * @param germplasm List
     * @return the insertion result, with the errors list or the uri of the inserted
     *         germplasm
     */
    public POSTResultsReturn insert(List<Germplasm> germplasmList) {
        List<Status> insertStatus = new ArrayList<>();
        List<String> createdResourcesUri = new ArrayList<>();
        
        POSTResultsReturn results; 
        boolean resultState = false;
        boolean annotationInsert = true;
        
        UriGenerator uriGenerator = new UriGenerator();
        
        //SILEX:test
        //Triplestore connection has to be checked (this is kind of an hot fix)
        this.getConnection().begin();
        //\SILEX:test
        
        for (Germplasm germplasm : germplasmList) {
            try {
                germplasm.setGermplasmDbId(uriGenerator.generateNewInstanceUri(Oeso.CONCEPT_GERMPLASM.toString(), null, null));
            } catch (Exception ex) { //In the sensors case, no exception should be raised
                annotationInsert = false;
            }
            
            UpdateRequest query = prepareInsertQuery(germplasm);
            Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
            prepareUpdate.execute();
            
            createdResourcesUri.add(germplasm.getGermplasmDbId());
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
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesUri.size() + " new resource(s) created"));
        }
        
        if (getConnection() != null) {
            getConnection().close();
        }
        
        return results;
    }
    
        /**
     * Generates an insert query for sensors.
     * @example
     * INSERT DATA {
     *  GRAPH <http://www.phenome-fppn.fr/diaphen/sensors> { 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  rdf:type  <http://www.opensilex.org/vocabulary/oeso#Thermocouple> . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  rdfs:label  "par03_p"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#hasBrand>  "Homemade"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#inServiceDate>  "2017-06-15"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#personInCharge>  "morgane.vidal@inra.fr"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#serialNumber>  "A1E345F32"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#dateOfPurchase>  "2017-06-15"  . 
     *      <http://www.phenome-fppn.fr/diaphen/2018/v18142>  <http://www.opensilex.org/vocabulary/oeso#dateOfLastCalibration>  "2017-06-15"  . 
     *  }
     * }
     * @param sensor
     * @return the query
     */
    private UpdateRequest prepareInsertQuery(Germplasm germplasm) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.GERMPLASM.toString());
        
        Resource germplasmURI = ResourceFactory.createResource(germplasm.getGermplasmDbId());
        
        Node germplasmType = NodeFactory.createURI("http://www.opensilex.org/vocabulary/oeso#GermplasmDescriptor");
        
        spql.addInsert(graph, germplasmURI, RDF.type, germplasmType);
        
        spql.addInsert(graph, germplasmURI, RDFS.label, germplasm.getGermplasmName());
        
        if (germplasm.getSpecies() != null) {
            Property relationHasSpecies = ResourceFactory.createProperty(Oeso.RELATION_HAS_SPECIES.toString());
            spql.addInsert(graph, germplasmURI, relationHasSpecies, germplasm.getSpecies());
        }
        if (germplasm.getSubtaxa()!= null) {
            Property relationHasVariety= ResourceFactory.createProperty(Oeso.RELATION_HAS_VARIETY.toString());
            spql.addInsert(graph, germplasmURI, relationHasVariety, germplasm.getSubtaxa());
        }
        if (germplasm.getAccessionNumber() != null) {
            Property relationHasAccessionNumber = ResourceFactory.createProperty(Oeso.RELATION_HAS_ACCESSION_NUMBER.toString());
            spql.addInsert(graph, germplasmURI, relationHasAccessionNumber, germplasm.getAccessionNumber());
        }
        if (germplasm.getInstituteCode()!= null) {
            Property relationFromInstitute = ResourceFactory.createProperty(Oeso.RELATION_HAS_INSTITUTE_CODE.toString());
            spql.addInsert(graph, germplasmURI, relationFromInstitute, germplasm.getInstituteCode());    
        }
        if (germplasm.getInstituteName()!= null) {
            Property relationFromInstitute = ResourceFactory.createProperty(Oeso.RELATION_HAS_INSTITUTE_NAME.toString());
            spql.addInsert(graph, germplasmURI, relationFromInstitute, germplasm.getInstituteName());    
        }
        if (germplasm.getCommonCropName()!= null) {
            Property relationFromInstitute = ResourceFactory.createProperty(Oeso.RELATION_HAS_COMMON_CROP_NAME.toString());
            spql.addInsert(graph, germplasmURI, relationFromInstitute, germplasm.getCommonCropName());    
        }
        if (germplasm.getDocumentationURL()!= null) {
            Property relationFromInstitute = ResourceFactory.createProperty(Oeso.RELATION_HAS_DOCUMENTATION_URL.toString());
            spql.addInsert(graph, germplasmURI, relationFromInstitute, germplasm.getDocumentationURL());    
        }

        
        
        
        
//        if (germplasm.getPlantMaterialLots() != null) {
//            Property relationHasPlantMaterialLot = ResourceFactory.createProperty(Oeso.RELATION_HAS_PLANT_MATERIAL_LOT.toString());
//            ArrayList<String> lots = germplasm.getPlantMaterialLots();
//            for (String lot:lots) {
//                spql.addInsert(graph, germplasmURI, relationHasPlantMaterialLot, lot);
//            }
//        }

        
        UpdateRequest query = spql.buildRequest();
        
        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());
        return query;
    }
    
    /**
     * Prepares a query to get the higher id of the variables.
     * @return 
     */
    private SPARQLQueryBuilder prepareGetLastId() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?" + URI);
        query.appendTriplet("?uri", Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_GERMPLASM.toString(), null);
        query.appendOrderBy("DESC(?" + URI + ")");
        query.appendLimit(1);
        
        return query;
    }
    
    
     /**
     * Gets the higher id of the variables.
     * @return the id
     */
    public int getLastId() {
        SPARQLQueryBuilder query = prepareGetLastId();
        
        //SILEX:test
        //All the triplestore connection has to been checked and updated
        //This is an unclean hot fix
        String tripleStoreServer = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "sesameServer");
        String repositoryID = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "repositoryID");
        rep = new HTTPRepository(tripleStoreServer, repositoryID); //Stockage triplestore
        rep.initialize();
        this.setConnection(rep.getConnection());
        this.getConnection().begin();
        //\SILEX:test

        //get last variable uri inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();

        //SILEX:test
        //For the pool connection problems
        getConnection().commit();
        getConnection().close();
        //\SILEX:test
        
        String uriVariable = null;
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            uriVariable = bindingSet.getValue(URI).stringValue();
        }
        
        if (uriVariable == null) {
            return 0;
        } else {
            String split = "variables/v";
            String[] parts = uriVariable.split(split);
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            } else {
                return 0;
            }
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
        query.appendGraph(Contexts.GERMPLASM.toString());
        
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
        if (species == null) {
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
        if (variety == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_VARIETY.toString() + "> " + "?" + VARIETY + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_VARIETY.toString(), "?" + VARIETY, null);
            query.appendAndFilter("REGEX ( str(?" + VARIETY + "),\".*" + variety + ".*\",\"i\")");
        }
        
        //accessionNumber filter
        query.appendSelect("?" + ACCESSION_NUMBER);
        if (accessionNumber == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_ACCESSION_NUMBER.toString() + "> " + "?" + ACCESSION_NUMBER + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_ACCESSION_NUMBER.toString(), "?" + ACCESSION_NUMBER, null);
            query.appendAndFilter("REGEX ( str(?" + ACCESSION_NUMBER + "),\".*" + accessionNumber + ".*\",\"i\")");
        }
        
        //instituteCode filter
        query.appendSelect("?" + INSTITUTE_CODE);
        if (instituteCode == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_INSTITUTE_CODE.toString() + "> " + "?" + INSTITUTE_CODE + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_INSTITUTE_CODE.toString(), "?" + INSTITUTE_CODE, null);
            query.appendAndFilter("REGEX ( str(?" + INSTITUTE_CODE + "),\".*" + instituteCode + ".*\",\"i\")");
        }    
        
        //instituteName filter
        query.appendSelect("?" + INSTITUTE_NAME);
        if (instituteCode == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_INSTITUTE_NAME.toString() + "> " + "?" + INSTITUTE_NAME + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_INSTITUTE_NAME.toString(), "?" + INSTITUTE_NAME, null);
            query.appendAndFilter("REGEX ( str(?" + INSTITUTE_NAME + "),\".*" + instituteName + ".*\",\"i\")");
        }    
               
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
    public ArrayList<Germplasm> find(int page, int pageSize, String uri, String accessionName, String accessionNumber, String genus, String species, String variety, String instituteCode, String instituteName) {
        SPARQLQueryBuilder query = prepareSearchQuery(page, pageSize, uri, accessionNumber, accessionName, genus, species, variety, instituteCode, instituteName);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Germplasm> germplasmList = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Germplasm germplasm = getGermplasmFromBindingSet(bindingSet, uri, accessionName, instituteName, genus, species, variety, instituteCode, instituteName);
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
            germplasm.setGermplasmDbId(bindingSet.getValue(URI).stringValue());
        }
        if (bindingSet.getValue(SPECIES) != null) {
            germplasm.setSpecies(bindingSet.getValue(SPECIES).stringValue());
        }
        if (bindingSet.getValue(VARIETY) != null) {
            germplasm.setSubtaxa(bindingSet.getValue(VARIETY).stringValue());
        }
        if (bindingSet.getValue(LABEL) != null) {
            germplasm.setGermplasmName(bindingSet.getValue(LABEL).stringValue());
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
//        if (bindingSet.getValue(PLANT_MATERIAL_LOT) != null) {
//            String lot = bindingSet.getValue(PLANT_MATERIAL_LOT).stringValue();
//            ArrayList<String> lots = new ArrayList();
//            lots.add(lot);
//            germplasm.setPlantMaterialLots(lots);
//        }
        
        return germplasm;
    }


    
}
