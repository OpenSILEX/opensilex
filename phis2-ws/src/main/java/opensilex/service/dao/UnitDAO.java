//******************************************************************************
//                                UnitDAO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 18 Nov. 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.ontology.Skos;
import opensilex.service.ontology.Oeso;
import opensilex.service.resource.dto.UnitDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.view.brapi.Status;
import opensilex.service.model.OntologyReference;
import opensilex.service.model.Unit;

/**
 * Unit DAO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class UnitDAO extends Rdf4jDAO<Unit> {
    final static Logger LOGGER = LoggerFactory.getLogger(UnitDAO.class);
    
    public String uri;
    public String label;
    public String comment;
    public ArrayList<OntologyReference> ontologiesReferences = new ArrayList<>();

    protected SPARQLQueryBuilder prepareSearchQuery() {
        //SILEX:todo
        // Add search by ontology references
        //\SILEX:todo
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        query.appendGraph(Contexts.VARIABLES.toString());
        String unitUri;
        
        if (uri != null) {
            unitUri = "<" + uri + ">";
        } else {
            unitUri = "?uri";
            query.appendSelect("?uri");
        }
        query.appendTriplet(unitUri, Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_UNIT.toString(), null);
        
        if (label != null) {
            query.appendTriplet(unitUri, Rdfs.RELATION_LABEL.toString(),"\"" + label + "\"", null);
        } else {
            query.appendSelect(" ?label");
            query.appendTriplet(unitUri, Rdfs.RELATION_LABEL.toString(), "?label", null);
        }
        
        if (comment != null) {
            query.appendTriplet(unitUri, Rdfs.RELATION_COMMENT.toString(), "\"" + comment + "\"", null);
        } else {
            query.appendSelect(" ?" + COMMENT);
            query.beginBodyOptional();
            query.appendToBody(unitUri + " <" + Rdfs.RELATION_COMMENT.toString() + "> " + "?" + COMMENT + " . ");
            query.endBodyOptional();
        }
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }
    
    /**
     * Check if the objects are valid.
     * @param unitsDTO
     * @return 
     */
    public POSTResultsReturn check(List<UnitDTO> unitsDTO) {
        //Résultats attendus
        POSTResultsReturn traitsCheck;
        //Liste des status retournés
        List<Status> checkStatusList = new ArrayList<>();
        boolean dataOk = true;
        
        //Vérification des unités
        for (UnitDTO unitDTO : unitsDTO) {
            //Vérification des relations d'ontologies de référence
            for (OntologyReference ontologyReference : unitDTO.getOntologiesReferences()) {
                if (!ontologyReference.getProperty().equals(Skos.RELATION_EXACT_MATCH.toString())
                   && !ontologyReference.getProperty().equals(Skos.RELATION_CLOSE_MATCH.toString())
                   && !ontologyReference.getProperty().equals(Skos.RELATION_NARROWER.toString())
                   && !ontologyReference.getProperty().equals(Skos.RELATION_BROADER.toString())) {
                    dataOk = false;
                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                            "Bad property relation given. Must be one of the following : " 
                            + Skos.RELATION_EXACT_MATCH.toString()
                            + ", " + Skos.RELATION_CLOSE_MATCH.toString()
                            + ", " + Skos.RELATION_NARROWER.toString()
                            + ", " + Skos.RELATION_BROADER.toString()
                            +". Given : " + ontologyReference.getProperty()));
                }
            }
        }
        
        traitsCheck = new POSTResultsReturn(dataOk, null, dataOk);
        traitsCheck.statusList = checkStatusList;
        return traitsCheck;
    }
    
    /**
     * Prepares a query to get the higher id of the units.
     * @return 
     */
    private SPARQLQueryBuilder prepareGetLastId() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect("?uri");
        query.appendTriplet("?uri", Rdf.RELATION_TYPE.toString(), Oeso.CONCEPT_UNIT.toString(), null);
        query.appendOrderBy("DESC(?uri)");
        query.appendLimit(1);
        
        return query;
    }
    
    /**
     * Gets the higher id of the units.
     * @return the id
     */
    public int getLastId() {
        SPARQLQueryBuilder query = prepareGetLastId();

        //get last unit uri inserted
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        TupleQueryResult result = tupleQuery.evaluate();
        
        String uriUnit = null;
        
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            uriUnit = bindingSet.getValue("uri").stringValue();
        }
        
        if (uriUnit == null) {
            return 0;
        } else {
            String split = "units/u";
            String[] parts = uriUnit.split(split);
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            } else {
                return 0;
            }
        }
    }
    
    /**
     * Prepares an update query for a unit.
     * @param unitDTO
     * @return update request
     */
    private UpdateRequest prepareInsertQuery(UnitDTO unitDTO) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.VARIABLES.toString());
        
        Node unitConcept = NodeFactory.createURI(Oeso.CONCEPT_UNIT.toString());
        Resource unitUri = ResourceFactory.createResource(unitDTO.getUri());

        spql.addInsert(graph, unitUri, RDF.type, unitConcept);
        spql.addInsert(graph, unitUri, RDFS.label, unitDTO.getLabel());
        
        if (unitDTO.getComment() != null) {
            spql.addInsert(graph, unitUri, RDFS.comment, unitDTO.getComment());
        }
        
        unitDTO.getOntologiesReferences().forEach((ontologyReference) -> {
            Property ontologyProperty = ResourceFactory.createProperty(ontologyReference.getProperty());
            Node ontologyObject = NodeFactory.createURI(ontologyReference.getObject());
            spql.addInsert(graph, unitUri, ontologyProperty, ontologyObject);
            Literal seeAlso = ResourceFactory.createStringLiteral(ontologyReference.getSeeAlso());
            spql.addInsert(graph, ontologyObject, RDFS.seeAlso, seeAlso);
        });
        
        return spql.buildRequest();
    }
    
    /**
     * Create objects. 
     * The objects integrity must have been checked previously.
     * @param unitsDTO
     * @return 
     */
    public POSTResultsReturn insert(List<UnitDTO> unitsDTO) {
        List<Status> insertStatusList = new ArrayList<>();
        List<String> createdResourcesURI = new ArrayList<>();
        
        POSTResultsReturn results;
        boolean resultState = false;
        boolean annotationInsert = true;
        
        UriGenerator uriGenerator = new UriGenerator();
        final Iterator<UnitDTO> iteratorUnitDTO = unitsDTO.iterator();
        
        while (iteratorUnitDTO.hasNext() && annotationInsert) {
            UnitDTO unitDTO = iteratorUnitDTO.next();
            try {
                unitDTO.setUri(uriGenerator.generateNewInstanceUri(Oeso.CONCEPT_UNIT.toString(), null, null));
            } catch (Exception ex) { //In the unit case, no exception should be raised
                annotationInsert = false;
            }
            
            // Register
            UpdateRequest spqlInsert = prepareInsertQuery(unitDTO);
            
            try {
                //SILEX:todo
                // Connection to review. Dirty hotfix.
                this.getConnection().begin();
                Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spqlInsert.toString());
                LOGGER.trace(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                prepareUpdate.execute();
                //\SILEX:todo

                createdResourcesURI.add(unitDTO.getUri());

                if (annotationInsert) {
                    resultState = true;
                    getConnection().commit();
                } else {
                    getConnection().rollback();
                }
            } catch (RepositoryException ex) {
                    LOGGER.error("Error during commit or rolleback Triplestore statements: ", ex);
            } catch (MalformedQueryException e) {
                    LOGGER.error(e.getMessage(), e);
                    annotationInsert = false;
                    insertStatusList.add(new Status(
                            StatusCodeMsg.QUERY_ERROR, 
                            StatusCodeMsg.ERR, 
                            "Malformed insertion query: " + e.getMessage()));
            } 
        }
        
        results = new POSTResultsReturn(resultState, annotationInsert, true);
        results.statusList = insertStatusList;
        results.setCreatedResources(createdResourcesURI);
        if (resultState && !createdResourcesURI.isEmpty()) {
            results.createdResources = createdResourcesURI;
            results.statusList.add(new Status(
                    StatusCodeMsg.RESOURCES_CREATED, 
                    StatusCodeMsg.INFO, 
                    createdResourcesURI.size() + " new resource(s) created."));
        }
        
        return results;
    }
    
    /**
     * Checks the integrity of the objects and create them in the storage.
     * @param unitsDTO
     * @return the result
     */
    public POSTResultsReturn checkAndInsert(List<UnitDTO> unitsDTO) {
        POSTResultsReturn checkResult = check(unitsDTO);
        if (checkResult.getDataState()) {
            return insert(unitsDTO);
        } else {
            return checkResult;
        }
    }
    
    /**
     * @param uri
     * @return the ontology references links
     */
    private SPARQLQueryBuilder prepareSearchOntologiesReferencesQuery(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendDistinct(Boolean.TRUE);
        query.appendGraph(Contexts.VARIABLES.toString());
        
        if (ontologiesReferences.isEmpty()) {
            query.appendSelect(" ?property ?object ?seeAlso");
            query.appendTriplet(uri, "?property", "?object", null);
            query.appendOptional("{?object <" + Rdfs.RELATION_SEE_ALSO.toString() + "> ?seeAlso}");
            query.appendFilter("?property IN(<" + Skos.RELATION_CLOSE_MATCH.toString() + ">, <"
                                               + Skos.RELATION_EXACT_MATCH.toString() + ">, <"
                                               + Skos.RELATION_NARROWER.toString() + ">, <"
                                               + Skos.RELATION_BROADER.toString() + ">)");
        } else {
            for (OntologyReference ontologyReference : ontologiesReferences) {
                query.appendTriplet(uri, ontologyReference.getProperty(), ontologyReference.getObject(), null);
                query.appendTriplet(
                        ontologyReference.getObject(), 
                        Rdfs.RELATION_SEE_ALSO.toString(), 
                        ontologyReference.getSeeAlso(), 
                        null);
            }
        }
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }
    
    /**
     * @return the units found
     */
    public ArrayList<Unit> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Unit> units = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Unit unit = new Unit();
                
                if (uri != null) {
                    unit.setUri(uri);
                } else {
                    unit.setUri(bindingSet.getValue("uri").stringValue());
                }
                
                if (label != null) {
                    unit.setLabel(label);
                } else {
                    unit.setLabel(bindingSet.getValue("label").stringValue());
                }
                
                if (comment != null) {
                    unit.setComment(comment);
                } else if (bindingSet.getValue(COMMENT) != null) {
                    unit.setComment(bindingSet.getValue(COMMENT).stringValue());
                }
                
                // Get ontology references  
                SPARQLQueryBuilder queryOntologiesReferences = prepareSearchOntologiesReferencesQuery(unit.getUri());
                TupleQuery tupleQueryOntologiesReferences = this.getConnection()
                        .prepareTupleQuery(QueryLanguage.SPARQL, queryOntologiesReferences.toString());
                TupleQueryResult resultOntologiesReferences = tupleQueryOntologiesReferences.evaluate();
                while (resultOntologiesReferences.hasNext()) {
                    BindingSet bindingSetOntologiesReferences = resultOntologiesReferences.next();
                    if (bindingSetOntologiesReferences.getValue("object") != null
                            && bindingSetOntologiesReferences.getValue("property") != null) {
                        OntologyReference ontologyReference = new OntologyReference();
                        ontologyReference.setObject(bindingSetOntologiesReferences.getValue("object").toString());
                        ontologyReference.setProperty(bindingSetOntologiesReferences.getValue("property").toString());
                        if (bindingSetOntologiesReferences.getValue("seeAlso") != null) {
                            ontologyReference.setSeeAlso(bindingSetOntologiesReferences.getValue("seeAlso").toString());
                        }
                        
                        unit.addOntologyReference(ontologyReference);
                    }
                }
                units.add(unit);
            }
        }
        return units;
    }
    
    /**
     * Prepares delete request for a unit.
     * @param unit
     * @return delete request
     */
    private UpdateRequest prepareDeleteQuery(Unit unit){
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.VARIABLES.toString());
        Resource unitUri = ResourceFactory.createResource(unit.getUri());
        
        spql.addDelete(graph, unitUri, RDFS.label, unit.getLabel());
        if (unit.getComment() != null) {
            spql.addDelete(graph, unitUri, RDFS.comment, unit.getComment());
        }
        
        unit.getOntologiesReferences().forEach((ontologyReference) -> {
            Property ontologyProperty = ResourceFactory.createProperty(ontologyReference.getProperty());
            Node ontologyObject = NodeFactory.createURI(ontologyReference.getObject());
            spql.addDelete(graph, unitUri, ontologyProperty, ontologyObject);
            if (ontologyReference.getSeeAlso() != null) {
                Literal seeAlso = ResourceFactory.createStringLiteral(ontologyReference.getSeeAlso());
                spql.addDelete(graph, ontologyObject, RDFS.seeAlso, seeAlso);
            }
        });
                
        return spql.buildRequest();        
    }
    
    private POSTResultsReturn updateAndReturnPOSTResultsReturn(List<UnitDTO> unitsDTO) {
        List<Status> updateStatusList = new ArrayList<>();
        List<String> updatedResourcesURIList = new ArrayList<>();
        POSTResultsReturn results;
        
        boolean annotationUpdate = true;
        boolean resultState = false;
        
        for (UnitDTO unitDTO : unitsDTO) {
            //1. Delete existing data
            //1.1 Get information to modify (to delete the right triplets)
            uri = unitDTO.getUri();
            ArrayList<Unit> unitsCorresponding = allPaginate();
            if (unitsCorresponding.size() > 0) {
                UpdateRequest deleteQuery = prepareDeleteQuery(unitsCorresponding.get(0));

                //2. Insert the new data
                UpdateRequest queryInsert = prepareInsertQuery(unitDTO);
                 try {
                        // transaction start: check connection
                        this.getConnection().begin();
                        Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery.toString());
                        LOGGER.debug(getTraceabilityLogs() + " query : " + prepareDelete.toString());
                        prepareDelete.execute();
                        Update prepareUpdate = this.getConnection()
                                .prepareUpdate(QueryLanguage.SPARQL, queryInsert.toString());
                        LOGGER.debug(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                        prepareUpdate.execute();

                        updatedResourcesURIList.add(unitDTO.getUri());
                    } catch (MalformedQueryException e) {
                        LOGGER.error(e.getMessage(), e);
                        annotationUpdate = false;
                        updateStatusList.add(new Status(
                                StatusCodeMsg.QUERY_ERROR, 
                                StatusCodeMsg.ERR, 
                                "Malformed update query: " + e.getMessage()));
                    }   
            } else {
                annotationUpdate = false;
                updateStatusList.add(
                        new Status("Unknown instance", StatusCodeMsg.ERR, "Unknown unit " + unitDTO.getUri()));
            }
        }
        
        if (annotationUpdate) {
            resultState = true;
            try {
                this.getConnection().commit();
            } catch (RepositoryException ex) {
                LOGGER.error("Error during commit Triplestore statements: ", ex);
            }
        } else {
            // Rollback
            try {
                this.getConnection().rollback();
            } catch (RepositoryException ex) {
                LOGGER.error("Error during rollback Triplestore statements : ", ex);
            }
        }
        
        results = new POSTResultsReturn(resultState, annotationUpdate, true);
        results.statusList = updateStatusList;
        if (resultState && !updatedResourcesURIList.isEmpty()) {
            results.createdResources = updatedResourcesURIList;
            results.statusList.add(new Status(
                    StatusCodeMsg.RESOURCES_UPDATED, 
                    StatusCodeMsg.INFO, 
                    updatedResourcesURIList.size() + " resources updated"));
        }
        return results;
    }
    
    /**
     * Checks the objects integrity and updates them in the storage.
     * @param unitsDTO
     * @return the result of check and update
     */
    public POSTResultsReturn checkAndUpdate(List<UnitDTO> unitsDTO) {
        POSTResultsReturn checkResult = check(unitsDTO);
        if (checkResult.getDataState()) {
            return updateAndReturnPOSTResultsReturn(unitsDTO);
        } else { //Les données ne sont pas bonnes
            return checkResult;
        }
    }

    @Override
    public List<Unit> create(List<Unit> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Unit> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Unit> update(List<Unit> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Unit find(Unit object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Unit findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Unit> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

