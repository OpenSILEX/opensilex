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
import java.util.Map;
import java.util.logging.Level;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.Germplasm;
import opensilex.service.model.Property;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Oeso;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.resource.dto.germplasm.BrapiGermplasmDTO;
import opensilex.service.resource.dto.germplasm.GermplasmDTO;
import opensilex.service.resource.dto.rdfResourceDefinition.PropertyPostDTO;
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
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_StrLang;
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
public class GermplasmDAO extends Rdf4jDAO<Germplasm> {
    final static Logger LOGGER = LoggerFactory.getLogger(GermplasmDAO.class);
    
    public String uri;
    
    private final String GENUS = "genus";
    private final String GENUS_LABEL = "genusLabel";
    private final String SPECIES = "species";
    private final String SPECIES_LABEL = "speciesLabel";
    private final String VARIETY = "variety";
    private final String VARIETY_LABEL = "varietyLabel";
    private final String ACCESSION = "accession";
    private final String ACCESSION_LABEL = "accessionLabel";
    private final String LOT = "lot";
    private final String LOT_LABEL = "lotLabel";
    private final String INSTITUTE_CODE = "instituteCode";
    private final String INSTITUTE_NAME = "instituteName";
    private final String RELATION = "relation";
    private final String PROPERTY = "property";
    private final String PROPERTY_TYPE = "propertyType";
    private final String GERMPLASM_LABEL = "germplasmLabel";
    private final String GERMPLASM_TYPE = "germplasmType";
    private final String GERMPLASM = "germplasm";    
    
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
     * Checks and inserts the given accession in the triplestore.
     * @param accessions
     * @return the insertion result. Message error if errors founded in data
     *         the list of the generated URI of the sensors if the insertion has been done
     */
    public POSTResultsReturn checkAndInsert(List<Germplasm> germplasms) throws Exception {
        POSTResultsReturn checkResult = check(germplasms);
        if (checkResult.getDataState()) {
            return insert(germplasms);
        } else { //errors founded in data
            return checkResult;
                }
            }
    
