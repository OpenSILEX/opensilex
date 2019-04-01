//******************************************************************************
//                                       TripletsDAOSesame.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 7 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  7 mars 2018
// Subject: a specific DAO to insert triplets in the triplestore
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateRequest;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.configuration.OType;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.resources.dto.TripletDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Triplet;

/**
 * a specific DAO to insert triplets in the triplestore
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class TripletDAOSesame extends DAOSesame<Triplet> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(TripletDAOSesame.class);
    
    //pattern to generate uri for the subject of a triplet
    private final static String REQUEST_GENERATION_URI_STRING = "?";
    private final static String LITERAL = "literal";
    
    /**
     * check each triplet's values 
     * @param tripletsGroup
     * @return the check of the triplets. Contains the list of the errors founded
     */
    public POSTResultsReturn checkTripletsGroup(ArrayList<TripletDTO> tripletsGroup) {
        //returned status list
        List<Status> checkStatusList = new ArrayList<>();
        boolean dataOk = true;
        
        //check triplets
        for (TripletDTO tripletDTO : tripletsGroup) {
            UriDaoSesame uriDaoSesame = new UriDaoSesame();

            //1. check if triplet.s is exist
            if (!uriDaoSesame.existUri(tripletDTO.getS())) { //unknown uri
                dataOk = false;
                checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, StatusCodeMsg.UNKNOWN_URI + " " + tripletDTO.getS()));
            }
            //2. check if triplet.p is an existing relation
            if (!uriDaoSesame.existUri(tripletDTO.getP())
                    && !tripletDTO.getP().equals(Rdf.RELATION_TYPE.toString())
                    && !tripletDTO.getP().equals(Rdfs.RELATION_LABEL.toString())) {
                dataOk = false;
                checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, StatusCodeMsg.UNKNOWN_URI + " " + tripletDTO.getP()));
            }

            //3. check if triplet.o_type is equals to "literal" or "uri" 
            //   and check the value of triplet.o
            if (tripletDTO.getO_type().equals(OType.URI.toString())) { //if value is supposed to be an uri
                //if the uri does not exist in the triplestore, error
                if (!uriDaoSesame.existUri(tripletDTO.getO())) {
                    dataOk = false;
                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, StatusCodeMsg.UNKNOWN_URI + " " + tripletDTO.getO()));
                }
            } else if (tripletDTO.getO_type().equals(OType.LITERAL.toString())) {
                if (tripletDTO.getO_lang() != null) {
                    //if the triplet.o_lang isn't an ISO language
                    if (!Arrays.asList(Locale.getISOLanguages()).contains(tripletDTO.getO_lang())) {
                        dataOk = false;
                        checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, StatusCodeMsg.BAD_DATA_FORMAT + " " + tripletDTO.getO_lang()));
                    }
                }
            }
        }
        
        POSTResultsReturn tripletsCheck = new POSTResultsReturn(dataOk, null, dataOk);
        tripletsCheck.statusList = checkStatusList;
        return tripletsCheck;
    }
    
    /**
     * for each list of triplets, check if there are some errors and return them
     * @see TripletDAOSesame#checkTripletsGroup(java.util.ArrayList) 
     * @param triplets
     * @return 
     */
    public POSTResultsReturn check(ArrayList<ArrayList<TripletDTO>> triplets) {
        //returned status list
        List<Status> checkStatusList = new ArrayList<>();
        boolean dataOk = true;
        for (int i = 0; i < triplets.size(); i++) {
            POSTResultsReturn tripletsGroupCheck = checkTripletsGroup(triplets.get(i));
            if (!tripletsGroupCheck.getDataState()) {
                dataOk = false;
                checkStatusList.addAll(tripletsGroupCheck.getStatusList());
            }
        }
        
        POSTResultsReturn tripletsCheck = new POSTResultsReturn(dataOk, null, dataOk);
        tripletsCheck.statusList = checkStatusList;
        
        return tripletsCheck;
    }
    
    /**
     * generates the query of insertion of a triplets group
     * @param triplets
     * @param graphUri
     * @return the insertion query
     * e.g.
     * INSERT DATA {
     *      GRAPH <http://www.phenome-fppn.fr/diaphen/1520935678173> { 
     *          <http://www.phenome-fppn.fr/diaphen/2018/s18001>  rdf:type  <http://www.opensilex.org/vocabulary/oeso#Sensor> . 
     *          <http://www.phenome-fppn.fr/diaphen/2018/s18001>  <http://www.opensilex.org/vocabulary/oeso#hasSensor>  <http://www.phenome-fppn.fr/phenovia/2017/o1032482> . 
     *      }
     * }
     */
    public UpdateRequest prepareInsertQuery(ArrayList<TripletDTO> triplets, String graphUri) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(graphUri);
        
        triplets.forEach((triplet) -> {
            RDFNode tripletValue = null;
            if (triplet.getO_type().equals(LITERAL)) {
                if (triplet.getO_lang() != null) {
                    tripletValue = ResourceFactory.createLangLiteral(triplet.getO(), triplet.getO_lang());
                } else {
                    tripletValue = ResourceFactory.createStringLiteral(triplet.getO());
                }
            } else {
                tripletValue = ResourceFactory.createProperty(triplet.getO());
            }
            
            Node tripletUri = NodeFactory.createURI(triplet.getS());
            Property tripletRelation = ResourceFactory.createProperty(triplet.getP());
            
            spql.addInsert(graph, tripletUri, tripletRelation, tripletValue);
        });
        
        return spql.buildRequest();
    }
    
    /**
     * generates the uri of the triplet group if it is needed
     * (REQUEST_GENERATION_URI_STRING founded as triplet subject)
     * needs to find the rdf:type property
     * @param tripletsGroup
     * @return the uri generated
     */
    private String generateUriIfNeeded(ArrayList<TripletDTO> tripletsGroup) {
        String rdfType = null;

        //1. get the type (if exist)
        for (TripletDTO triplet : tripletsGroup) {
            //if there is a type, generate the uri
            if (triplet.getP().equals(Rdf.RELATION_TYPE.toString())
                    && triplet.getS().equals(REQUEST_GENERATION_URI_STRING)) {
                rdfType = triplet.getO();
            }
        }

        //2. generate uri if needed
        UriGenerator uriGenerator = new UriGenerator();
        String uri = null;

        for (TripletDTO triplet : tripletsGroup) {
            //if there is a type, generate the uri
            if (triplet.getS().equals(REQUEST_GENERATION_URI_STRING) && uri == null) {
                try {
                    uri = uriGenerator.generateNewInstanceUri(rdfType, null, null);
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(TripletDAOSesame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return uri;
    }
    
    /**
     * register the triplets in the triplestore. Add the triplets in the given
     * graphs
     * @param tripletsGroup
     */
    private ArrayList<String> registerTripletsInGivenGraphs(ArrayList<TripletDTO> tripletsGroup) {
        HashMap<String, ArrayList<TripletDTO>> tripletsByGraph = new HashMap<>();
        ArrayList<String> createdResourcesUris = new ArrayList<>();

        //1. order triplets by graphs
        for (TripletDTO triplet : tripletsGroup) {
            if (triplet.getG() != null) {
                ArrayList<TripletDTO> triplets = new ArrayList<>();

                if (tripletsByGraph.containsKey(triplet.getG())) {
                    triplets = tripletsByGraph.get(triplet.getG());
                }

                triplets.add(triplet);
                tripletsByGraph.put(triplet.getG(), triplets);
            }

            if (!createdResourcesUris.contains(triplet.getS())) {
                createdResourcesUris.add(triplet.getS());
            }
        }

        //2. save each group of triplets
        for (Map.Entry<String,ArrayList<TripletDTO>> triplets : tripletsByGraph.entrySet()) {
            UpdateRequest insertInGivenGraph = prepareInsertQuery(triplets.getValue(), triplets.getKey());
            LOGGER.debug(SPARQL_QUERY + insertInGivenGraph.toString());
            Update prepareInsertInGivenGraph = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, insertInGivenGraph.toString());
            prepareInsertInGivenGraph.execute();
        }

        return createdResourcesUris;
    }
    
    /**
     * insert a group of triplets in the triplestore
     * @param tripletsGroup
     * @param graphUri
     * @return the insertion result with the resources created or the errors
     */
    public POSTResultsReturn insertTripletsGroup(ArrayList<TripletDTO> tripletsGroup, String graphUri) {
        //SILEX:todo
        //add transaction
        //\SILEX:todo
        List<Status> insertStatus = new ArrayList<>();
        List<String> createdResourcesUris = new ArrayList<>();
        boolean tripletInserted = true;
        boolean resultState = false;

        final Iterator<TripletDTO> iteratorTriplets = tripletsGroup.iterator();

        String generatedUri = generateUriIfNeeded(tripletsGroup);

        while(iteratorTriplets.hasNext() && tripletInserted) {
            TripletDTO tripletDTO = iteratorTriplets.next();

            if (tripletDTO.getS().equals(REQUEST_GENERATION_URI_STRING)) {
                if (generatedUri != null) {
                    tripletDTO.setS(generatedUri);
                } else { //error (blank node unimplemented yet)
                    POSTResultsReturn result = new POSTResultsReturn(false, false, false);
                    insertStatus.add(new Status(StatusCodeMsg.ERR, StatusCodeMsg.ERR, "Cannot generate uri, unknown type or given type uri generator not implemetend yet."));
                    result.statusList = insertStatus;
                    return result;
                }
            }
        }
        try {
            //SILEX:test
            //All the triplestore connection has to been checked and updated
            //This is an unclean hot fix

//            String sesameServer = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, SESAME_SERVER);
//            String repositoryID = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, REPOSITORY_ID);
//            rep = new HTTPRepository(sesameServer, repositoryID); //Stockage triplestore Sesame
//            rep.initialize();
//            this.setConnection(rep.getConnection());

            this.getConnection().begin();

            //Register triplet in the triplestore, in the graph created at the request reception
            UpdateRequest insertQuery = prepareInsertQuery(tripletsGroup, graphUri);
            LOGGER.debug(SPARQL_QUERY + insertQuery.toString());
            Update prepareInsert = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, insertQuery.toString());
            prepareInsert.execute();

            //Register triplets in the triplestore, in the given graphs
            createdResourcesUris = registerTripletsInGivenGraphs(tripletsGroup);

            if (tripletInserted) {
                resultState = true;
                getConnection().commit();
            } else {
                getConnection().rollback();
            }

//            //SILEX:test
//            //For the pool connection problems
//            getConnection().close();
//            //\SILEX:test
        } catch (RepositoryException ex) {
                LOGGER.error(StatusCodeMsg.COMMIT_TRIPLESTORE_ERROR, ex);
        } catch (MalformedQueryException e) {
               LOGGER.error(e.getMessage(), e);
               tripletInserted = false;
               insertStatus.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.MALFORMED_CREATE_QUERY + " : " + e.getMessage()));
        }

        POSTResultsReturn result = new POSTResultsReturn(resultState, tripletInserted, true);
        result.statusList = insertStatus;
        result.setCreatedResources(createdResourcesUris);
        if (resultState && !createdResourcesUris.isEmpty()) {
            result.createdResources = createdResourcesUris;
            result.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesUris.size() + " " + StatusCodeMsg.RESOURCES_CREATED));
        }

        return result;
    }
    
    /**
     * for each group of triplets, insert them in the triplestore
     * @param graphUri
     * @see POSTResultsReturn
     * @see TripletDAOSesame#insertTripletsGroup(java.utils.ArrayList)
     * @param triplets
     * @return the insertion result. Contains the list of created resources
     */
    public POSTResultsReturn insert(ArrayList<ArrayList<TripletDTO>> triplets, String graphUri) {
        //SILEX:todo
        //add transaction
        //\SILEX:todo
        List<Status> insertStatus = new ArrayList<>();
        List<String> createdResourcesUris = new ArrayList<>();
        boolean tripletInserted = true;
        
        final Iterator<ArrayList<TripletDTO>> iteratorTripletsGroups = triplets.iterator();
        while (iteratorTripletsGroups.hasNext() && tripletInserted) {
            ArrayList<TripletDTO> tripletsGroup = iteratorTripletsGroups.next();
            //insert group in triplestore
            POSTResultsReturn insertionGroupResult = insertTripletsGroup(tripletsGroup, graphUri);
            if (insertionGroupResult.getInsertState()) { //insertion has been done
                createdResourcesUris.addAll(insertionGroupResult.getCreatedResources());
                insertStatus.addAll(insertionGroupResult.getStatusList());
            } else {
                tripletInserted = false;
            }
        }
        
        POSTResultsReturn insertResult = new POSTResultsReturn(tripletInserted, tripletInserted, true);
        insertResult.setStatusList(insertStatus);
        if (tripletInserted && !createdResourcesUris.isEmpty()) {
            insertResult.setCreatedResources(createdResourcesUris);
        }
        
        return insertResult;
    }
    
    /**
     * check if there are some errors in the triplets list when no error founded
     * insert them in the triplestore
     * @param triplets
     * @param graphUri
     * @see POSTResultsReturn
     * @see TripletDAOSesame#check(java.util.ArrayList) 
     * @see TripletDAOSesame#insert(java.util.ArrayList)
     * @return the list of errors founded or the list of created resources
     */
    public POSTResultsReturn checkAndInsert(ArrayList<ArrayList<TripletDTO>> triplets, String graphUri) {
        POSTResultsReturn checkResults = check(triplets);
        if (checkResults.getDataState()) { //valid data
            return insert(triplets, graphUri);
        } else { //errors founded
            return checkResults; 
        }
    }

    @Override
    public List<Triplet> create(List<Triplet> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Triplet> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Triplet> update(List<Triplet> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Triplet find(Triplet object) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Triplet findById(String id) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