    /**
     * Checks the given accessions' metadata.
     * @param germplasms
     * @return the result with the list of the errors founded (empty if no error founded)
     */
    public POSTResultsReturn check(List<Germplasm> germplasms) {
        POSTResultsReturn check = null;
        //list of the returned results
        List<Status> checkStatusList = new ArrayList<>();
        boolean dataOk = true;
        
        //Caches
        List<String> propertyUriCache = new ArrayList<>();        
        
        //1. check if user is an admin
        UserDAO userDao = new UserDAO();
        if (!userDao.isAdmin(user)) {
            dataOk = false;
            checkStatusList.add(new Status(StatusCodeMsg.ACCESS_DENIED, StatusCodeMsg.ERR, StatusCodeMsg.ADMINISTRATOR_ONLY));
        } else {
            //2. check data
            for (Germplasm germplasm:germplasms) { 
                try {
                    //2.1 Check type (subclass of Germplasm)
                    UriDAO uriDao = new UriDAO();
                    if (!uriDao.isSubClassOf(germplasm.getRdfType(), Oeso.CONCEPT_GERMPLASM.toString())) {
                        dataOk = false;
                        checkStatusList.add(new Status(
                                StatusCodeMsg.DATA_ERROR, 
                                StatusCodeMsg.ERR, 
                                "Bad germplasm type given. Must be sublass of Germplasm concept (variety, accession, seedLot)"));
                    }                
                
                    //2.2 Check the label
                    Map<String, List<String>> germplasmUrisAndLabels = this.findUriAndLabelsByLabelAndRdfType(germplasm.getLabel(), germplasm.getRdfType());
                    if (!germplasmUrisAndLabels.isEmpty()) {
                        dataOk = false;
                        checkStatusList.add(new Status(
                                StatusCodeMsg.DATA_ERROR, 
                                StatusCodeMsg.ERR, 
                                "The label already exists"));
                    }
                    
                    //2.3 Check properties

                    boolean missingLink = true;
                    
                    for (Property property : germplasm.getProperties()) {
                        //Check link to others germplasm instances                           
                        if (property.getRelation().equals(Oeso.RELATION_FROM_ACCESSION.toString())) {
                            if (uriDao.isSubClassOf(germplasm.getRdfType(), Oeso.CONCEPT_PLANT_MATERIAL_LOT.toString())) {
                                if (!existUriInGraph(property.getValue(), Contexts.GERMPLASM.toString())) {
                                    dataOk = false;
                                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "the accession URI doesn't exist"));
                                } else {
                                    missingLink = false;
                                }
                            } else {
                                dataOk = false;
                                checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "the relation fromAccession can't have this type of germplasm"));
                            }

                        } else if (property.getRelation().equals(Oeso.RELATION_FROM_VARIETY.toString())) {
                            if (uriDao.isSubClassOf(germplasm.getRdfType(), Oeso.CONCEPT_PLANT_MATERIAL_LOT.toString()) | germplasm.getRdfType().equals(Oeso.CONCEPT_ACCESSION.toString())) {
                                if (!existUriInGraph(property.getValue(), Contexts.GERMPLASM.toString())) {
                                    dataOk = false;
                                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "the variety URI doesn't exist"));
                                } else {
                                    missingLink = false;
                                }
                            } else {
                                dataOk = false;
                                checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "the relation fromVariety can't have this type of germplasm"));
                            }

                        } else if (property.getRelation().equals(Oeso.RELATION_FROM_SPECIES.toString())) {
                            if (uriDao.isSubClassOf(germplasm.getRdfType(), Oeso.CONCEPT_PLANT_MATERIAL_LOT.toString()) | germplasm.getRdfType().equals(Oeso.CONCEPT_ACCESSION.toString()) | germplasm.getRdfType().equals(Oeso.CONCEPT_VARIETY.toString())) {
                                if (!existUriInGraph(property.getValue(), Contexts.GERMPLASM.toString())) {
                                    dataOk = false;
                                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "the species URI doesn't exist"));
                                } else {
                                    missingLink = false;
                                }
                            }else {
                                dataOk = false;
                                checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "the relation fromSpecies can't have this type of germplasm"));
                            }
                        } else if (property.getRelation().equals(Oeso.RELATION_FROM_GENUS.toString())) {
                            if (germplasm.getRdfType().equals(Oeso.CONCEPT_SPECIES.toString())) {
                                if (!existUriInGraph(property.getValue(), Contexts.GERMPLASM.toString())) {
                                    dataOk = false;
                                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "the genus URI doesn't exist"));
                                } 
                            }else {
                                dataOk = false;
                                checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "the relation fromGenus must link a species and a genus"));
                            }

                        } else {
                            //Check if property exists in the ontology Vocabulary --> see how to check rdfs
                            if (!propertyUriCache.contains(property.getRelation())) {
                                if (existUri(property.getRelation()) == false) {
                                    dataOk = false;
                                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "the property relation " + property.getRelation() + " doesn't exist in the ontology"));
                                } else {
                                    propertyUriCache.add(property.getRelation());
                                }
                            }
                        }
                            
                        
                    }
                    if (missingLink && !germplasm.getRdfType().equals(Oeso.CONCEPT_GENUS.toString()) && !germplasm.getRdfType().equals(Oeso.CONCEPT_SPECIES.toString()) ) {
                        dataOk = false;
                        checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "a relation to another germplasm type is required (at least the species)"));
                    }
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(GermplasmDAO.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
//                if (existUriInGraph(accession.getAccessionURI(), Contexts.GENETIC_RESOURCE.toString())) {
//                    dataOk = false;
//                                checkStatus.add(new Status(
//                                StatusCodeMsg.DATA_ERROR, 
//                                StatusCodeMsg.ERR, 
//                                "Germplasm already exists"));
//                } else {
//                    //Check if species is given
//                    if (accession.getSpeciesURI() == null && accession.getSpeciesLabel() == null) {
//                        dataOk = false;
//                                checkStatus.add(new Status(
//                                StatusCodeMsg.DATA_ERROR, 
//                                StatusCodeMsg.ERR, 
//                                "Species information missing (speciesLabel or speciesURI"));
//                    } else {
//                    
//                        //Check if species correspond to the variety
//                        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
//                        query.appendGraph(Contexts.GENETIC_RESOURCE.toString()); 
//                        query.appendDistinct(Boolean.TRUE);
//
//                        if (accession.getVarietyURI()!= null) {
//                            query.appendSelect("?" + SPECIES);
//                            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_VARIETY.toString(), "?" + VARIETY, null);
//                            query.appendTriplet("?" + VARIETY, Oeso.RELATION_HAS_SPECIES.toString(), "?" + SPECIES, null);
//                            query.appendAndFilter("REGEX ( str(?" + VARIETY + "),\".*" + accession.getVarietyURI() + ".*\",\"i\")");
//                        }
//
//                        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());
//
//                        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());        
//                        try (TupleQueryResult result = tupleQuery.evaluate()) {
//                            while (result.hasNext()) {
//                                BindingSet bindingSet = result.next();
//                                if (bindingSet.getValue(SPECIES) != null) {
//                                    if (!bindingSet.getValue(SPECIES).stringValue().equals(accession.getSpeciesURI())) {
//                                        dataOk = false;
//                                        checkStatus.add(new Status(
//                                        StatusCodeMsg.DATA_ERROR, 
//                                        StatusCodeMsg.ERR, 
//                                        "Bad species given, it does not correspond to the variety"));
//                                    } 
//
//                                }                        
//                            }
//                        } 
//                    }
//                }
            
        }      
        
        check = new POSTResultsReturn(dataOk, null, dataOk);
        check.statusList = checkStatusList;
        return check;
    }  
    
     /**
     * Inserts the given accession in the triplestore.
     * @param germplasms
     * @return the insertion result, with the errors list or the uri of the inserted
         accession
     */
    public POSTResultsReturn insert(List<Germplasm> germplasms) throws Exception {
        List<Status> insertStatus = new ArrayList<>();
        List<String> createdResourcesUri = new ArrayList<>();
        
        POSTResultsReturn results; 
        boolean resultState = false;
        boolean annotationInsert = true;

        //SILEX:test
        //Triplestore connection has to be checked (this is kind of an hot fix)
        this.getConnection().begin();
        //\SILEX:test
        
        for (Germplasm germplasm : germplasms) {    
            if (germplasm.getUri() == null) {
                germplasm.setUri(UriGenerator.generateNewInstanceUri(germplasm.getRdfType(),null, germplasm.getLabel()));
            }
            
            UpdateRequest query = prepareInsertQuery(germplasm);
            Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
            prepareUpdate.execute();
            
            createdResourcesUri.add(germplasm.getUri());
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
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesUri.size() + " new germplasm created"));
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
     *  GRAPH <http://www.phenome-fppn.fr/platform/germplasm> { 
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
    private UpdateRequest prepareInsertQuery(Germplasm germplasm) {
        
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.GERMPLASM.toString());
        
        Resource uri = ResourceFactory.createResource(germplasm.getUri());
        
        Node germplasmType = NodeFactory.createURI(germplasm.getRdfType());
        
        //insert germplasm
        spql.addInsert(graph, uri, RDF.type, germplasmType);
        if (germplasm.getLabel() != null) {
            spql.addInsert(graph, uri, RDFS.label, germplasm.getLabel());
        }
        
        //insert property
        for (Property property : germplasm.getProperties()) {
            org.apache.jena.rdf.model.Property propertyRelation = ResourceFactory.createProperty(property.getRelation());
            
            if (property.getRdfType() != null) {
                Node propertyValue = NodeFactory.createURI(property.getValue());
                spql.addInsert(graph, uri, propertyRelation, propertyValue);
            } else {
                if (property.getRelation().equals(Rdfs.RELATION_LABEL.toString())) {
                    String[] label = property.getValue().split("@");
                    Literal propertyValue = ResourceFactory.createLangLiteral(label[0],label[1]);
                    spql.addInsert(graph, uri, propertyRelation, propertyValue);
                } else {
                    Literal propertyValue = ResourceFactory.createStringLiteral(property.getValue());
                    spql.addInsert(graph, uri, propertyRelation, propertyValue);
                }
                
            }
        }
             
        UpdateRequest query = spql.buildRequest();
               
        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());
        return query;
    }
    
    /**
     * Prepares a query to get the higher id of the accession.
     * @return 
     */
    private Query prepareGetLastId() {
        SelectBuilder query = new SelectBuilder();
        
        Var uri = makeVar(URI);
        Var maxID = makeVar(MAX_ID);
        
        // Select the highest identifier
        query.addVar(maxID);
        
        // Filter by Accession
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
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        
        return query.build();

    }    
    
     /**
     * Gets the higher id of the variables.
     * @return the id
     */
    public int getLastId() {
        Query query = prepareGetLastId();
        //get last accession uri ID inserted
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
     * @param germplasmName
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
    private SPARQLQueryBuilder prepareSearchQuery(Integer page, Integer pageSize, String uri, String label, String germplasmType, String language, String fromGenus, String fromSpecies, String fromVariety, String fromAccession) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        //uri - germplasmDbId filter
        query.appendSelect("?" + URI);
        if (uri == null) {
            uri = "";
        }
        
        query.appendFrom("<" + Contexts.GERMPLASM.toString() + ">");
        query.appendAndFilter("REGEX ( str(?" + URI + "),\".*" + uri + ".*\",\"i\")");       

        //germplasmType filter
        if (germplasmType == null) {
            germplasmType = "";
        }
        query.appendSelect("?" + RDF_TYPE);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        //query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">", Oeso.CONCEPT_GERMPLASM.toString(), null);
        query.appendAndFilter("REGEX ( str(?" + RDF_TYPE + "),\".*" + germplasmType + ".*\",\"i\")");
        
        //Label filter
        query.appendSelect("?" + LABEL);
        if (label == null) {
            query.appendToBody("?" + URI + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + LABEL + " . ");
            if (language != null) {
                query.appendAndFilter("LANG(?" + LABEL + ") = \"\" || LANGMATCHES(LANG(?" + LABEL + "), \"" + language + "\")");
            }
        
        } else {
            query.appendTriplet("?" + URI, Rdfs.RELATION_LABEL.toString(), "?" + LABEL, null);
            query.appendAndFilter("REGEX ( str(?" + LABEL + "),\".*" + label + ".*\",\"i\")");
            if (language != null) {
                query.appendAndFilter("LANG(?" + LABEL + ") = \"\" || LANGMATCHES(LANG(?" + LABEL + "), \"" + language + "\")");
            }    
        }
        
        //fromGenus filter
        if (fromGenus != null) {
            query.appendToBody("{ ?" + URI + " <" + Oeso.RELATION_FROM_GENUS.toString() + "> " + "?" + GENUS + " }");
            query.appendToBody(" UNION ");
            query.appendToBody("{ ?" + SPECIES + " <" + Oeso.RELATION_FROM_GENUS.toString() + "> " + "?" + GENUS + " .");
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_FROM_SPECIES.toString() + "> " + "?" + SPECIES + " }");
            query.appendToBody(" UNION ");
            query.appendToBody("{ ?" + SPECIES + " <" + Oeso.RELATION_FROM_GENUS.toString() + "> " + "?" + GENUS + " .");
            query.appendToBody("?" + VARIETY + " <" + Oeso.RELATION_FROM_SPECIES.toString() + "> " + "?" + SPECIES + " .");
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_FROM_VARIETY.toString() + "> " + "?" + VARIETY + " }");
            query.appendToBody(" UNION ");
            query.appendToBody("{ ?" + SPECIES + " <" + Oeso.RELATION_FROM_GENUS.toString() + "> " + "?" + GENUS + " .");
            query.appendToBody("?" + VARIETY + " <" + Oeso.RELATION_FROM_SPECIES.toString() + "> " + "?" + SPECIES + " .");
            query.appendToBody("?" + ACCESSION + " <" + Oeso.RELATION_FROM_VARIETY.toString() + "> " + "?" + VARIETY + " .");
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_FROM_ACCESSION.toString() + "> " + "?" + ACCESSION + " }");
            query.appendAndFilter("REGEX ( str(?" + GENUS + "),\".*" + fromGenus + ".*\",\"i\")");
        }
        
        //fromSpecies filter
        if (fromSpecies != null) {
            query.appendToBody("{ ?" + URI + " <" + Oeso.RELATION_FROM_SPECIES.toString() + "> " + "?" + SPECIES + " }");
            query.appendToBody(" UNION ");
            query.appendToBody("{ ?" + VARIETY + " <" + Oeso.RELATION_FROM_SPECIES.toString() + "> " + "?" + SPECIES + " .");
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_FROM_VARIETY.toString() + "> " + "?" + VARIETY + " }");
            query.appendToBody(" UNION ");
            query.appendToBody("{ ?" + VARIETY + " <" + Oeso.RELATION_FROM_SPECIES.toString() + "> " + "?" + SPECIES + " .");
            query.appendToBody("?" + ACCESSION + " <" + Oeso.RELATION_FROM_VARIETY.toString() + "> " + "?" + VARIETY + " .");
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_FROM_ACCESSION.toString() + "> " + "?" + ACCESSION + " }");
            query.appendAndFilter("REGEX ( str(?" + SPECIES + "),\".*" + fromSpecies + ".*\",\"i\")");
        }
        
        //fromVariety filter
        if (fromVariety != null) {
            query.appendToBody("{ ?" + URI + " <" + Oeso.RELATION_FROM_VARIETY.toString() + "> " + "?" + VARIETY + " }");
            query.appendToBody(" UNION ");
            query.appendToBody("{ ?" + ACCESSION + " <" + Oeso.RELATION_FROM_VARIETY.toString() + "> " + "?" + VARIETY + " .");
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_FROM_ACCESSION.toString() + "> " + "?" + ACCESSION + " }");
            query.appendAndFilter("REGEX ( str(?" + VARIETY + "),\".*" + fromVariety + ".*\",\"i\")");
        }
        
        //fromAccession filter
        if (fromAccession != null) {
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_FROM_ACCESSION.toString() + "> " + "?" + ACCESSION + " }");
            query.appendAndFilter("REGEX ( str(?" + VARIETY + "),\".*" + fromAccession + ".*\",\"i\")");
        }
        
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
    private SPARQLQueryBuilder prepareCount(String uri, String label, String germplasmType, String language, String fromGenus, String fromSpecies, String fromVariety, String fromAccession) {
        SPARQLQueryBuilder query = this.prepareSearchQuery(null, null, uri, label, germplasmType, language, fromGenus, fromSpecies, fromVariety, fromAccession);
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
     * @param label
     * @param germplasmType
     * @return The number of accession
     * @inheritdoc
     */
    public Integer count(String uri, String label, String germplasmType, String language, String fromGenus, String fromSpecies, String fromVariety, String fromAccession) {
        SPARQLQueryBuilder prepareCount = prepareCount(uri, label, germplasmType, language, fromGenus, fromSpecies, fromVariety, fromAccession);
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
     * @param label
     * @param germplasmType
     * @return the list of the accession.
     */
    public ArrayList<Germplasm> find(int page, int pageSize, String uri, String label, String germplasmType, String language, String fromGenus, String fromSpecies, String fromVariety, String fromAccession) {
        SPARQLQueryBuilder query = prepareSearchQuery(page, pageSize, uri, label, germplasmType, language, fromGenus, fromSpecies, fromVariety, fromAccession);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Germplasm> germplasmList = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Germplasm germplasm = getGermplasmFromBindingSet(bindingSet);
                germplasm.setProperties(findGerplasmProperties(germplasm.getUri()));
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
    private Germplasm getGermplasmFromBindingSet(BindingSet bindingSet) {
        Germplasm germplasm = new Germplasm();
        
        if (bindingSet.getValue(URI) != null) {
            germplasm.setUri(bindingSet.getValue(URI).stringValue());
        }      
        if (bindingSet.getValue(LABEL) != null) {
            germplasm.setLabel(bindingSet.getValue(LABEL).stringValue());
        }
        if (bindingSet.getValue(RDF_TYPE) != null) {
            germplasm.setRdfType(bindingSet.getValue(RDF_TYPE).stringValue());
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
        germplasm.setUri(id);
        
        try(TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet row = result.next();
                
                if (row.getValue(LABEL) != null) {
                    germplasm.setLabel(row.getValue(LABEL).stringValue());
                }
                
                if (row.getValue(RDF_TYPE) != null) {
                    germplasm.setRdfType(row.getValue(RDF_TYPE).stringValue());
                }
           
            }
        }
        return germplasm;
    }

    private SPARQLQueryBuilder prepareSearchByUri(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        query.appendFrom("<" + Contexts.GERMPLASM.toString() + ">");
        
        query.appendSelect(" ?" + URI + "?" + RDF_TYPE + "?" + LABEL);
        query.appendToBody("?" + URI + " <" + Rdf.RELATION_TYPE.toString() + "> " + "?" + RDF_TYPE + " . ");
        query.appendToBody("?" + URI + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + LABEL + " . ");
        query.appendAndFilter("REGEX ( str(?" + URI + "),\".*" + uri + ".*\",\"i\")");
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        
        return query;
    }
    
    /**
     * Get the properties of a given germplasm uri.
     * @param uri
     * @return the list of properties
     */
    public ArrayList<Property> findGerplasmProperties(String uri) {
        SPARQLQueryBuilder queryProperties = prepareSearchGermplasmProperties(uri);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, queryProperties.toString());
        List<String> foundedProperties = new ArrayList<>();
        ArrayList<Property> properties = new ArrayList<>();
            
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();

                if (!foundedProperties.contains(bindingSet.getValue(PROPERTY).stringValue())) {
                    Property property = new Property();

                    property.setRelation(bindingSet.getValue(RELATION).stringValue());
                    property.setValue(bindingSet.getValue(PROPERTY).stringValue());
                    if (bindingSet.getValue(PROPERTY_TYPE) != null) {
                        property.setRdfType(bindingSet.getValue(PROPERTY_TYPE).stringValue());
                    }

                    properties.add(property);
                    foundedProperties.add(bindingSet.getValue(PROPERTY).stringValue());
                }
            }
        }
        return properties;
    }
    
    
    
    /**
     * Generates the query to get the list of properties for a given scientific object
     * @param uri
     * @return 
     * @example
     * SELECT   ?relation ?property ?propertyType 
     * WHERE {
     *      <http://www.opensilex.org/opensilex/2019/o19000115>  ?relation  ?property  . 
     *      OPTIONAL {?property <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?propertyType } 
     * }
     */
    public SPARQLQueryBuilder prepareSearchGermplasmProperties(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendSelect(" ?" + RELATION + " ?" + PROPERTY + " ?" + PROPERTY_TYPE);
        query.appendTriplet(uri, "?" + RELATION, "?" + PROPERTY, null);        
        query.appendOptional("?" + PROPERTY + " <" + Rdf.RELATION_TYPE.toString() + "> ?" + PROPERTY_TYPE);
        
        LOGGER.debug(query.toString());
        
        return query;
    }

    public GermplasmDTO getGermplasmDTO(Germplasm germplasm, String language) {
        GermplasmDTO germplasmDTO = new GermplasmDTO();
        SPARQLQueryBuilder query = prepareSearchAllInformationGermplasm(germplasm, language);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                germplasmDTO = getGermplasmDTOFromBindingSet(bindingSet);
            }
        }
        germplasmDTO.setRdfType(germplasm.getRdfType());
        germplasmDTO.setUri(germplasm.getUri());
        germplasmDTO.setLabel(germplasm.getLabel());
        
        return germplasmDTO;
        
    }
    
    public SPARQLQueryBuilder prepareSearchAllInformationGermplasm(Germplasm germplasm, String language) {         
        String germplasmURI = germplasm.getUri();
        String germplasmType = germplasm.getRdfType();
       
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        query.appendSelect(" ?" + LOT_LABEL + " ?" +  ACCESSION_LABEL + " ?" + VARIETY_LABEL + " ?" + SPECIES_LABEL + " ?" + GENUS_LABEL );
        query.appendFrom("<" + Contexts.GERMPLASM.toString() + ">");
        
        String germplasmTypeName = GENUS;  
        String germplasmLabel = GENUS_LABEL;
        if (germplasmType.equals(Oeso.CONCEPT_PLANT_MATERIAL_LOT.toString())) {
            germplasmTypeName = LOT;
            germplasmLabel = LOT_LABEL;
        } else if (germplasmType.equals(Oeso.CONCEPT_ACCESSION.toString())) {
            germplasmTypeName = ACCESSION;
            germplasmLabel = ACCESSION_LABEL;
        } else if (germplasmType.equals(Oeso.CONCEPT_VARIETY.toString())) {
            germplasmTypeName = VARIETY;
            germplasmLabel = VARIETY_LABEL;
        } else if (germplasmType.equals(Oeso.CONCEPT_SPECIES.toString())) {
            germplasmTypeName = SPECIES;
            germplasmLabel = SPECIES_LABEL;
        } 
        
        query.appendToBody("?" + germplasmTypeName + " <" + Rdf.RELATION_TYPE.toString() + "> " + "<" + germplasm.getRdfType() + "> . ");
        query.appendToBody("?" + germplasmTypeName + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + germplasmLabel + " . ");
        if (germplasmType.equals(Oeso.CONCEPT_SPECIES.toString()) && language != null) {
            query.appendAndFilter("LANG(?" + SPECIES_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + SPECIES_LABEL + "), \"" + language + "\")");    
        }
        
        if (!germplasmType.equals(Oeso.CONCEPT_GENUS.toString())) {
            query.beginBodyOptional();
            query.appendToBody("?" + germplasmTypeName + " <" + Oeso.RELATION_FROM_GENUS.toString() + "> " + "?" + GENUS + " . ");
            query.appendToBody("?" + GENUS + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + GENUS_LABEL + " . ");
            query.endBodyOptional();            
               
            if (!germplasmType.equals(Oeso.CONCEPT_SPECIES.toString())) {
                query.beginBodyOptional();
                query.appendToBody("?" + germplasmTypeName + " <" + Oeso.RELATION_FROM_SPECIES.toString() + "> " + "?" + SPECIES + " . ");
                query.appendToBody("?" + SPECIES + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + SPECIES_LABEL + " . ");
                query.appendAndFilter("LANG(?" + SPECIES_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + SPECIES_LABEL + "), \"" + language + "\")");
                query.beginBodyOptional();
                query.appendToBody("?" + SPECIES + " <" + Oeso.RELATION_FROM_GENUS.toString() + "> " + "?" + GENUS + " . ");
                query.appendToBody("?" + GENUS + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + GENUS_LABEL + " . ");
                query.endBodyOptional();
                query.endBodyOptional();
            
                if (!germplasmType.equals(Oeso.CONCEPT_VARIETY.toString())) {
                    query.beginBodyOptional();
                    query.appendToBody("?" + germplasmTypeName + " <" + Oeso.RELATION_FROM_VARIETY.toString() + "> " + "?" + VARIETY + " . ");
                    query.appendToBody("?" + VARIETY + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + VARIETY_LABEL + " . ");
                    query.beginBodyOptional();
                    query.appendToBody("?" + VARIETY + " <" + Oeso.RELATION_FROM_SPECIES.toString() + "> " + "?" + SPECIES + " . ");
                    query.appendToBody("?" + SPECIES + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + SPECIES_LABEL + " . ");
                    query.beginBodyOptional();
                    query.appendToBody("?" + SPECIES + " <" + Oeso.RELATION_FROM_GENUS.toString() + "> " + "?" + GENUS + " . ");
                    query.appendToBody("?" + GENUS + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + GENUS_LABEL + " . ");
                    query.endBodyOptional();
                    query.endBodyOptional();
                    query.endBodyOptional();
                
                    if (!germplasmType.equals(Oeso.CONCEPT_ACCESSION.toString())) {
                        query.beginBodyOptional();
                        query.appendToBody("?" + germplasmTypeName + " <" + Oeso.RELATION_FROM_ACCESSION.toString() + "> " + "?" + ACCESSION + " . ");
                        query.appendToBody("?" + ACCESSION + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + ACCESSION_LABEL + " . ");        
                        query.beginBodyOptional();
                        query.appendToBody("?" + ACCESSION + " <" + Oeso.RELATION_FROM_VARIETY.toString() + "> " + "?" + VARIETY + " . ");
                        query.appendToBody("?" + VARIETY + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + VARIETY_LABEL + " . ");
                        query.beginBodyOptional();
                        query.appendToBody("?" + VARIETY + " <" + Oeso.RELATION_FROM_SPECIES.toString() + "> " + "?" + SPECIES + " . ");
                        query.appendToBody("?" + SPECIES + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + SPECIES_LABEL + " . ");
                        query.beginBodyOptional();
                        query.appendToBody("?" + SPECIES + " <" + Oeso.RELATION_FROM_GENUS.toString() + "> " + "?" + GENUS + " . ");
                        query.appendToBody("?" + GENUS + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + GENUS_LABEL + " . ");
                        query.endBodyOptional();
                        query.endBodyOptional();
                        query.endBodyOptional();
                        query.endBodyOptional();
                    }
                }
            
            } 
            
        }
        query.appendAndFilter("REGEX ( str(?" + germplasmTypeName + "),\".*" + germplasmURI + ".*\",\"i\")");
        
        LOGGER.debug(query.toString());
        
        return query;
    }    
    
    /**
     * Gets a germplasmDTO from a given binding set.
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
    private GermplasmDTO getGermplasmDTOFromBindingSet(BindingSet bindingSet) {
        GermplasmDTO germplasmDTO = new GermplasmDTO();
        
        if (bindingSet.getValue(URI) != null) {
            germplasmDTO.setUri(bindingSet.getValue(URI).stringValue());
        }      
        if (bindingSet.getValue(GENUS_LABEL) != null) {
            germplasmDTO.setGenus(bindingSet.getValue(GENUS_LABEL).stringValue());
        }
        if (bindingSet.getValue(SPECIES_LABEL) != null) {
            germplasmDTO.setSpecies(bindingSet.getValue(SPECIES_LABEL).stringValue());
        }
        if (bindingSet.getValue(VARIETY_LABEL) != null) {
            germplasmDTO.setVariety(bindingSet.getValue(VARIETY_LABEL).stringValue());
        }
        if (bindingSet.getValue(ACCESSION_LABEL) != null) {
            germplasmDTO.setAccession(bindingSet.getValue(ACCESSION_LABEL).stringValue());
        }
        if (bindingSet.getValue(LOT_LABEL) != null) {
            germplasmDTO.setPlantMaterialLot(bindingSet.getValue(LOT_LABEL).stringValue());
        }
        return germplasmDTO;
    }
    
    /**
     * Search all the germplasm corresponding to the search params given.
     * @param page
     * @param pageSize
     * @param uri
     * @param label
     * @param germplasmType
     * @return the list of the accession.
     */
    public ArrayList<Germplasm> findWithAllInformation(int page, int pageSize, String uri, String label, String germplasmType, String language) {
        SPARQLQueryBuilder query = prepareSearchQueryWithAllInformation();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Germplasm> germplasmList = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Germplasm germplasm = getGermplasmFromBindingSet(bindingSet);
                germplasm.setProperties(findGerplasmProperties(germplasm.getUri()));
                germplasmList.add(germplasm);
            }
        }
        return germplasmList;
    }
    
    private SPARQLQueryBuilder prepareSearchQueryWithAllInformation() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendSelect("?" + GERMPLASM + "?" + GERMPLASM_TYPE + 
                GERMPLASM_LABEL + GENUS + GENUS_LABEL + SPECIES + SPECIES_LABEL
                + VARIETY + VARIETY_LABEL + ACCESSION + ACCESSION_LABEL);
        
        query.appendFrom("<" +Contexts.GERMPLASM + ">");

        query.beginBodyOptional();

        query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_GERMPLASM.toString() + "> " + "?" + GERMPLASM + " . ");
        query.appendToBody("?" + GERMPLASM + " <" + Rdf.RELATION_TYPE.toString() + "> " + "?" + GERMPLASM_TYPE + " . ");
        query.appendToBody("?" + GERMPLASM + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + GERMPLASM_LABEL + " . ");

        //1. case when the germplasm is a species 
        query.beginBodyOptional();
        query.appendToBody("?" + GERMPLASM + " <" + Oeso.RELATION_FROM_GENUS.toString() + "> " + "?" + GENUS + " . ");
        query.appendToBody("?" + GENUS + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + GENUS_LABEL + " . ");
        query.endBodyOptional();            

        //2. case when the germplasm is a variety or an accession or a lot linked to the species and so you get the species and possibly the genus
        query.beginBodyOptional();
        query.appendToBody("?" + GERMPLASM + " <" + Oeso.RELATION_FROM_SPECIES.toString() + "> " + "?" + SPECIES + " . ");
        query.appendToBody("?" + SPECIES + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + SPECIES_LABEL + " . ");            
        query.beginBodyOptional();
        query.appendToBody("?" + SPECIES + " <" + Oeso.RELATION_FROM_GENUS.toString() + "> " + "?" + GENUS + " . ");
        query.appendToBody("?" + GENUS + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + GENUS_LABEL + " . ");
        query.endBodyOptional();
        query.endBodyOptional();

        //3. case when the germplasm is an accession or a lot linked to the variety (so you get the variety, the species and possibly the genus)
        query.beginBodyOptional();
        query.appendToBody("?" + GERMPLASM + " <" + Oeso.RELATION_FROM_VARIETY.toString() + "> " + "?" + VARIETY + " . ");
        query.appendToBody("?" + VARIETY + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + VARIETY_LABEL + " . "); 
        query.appendToBody("?" + VARIETY + " <" + Oeso.RELATION_FROM_SPECIES.toString() + "> " + "?" + SPECIES + " . ");
        query.appendToBody("?" + SPECIES + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + SPECIES_LABEL + " . ");            
        query.beginBodyOptional();
        query.appendToBody("?" + SPECIES + " <" + Oeso.RELATION_FROM_GENUS.toString() + "> " + "?" + GENUS + " . ");
        query.appendToBody("?" + GENUS + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + GENUS_LABEL + " . ");
        query.endBodyOptional();
        query.endBodyOptional();

        //4. case when the germplasm is a lot linked to the accession            
        query.appendToBody("?" + GERMPLASM + " <" + Oeso.RELATION_FROM_ACCESSION.toString() + "> " + "?" + ACCESSION + " . ");
        query.appendToBody("?" + ACCESSION + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + ACCESSION_LABEL + " . "); 

        //4.a. the accession is linked to a variety (so you get the variety and the species. and the genus eventually)
        query.beginBodyOptional();
        query.appendToBody("?" + ACCESSION + " <" + Oeso.RELATION_FROM_VARIETY.toString() + "> " + "?" + VARIETY + " . ");
        query.appendToBody("?" + VARIETY + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + VARIETY_LABEL + " . "); 
        query.appendToBody("?" + VARIETY + " <" + Oeso.RELATION_FROM_SPECIES.toString() + "> " + "?" + SPECIES + " . ");
        query.appendToBody("?" + SPECIES + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + SPECIES_LABEL + " . "); 
        query.beginBodyOptional();
        query.appendToBody("?" + SPECIES + " <" + Oeso.RELATION_FROM_GENUS.toString() + "> " + "?" + GENUS + " . ");
        query.appendToBody("?" + GENUS + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + GENUS_LABEL + " . ");
        query.endBodyOptional();
        query.endBodyOptional();

        //4.b. the accession is linked to a species (you only get the species and the genus eventually)
        query.beginBodyOptional();
        query.appendToBody("?" + ACCESSION + " <" + Oeso.RELATION_FROM_SPECIES.toString() + "> " + "?" + SPECIES + " . ");
        query.appendToBody("?" + SPECIES + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + SPECIES_LABEL + " . "); 
        query.beginBodyOptional();
        query.appendToBody("?" + SPECIES + " <" + Oeso.RELATION_FROM_GENUS.toString() + "> " + "?" + GENUS + " . ");
        query.appendToBody("?" + GENUS + " <" + Rdfs.RELATION_LABEL.toString() + "> " + "?" + GENUS_LABEL + " . ");
        query.endBodyOptional();
        query.endBodyOptional();

        query.endBodyOptional();
        
        LOGGER.debug(query.toString());
        
        return query;
    }
    
    public String getPropertyValueLabel(Germplasm germplasm) {
        ArrayList<Property> properties = germplasm.getProperties();
        String[] array = {Oeso.RELATION_FROM_GENUS.toString(), Oeso.RELATION_FROM_SPECIES.toString(), Oeso.RELATION_FROM_VARIETY.toString(), Oeso.RELATION_FROM_ACCESSION.toString()};
        List<String> label = new ArrayList();
        
        for (Property property:properties) {
            if (Arrays.asList(array).contains(property.getRelation())) {
                label = findLabelsForUri(property.getValue());
            }

        }
        return label.get(0);
            
    }
    
}
