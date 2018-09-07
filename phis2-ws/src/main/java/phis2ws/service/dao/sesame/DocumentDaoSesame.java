//******************************************************************************
//                                       DocumentsDaoSesame.java
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: Aug, 2016
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
// pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.Orders;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.mongo.DocumentDaoMongo;
import phis2ws.service.dao.phis.ExperimentDao;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.model.User;
import phis2ws.service.resources.dto.ConcernItemDTO;
import phis2ws.service.resources.dto.DocumentMetadataDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.ResourcesUtils;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Document;
import phis2ws.service.view.model.phis.Experiment;

//SILEX:warning
//After the update of the June 12, 2018 document's metadata are inserted inside 
//the triplestore and in mongodb, the document is updated (linked/unlinked)
//\SILEX:warning

//SILEX:conception
//If the object concerned by the document does not exist in the triplestore, 
//dont add the triplet (element rdf:type elementType). It allows more genericity
//but might need to be updated in the future
//\SILEX:conception

/**
 * A Dao specific to insert the metadata of a document inside the triplestore.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 * @update [Morgane Vidal] 12 October, 2017 : add status on documents : linked/unlinked
 */
public class DocumentDaoSesame extends DAOSesame<Document> {
    final static Logger LOGGER = LoggerFactory.getLogger(DocumentDaoSesame.class);
    public String uri;
    public static final String URI = "documentUri";
    
    public String documentType;
    public static final String DOCUMENT_TYPE = "documentType";
    
    public String creator;
    public static final String CREATOR = "creator";
    
    public String language;
    public static final String LANGUAGE = "language";
    
    public String title;
    public static final String TITLE = "title";
    
    public String creationDate;
    public static final String CREATION_DATE = "creationDate";
    
    public String format;
    public static final String FORMAT = "format";
    
    public String comment;
    public static final String COMMENT = "comment";
    public static final String COMMENTS = "comments";

    /**
     * Sort document by date
     * Allowable values : asc, desc
     */
    public String order;
    
    //List of the elements concerned by the document
    public List<String> concernedItemsUris = new ArrayList<>();
    public static final String CONCERN = "concern";
    public static final String CONCERN_TYPE = "typeConcern";
    
    //Document's status. Equals to linked if the document has been linked to at 
    //least one element (concernedItems). Unlinked if the document isnt linked 
    //to any element
    public String status;
    public static final String STATUS = "status";
            
    /**
     * Triplestore relations, graph, context, concept labels  
     * @see https://www.w3.org/TR/rdf-schema/ 
     * @see http://dublincore.org/documents/dces/
     */
    //SILEX:conception
    //Maybe change the URINamespaces class in static ? 
    //\SILEX:conception
    private final static URINamespaces NAMESPACES = new URINamespaces();
    
    final static String TRIPLESTORE_RELATION_TYPE = "rdf:type";
    final static String TRIPLESTORE_RELATION_CREATOR = "dc:creator";
    final static String TRIPLESTORE_RELATION_LANGUGAGE = "dc:language";
    final static String TRIPLESTORE_RELATION_TITLE = "dc:title";
    final static String TRIPLESTORE_RELATION_DATE = "dc:date";
    final static String TRIPLESTORE_RELATION_FORMAT = "dc:format";
    final static String TRIPLESTORE_RELATION_COMMENT = "rdfs:comment";
    final static String TRIPLESTORE_RELATION_STATUS = NAMESPACES.getRelationsProperty("rStatus");
    final static String TRIPLESTORE_RELATION_CONCERN = NAMESPACES.getRelationsProperty("rConcern");
    
    final static String TRIPLESTORE_GRAPH_DOCUMENT = NAMESPACES.getContextsProperty("documents");
    
    final static String TRIPLESTORE_PREFIX_DUBLIN_CORE = NAMESPACES.getContextsProperty("pxDublinCore");
    
    final static String TRIPLESTORE_CONCEPT_DOCUMENT = NAMESPACES.getObjectsProperty("cDocuments");
    

    public DocumentDaoSesame() {
        super(); // Repository
        resourceType = "documents";
    }
    
    /**
     * Check if document's metadata are valid 
     * (check rules, documents types, documents status)
     * @see phis2ws.service.resources.dto.DocumentMetadataDTO#rules() 
     * @param documentsMetadata 
     * @return The POSTResultsReturn of the check. Contains list of errors if
     * errors found.
     */
    public POSTResultsReturn check(List<DocumentMetadataDTO> documentsMetadata) throws RepositoryException {
        POSTResultsReturn documentsMetadataCheck = null;
        // status list which will be returned. It will contains some fails or
        // informations
        List<Status> checkStatus = new ArrayList<>(); 

        //Get ontology documents types to check
        ArrayList<String> documentsTypes = null;
        try {
            documentsTypes = getDocumentsTypes();
        } catch (RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        
        boolean dataOk = true; 
        for (DocumentMetadataDTO documentMetadata : documentsMetadata) {
            //1. Check document's type
            if (documentsTypes != null && !documentsTypes.contains(documentMetadata.getDocumentType())) {
                dataOk = false;
                checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "Wrong document type value. Authorized document type values : " + documentsTypes.toString()));
            }

            //3. Check status (equals to linked or unlinked)
            if (!(documentMetadata.getStatus().equals("linked") || documentMetadata.getStatus().equals("unlinked"))) {
                dataOk = false;
                checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                        "Wrong status value given : " + documentMetadata.getStatus() + ". Expected : \"linked\" or \"unlinked\"" ));
            }
        }
        documentsMetadataCheck = new POSTResultsReturn(dataOk, null, dataOk);
        documentsMetadataCheck.statusList = checkStatus;
        return documentsMetadataCheck;
    }
    
    /**
     * Save the document in mongodb
     * @param filePath the file path of the document to save in mongodb
     * @return true document saved in mongodb
     *         false an error occurred
     */
    private POSTResultsReturn saveFileInMongoDB(String filePath, String fileURI) {
        DocumentDaoMongo documentDaoMongo = new DocumentDaoMongo();
        return documentDaoMongo.insertFile(filePath, fileURI);
    }
    
    /**
     * Generate a unique document uri. 
     * @see phis2ws.service.utils.ResourcesUtils#getUniqueID() 
     * @return the generated document's uri
     */
    private String generateDocumentsURI() {
        String fileName = "";
        boolean nameExist = true;
        final String uniqueID = ResourcesUtils.getUniqueID();
        while (nameExist) {
            fileName = new StringBuilder("document").append(uniqueID).toString();
            try {
                nameExist = exist(TRIPLESTORE_GRAPH_DOCUMENT + "/" + fileName, null, null);
            } catch (MalformedQueryException | QueryEvaluationException ex) {
                LOGGER.error(ex.getMessage(), ex);
                break;
            }
        }
        return TRIPLESTORE_GRAPH_DOCUMENT + "/" + fileName;
    }

    /**
     * Insert document's metadata in the triplestore and the file in mongo
     * @param documentsMetadata
     * @return the insert result, with each error or information
     */
    public POSTResultsReturn insert(List<DocumentMetadataDTO> documentsMetadata) {
        List<Status> insertStatus = new ArrayList<>(); // returned status, Failed or Info
        List<String> createdResourcesURIs = new ArrayList<>();

        POSTResultsReturn results = null;

        boolean resultState = false; // To know if data ok and saved

        boolean documentsMetadataState = true; // true if all the metadata is valid
        boolean AnnotationInsert = true; // true if the insertion has been done

        final Iterator<DocumentMetadataDTO> itAnot = documentsMetadata.iterator();

        while (itAnot.hasNext() && AnnotationInsert) {
            DocumentMetadataDTO annotObject = itAnot.next();
            
            //1. Save document in mongodb
            final String documentName = generateDocumentsURI(); 
            
            POSTResultsReturn saveFileResult = saveFileInMongoDB(annotObject.getServerFilePath(), documentName);
            insertStatus.addAll(saveFileResult.statusList);
            
            //Document has been save
            if (saveFileResult.getResultState()) { 
                //2. Save document's metadata
                //SILEX:conception
                // Here, the triplet corresponding to the concerned element which
                // does not exist should be added
                //\SILEX:conception
                //Document's metadata are correct and can be savec in triplestore                  
                //3. Save metadata in triplestore
                SPARQLUpdateBuilder spqlInsert = new SPARQLUpdateBuilder();
                spqlInsert.appendPrefix("dc", TRIPLESTORE_PREFIX_DUBLIN_CORE);

                spqlInsert.appendGraphURI(TRIPLESTORE_GRAPH_DOCUMENT); //Documents named graph
                spqlInsert.appendTriplet(documentName, TRIPLESTORE_RELATION_TYPE, annotObject.getDocumentType(), null);
                spqlInsert.appendTriplet(documentName, TRIPLESTORE_RELATION_CREATOR, "\"" + annotObject.getCreator() + "\"", null);
                spqlInsert.appendTriplet(documentName, TRIPLESTORE_RELATION_LANGUGAGE, "\"" + annotObject.getLanguage() + "\"", null);
                spqlInsert.appendTriplet(documentName, TRIPLESTORE_RELATION_TITLE, "\"" + annotObject.getTitle() + "\"", null);
                spqlInsert.appendTriplet(documentName, TRIPLESTORE_RELATION_DATE, "\"" + annotObject.getCreationDate() + "\"", null);
                spqlInsert.appendTriplet(documentName, TRIPLESTORE_RELATION_FORMAT, "\"" + annotObject.getExtension() + "\"", null);
                spqlInsert.appendTriplet(documentName, TRIPLESTORE_RELATION_STATUS, "\"" + annotObject.getStatus() + "\"", null);

                if (annotObject.getComment() != null) {
                    spqlInsert.appendTriplet(documentName, TRIPLESTORE_RELATION_COMMENT, "\"" + annotObject.getComment() + "\"", null);
                }

                if (!(annotObject.getConcern() == null) && !annotObject.getConcern().isEmpty()) {
                    for (ConcernItemDTO concernedItem : annotObject.getConcern()) {
                        spqlInsert.appendTriplet(documentName, TRIPLESTORE_RELATION_CONCERN, concernedItem.getUri(), null);
                        spqlInsert.appendTriplet(concernedItem.getUri(),TRIPLESTORE_RELATION_TYPE , concernedItem.getTypeURI(), null);
                    }
                }

                try {
                    // transaction begining
                    this.getConnection().begin();
                    Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spqlInsert.toString());
                    LOGGER.trace(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                    prepareUpdate.execute();

                    createdResourcesURIs.add(documentName);
                } catch (MalformedQueryException e) {
                    LOGGER.error(e.getMessage(), e);
                    AnnotationInsert = false;
                    insertStatus.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.MALFORMED_CREATE_QUERY + " : " + e.getMessage()));
                }

                 // JSON bien formé et pas de problème avant l'insertion
                if (AnnotationInsert && documentsMetadataState) {
                    resultState = true;
                    try {
                        this.getConnection().commit();
                    } catch (RepositoryException ex) {
                        LOGGER.error("Error during commit Triplestore statements: ", ex);
                    }
                } else {
                    // retour en arrière sur la transaction
                    try {
                        this.getConnection().rollback();
                    } catch (RepositoryException ex) {
                        LOGGER.error("Error during rollback Triplestore statements : ", ex);
                    }
                }
            }
        }
            
        results = new POSTResultsReturn(resultState, AnnotationInsert, documentsMetadataState);
        results.statusList = insertStatus;
        if (resultState && !createdResourcesURIs.isEmpty()) {
            results.createdResources = createdResourcesURIs;
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesURIs.size() + " new resource(s) created."));
        }
        return results;
    }
    
    /**
     * Return the list of documents types Retourne les types de documents disponibles
     * @return List de concepts de document 
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException 
     */
    public ArrayList<String> getDocumentsTypes() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        ArrayList<String> documentsSchemasUri = new ArrayList<>();
        SPARQLQueryBuilder sparqlQ = new SPARQLQueryBuilder();
        sparqlQ.appendDistinct(true);
        sparqlQ.appendSelect("?" + DOCUMENT_TYPE);
        sparqlQ.appendTriplet("?" + DOCUMENT_TYPE, "rdfs:subClassOf*", TRIPLESTORE_CONCEPT_DOCUMENT, null);
        sparqlQ.appendFilter("?" + DOCUMENT_TYPE + " != <" + TRIPLESTORE_CONCEPT_DOCUMENT +">");
        LOGGER.debug(sparqlQ.toString());
        TupleQuery tupleQueryTo = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQ.toString());
        
        try (TupleQueryResult resultTo = tupleQueryTo.evaluate()) {
            while (resultTo.hasNext()) {
                BindingSet bindingSet = resultTo.next();
                if (bindingSet.getValue(DOCUMENT_TYPE) != null) {
                    documentsSchemasUri.add(bindingSet.getValue(DOCUMENT_TYPE).stringValue());
                }
            }
        }
        return documentsSchemasUri;
    }

    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendPrefix("dc", TRIPLESTORE_PREFIX_DUBLIN_CORE);
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendGraph(TRIPLESTORE_GRAPH_DOCUMENT);
        String select;
        if (uri != null) {
            select = "<" + uri + ">";
        } else {
            select = "?" + URI;
            sparqlQuery.appendSelect(select);
            sparqlQuery.appendGroupBy(select);
        }

        if (documentType != null) {
             sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_TYPE, documentType, null);
        } else {
            sparqlQuery.appendGroupBy("?" + DOCUMENT_TYPE);
            sparqlQuery.appendSelect("?" + DOCUMENT_TYPE);
            sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_TYPE, "?" + DOCUMENT_TYPE, null);
        }
        
        if (creator != null) {
            sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_CREATOR, "\"" + creator + "\"", null);
        } else {
            sparqlQuery.appendGroupBy("?" + CREATOR);
            sparqlQuery.appendSelect("?" + CREATOR);
            sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_CREATOR, "?" + CREATOR, null);
        }
        
        if (language != null) {
            sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_LANGUGAGE, "\"" + language + "\"", null);
        } else {
            sparqlQuery.appendGroupBy("?" + LANGUAGE);
            sparqlQuery.appendSelect("?" + LANGUAGE);
            sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_LANGUGAGE, "?" + LANGUAGE, null);
        }
        
        if (title != null) {
            sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_TITLE, "\"" + title + "\"", null);
        } else {
            sparqlQuery.appendGroupBy("?" + TITLE);
            sparqlQuery.appendSelect("?" + TITLE);
            sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_TITLE, "?" + TITLE, null);
        }
        
        if (creationDate != null) {
            sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_DATE, "\"" + creationDate + "\"", null);
        } else {
            sparqlQuery.appendGroupBy("?" + CREATION_DATE);
            sparqlQuery.appendSelect("?" + CREATION_DATE);
            sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_DATE, "?" + CREATION_DATE, null);
        }
        
        if (format != null) {
            sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_FORMAT, "\"" + format + "\"", null);
        } else {
            sparqlQuery.appendGroupBy("?" + FORMAT);
            sparqlQuery.appendSelect("?" + FORMAT);
            sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_FORMAT, "?" + FORMAT, null);
        }
        
        if (!concernedItemsUris.isEmpty() && concernedItemsUris.size() > 0) {
            for (String concernedItemUri : concernedItemsUris) {
                sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_CONCERN, concernedItemUri, null);
            }
        } 
        
        if (status != null) {
            sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_STATUS, "\"" + status + "\"", null);
        } else {
            sparqlQuery.appendGroupBy("?" + STATUS);
            sparqlQuery.appendSelect("?" + STATUS);
            sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_STATUS, "?" + STATUS, null);
        }
        
        //SILEX:info
        // Add group concat to prevent multiple triplestore requests
        // the query return a list with comma separated value in one column
        //\SILEX:info
        sparqlQuery.appendSelectConcat("?" + COMMENT, SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR, "?" + COMMENTS);
        sparqlQuery.appendTriplet(select, TRIPLESTORE_RELATION_COMMENT, "?" + COMMENT, null);
        if (comment != null) {
            sparqlQuery.appendFilter("regex(STR(?" + COMMENT +"), '" + comment + "', 'i')");
        }
        
        if (order != null) {
            sparqlQuery.appendOrderBy(order.toUpperCase() + "(?" + CREATION_DATE + ")");
        }else{
            // Use by default DESC if the order parameter is null
            sparqlQuery.appendOrderBy(Orders.DESC.toString().toUpperCase() + "(?" + CREATION_DATE + ")");
        }
        
        sparqlQuery.appendLimit(this.getPageSize());
        sparqlQuery.appendOffset(this.getPage()* this.getPageSize());
        
        LOGGER.debug("sparql select query : " + sparqlQuery.toString());
        
        return sparqlQuery;
    }
    
    /**
     * prepare the query to search the elements which are concerned by the document
     * @param uriDocument
     * @return the search query
     */
    private SPARQLQueryBuilder prepareSearchConcernQuery(String uriDocument) {
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendGraph(TRIPLESTORE_GRAPH_DOCUMENT);

        sparqlQuery.appendSelect(" ?" + CONCERN  + " ?" +CONCERN_TYPE );
        sparqlQuery.appendTriplet(uriDocument, TRIPLESTORE_RELATION_CONCERN, "?" + CONCERN, null);
        sparqlQuery.appendTriplet("?" + CONCERN, TRIPLESTORE_RELATION_TYPE, "?" + CONCERN_TYPE, null);

        LOGGER.debug("sparql select query : " + sparqlQuery.toString());

        return sparqlQuery;
    }

     /**
     * Count query generated by the searched parameters (class attributes)
     * (uri, documentType, creator, language, title, creationDate, format, comment, order). 
     * Must be done to find the total of instances
     * found in the triplestore using these search parameters because the query
     * is paginated (reduce the amount of data retrieved and the time to process
     * data before to send it to the client) 
     * @example
     * SELECT DISTINCT  (count(distinct ?documentUri) as ?count) WHERE {
     *   GRAPH <http://www.phenome-fppn.fr/phis2/documents> { 
     *       ?documentUri  rdf:type  ?documentType  . 
     *       ?documentUri  dc:creator  ?creator  . 
     *       ?documentUri  dc:language  ?language  . 
     *       ?documentUri  dc:title  ?title  . 
     *       ?documentUri  dc:date  ?creationDate  . 
     *       ?documentUri  dc:format  ?format  . 
     *       ?documentUri  <http://www.phenome-fppn.fr/vocabulary/2017#status>  ?status  . 
     *       ?documentUri  rdfs:comment  ?comment  . 
     *      }
     * }
     * @return query generated with the searched parameters
     */
    private SPARQLQueryBuilder prepareCount() {
        SPARQLQueryBuilder query = this.prepareSearchQuery();
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.clearOrderBy();
        query.appendSelect("(count(distinct ?" + URI + ") as ?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        return query;
    }
    
    /**
     * @return number of total annotation returned with the search fields
     * @inheritdoc
     */
    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        SPARQLQueryBuilder prepareCount = prepareCount();
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
     * Check if the given user has the right to see the document
     * @param u the user
     * @param document
     * @return true if the user can see the document
     *         false if not
     */
    private boolean canUserSeeDocument(User u, Document document) {
        UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();
        userDao.isAdmin(u);
        if (u.getAdmin().equals("t") || u.getAdmin().equals("true")) {
            return true;
        } else {
            ExperimentDao experimentDao = new ExperimentDao();
            for (ConcernItemDTO concernItem : document.getConcernedItems()) {
                if (concernItem.getTypeURI().equals(NAMESPACES.getContextsProperty("pVoc2017") + "#Experiment")) {
                    Experiment experiment = new Experiment(concernItem.getUri());
                    
                    if (experimentDao.canUserSeeExperiment(u, experiment)) {
                        return true;
                    }
                } 
            }
            return false;
        }
    }
    
    /**
     * Retreive the list of the searched documents.
     * @return  a list of documents
     */
    public ArrayList<Document> allPaginate() {
        SPARQLQueryBuilder sparqlQuery = prepareSearchQuery();
        TupleQuery tupleQuery = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
        ArrayList<Document> documents = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Document document = new Document();
                
                if (uri != null) {
                    document.setUri(uri);
                } else {
                    document.setUri(bindingSet.getValue(URI).stringValue());
                }
                
                if (documentType != null) {
                    document.setDocumentType(documentType);
                } else {
                    document.setDocumentType(bindingSet.getValue(DOCUMENT_TYPE).stringValue());
                }
                
                if (creator != null) {
                    document.setCreator(creator);
                } else {
                    document.setCreator(bindingSet.getValue(CREATOR).stringValue());
                }
                
                if (language != null) {
                    document.setLanguage(language);
                } else {
                    document.setLanguage(bindingSet.getValue(LANGUAGE).stringValue());
                }
                
                if (title != null) {
                    document.setTitle(title);
                } else {
                    document.setTitle(bindingSet.getValue(TITLE).stringValue());
                }
                
                if (creationDate != null) {
                    document.setCreationDate(creationDate);
                } else {
                    document.setCreationDate(bindingSet.getValue(CREATION_DATE).stringValue());
                }
                
                if (format != null) {
                    document.setFormat(format);
                } else {
                    document.setFormat(bindingSet.getValue(FORMAT).stringValue());
                }
                
                if (status != null) {
                    document.setStatus(status);
                } else {
                    document.setStatus(bindingSet.getValue(STATUS).stringValue());
                }
                
                if (bindingSet.getValue(COMMENTS) != null) {
                    //SILEX:info
                    // concat query return a list with comma separated value in one column
                    //\SILEX:info
                    ArrayList<String> comments = new ArrayList<>(Arrays.asList(bindingSet.getValue(COMMENTS).stringValue().split(SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR)));
                    if (comments != null && !comments.isEmpty()) {
                        //SILEX:info
                        // for now only one comment can be linked to document
                        //\SILEX:info
                        document.setComment(comments.get(0));
                    } 
                }
 
                //Check if document is linked to other elements
                SPARQLQueryBuilder sparqlQueryConcern = prepareSearchConcernQuery(document.getUri());
                TupleQuery tupleQueryConcern = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQueryConcern.toString());
                TupleQueryResult resultConcern = tupleQueryConcern.evaluate();
                while (resultConcern.hasNext()) {
                    BindingSet bindingSetConcern = resultConcern.next();
                    if (bindingSetConcern.getValue(CONCERN) != null) {
                        ConcernItemDTO concernedItem = new ConcernItemDTO();
                        concernedItem.setTypeURI(bindingSetConcern.getValue(CONCERN_TYPE).stringValue());
                        concernedItem.setUri(bindingSetConcern.getValue(CONCERN).stringValue());
                        document.addConcernedItem(concernedItem);
                    }
                }
                
                if (canUserSeeDocument(user, document)) {
                    documents.add(document);
                }
            }
        }
        return documents;
    }
    
    //SILEX:todo
    //Create a check method for the update and the insert
    //\SILEX:todo
    /**
     * check the metadata and update them if metadata valid
     * @see phis2ws.service.resources.dto.DocumentMetadataDTO#rules() 
     * @param documentsMetadata
     * @return the query result with the list of error or informations
     */
    private POSTResultsReturn checkAndUpdateDocumentsMetadataList(List<DocumentMetadataDTO> documentsMetadata) {
        List<Status> updateStatusList = new ArrayList<>(); // Failed or Info
        List<String> updatedResourcesURIList = new ArrayList<>(); // Failed or Info
        POSTResultsReturn results;
        
        boolean annotationUpdate = true; // true if the update has been done
        boolean docsMetadataState = true;
        boolean resultState = false; // To know if the metadata where valid and updated
        
        for (DocumentMetadataDTO documentMetadata : documentsMetadata) {
            //1. Delete actual metadata
            //1.1 Get informations which will be updated (to remove triplets)
            DocumentDaoSesame docDaoSesame = new DocumentDaoSesame();
            docDaoSesame.user = user;
            docDaoSesame.uri = documentMetadata.getUri();
            ArrayList<Document> documentsCorresponding = docDaoSesame.allPaginate();

            String deleteQuery = null;
            //1.2 Delete metatada associated to the URI
            if (documentsCorresponding.size() > 0) {
                //SILEX:conception
                //Such as the existing querybuilder for the insert, create a
                //delete query builder and use it
                deleteQuery = "PREFIX dc: <" + TRIPLESTORE_PREFIX_DUBLIN_CORE + "#> "
                                   + "DELETE WHERE { "
                                   + "<" + documentsCorresponding.get(0).getUri() + "> " + TRIPLESTORE_RELATION_CREATOR + " \"" + documentsCorresponding.get(0).getCreator() + "\" . "
                                   + "<" + documentsCorresponding.get(0).getUri() + "> " + TRIPLESTORE_RELATION_LANGUGAGE + " \"" + documentsCorresponding.get(0).getLanguage() + "\" . "
                                   + "<" + documentsCorresponding.get(0).getUri() + "> " + TRIPLESTORE_RELATION_TITLE + " \"" + documentsCorresponding.get(0).getTitle() + "\" . "
                                   + "<" + documentsCorresponding.get(0).getUri() + "> " + TRIPLESTORE_RELATION_DATE + " \"" + documentsCorresponding.get(0).getCreationDate() + "\" . "
                                   + "<" + documentsCorresponding.get(0).getUri() + "> " + TRIPLESTORE_RELATION_TYPE + " <" + documentsCorresponding.get(0).getDocumentType() +"> . "
                                   + "<" + documentsCorresponding.get(0).getUri() + "> <" + TRIPLESTORE_RELATION_STATUS + "> \"" + documentsCorresponding.get(0).getStatus() + "\" . ";

                if (documentsCorresponding.get(0).getComment() != null) {
                    deleteQuery += "<" + documentsCorresponding.get(0).getUri() + "> " + TRIPLESTORE_RELATION_COMMENT + " \"" + documentsCorresponding.get(0).getComment() + "\" . ";
                }

                for (ConcernItemDTO concernedItem : documentsCorresponding.get(0).getConcernedItems()) {
                    deleteQuery += "<" + documentsCorresponding.get(0).getUri() + "> <" + TRIPLESTORE_RELATION_CONCERN + "> <" + concernedItem.getUri() + "> . ";
                }
                deleteQuery += "}";
                //\SILEX:conception
            }

            //2. Insert updated metadata
            SPARQLUpdateBuilder spqlInsert = new SPARQLUpdateBuilder();
            spqlInsert.appendPrefix("dc", TRIPLESTORE_PREFIX_DUBLIN_CORE);
            spqlInsert.appendGraphURI(TRIPLESTORE_GRAPH_DOCUMENT); 
            spqlInsert.appendTriplet(documentMetadata.getUri(), TRIPLESTORE_RELATION_TYPE, documentMetadata.getDocumentType(), null);
            spqlInsert.appendTriplet(documentMetadata.getUri(), TRIPLESTORE_RELATION_CREATOR, "\"" + documentMetadata.getCreator() + "\"", null);
            spqlInsert.appendTriplet(documentMetadata.getUri(), TRIPLESTORE_RELATION_LANGUGAGE, "\"" + documentMetadata.getLanguage() + "\"", null);
            spqlInsert.appendTriplet(documentMetadata.getUri(), TRIPLESTORE_RELATION_TITLE, "\"" + documentMetadata.getTitle() + "\"", null);
            spqlInsert.appendTriplet(documentMetadata.getUri(), TRIPLESTORE_RELATION_DATE, "\"" + documentMetadata.getCreationDate() + "\"", null);
            spqlInsert.appendTriplet(documentMetadata.getUri(), TRIPLESTORE_RELATION_STATUS, "\"" + documentMetadata.getStatus() + "\"", null);

            if (documentMetadata.getComment() != null) {
                spqlInsert.appendTriplet(documentMetadata.getUri(), TRIPLESTORE_RELATION_COMMENT, "\"" + documentMetadata.getComment() + "\"", null);
            }

            if (documentMetadata.getConcern() != null && !documentMetadata.getConcern().isEmpty() && documentMetadata.getConcern().size() > 0) {
                for (ConcernItemDTO concernedItem : documentMetadata.getConcern()) {
                    spqlInsert.appendTriplet(documentMetadata.getUri(), TRIPLESTORE_RELATION_CONCERN, concernedItem.getUri(), null);
                    spqlInsert.appendTriplet(concernedItem.getUri(), TRIPLESTORE_RELATION_TYPE, concernedItem.getTypeURI(), null);
                }
            }

            try {
                // début de la transaction : vérification de la requête
                this.getConnection().begin();
                Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery);
                Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spqlInsert.toString());
                LOGGER.debug(getTraceabilityLogs() + " query : " + prepareDelete.toString());
                LOGGER.debug(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                prepareDelete.execute();
                prepareUpdate.execute();

                updatedResourcesURIList.add(documentMetadata.getUri());
            } catch (MalformedQueryException e) {
                LOGGER.error(e.getMessage(), e);
                annotationUpdate = false;
                updateStatusList.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.MALFORMED_UPDATE_QUERY + e.getMessage()));
            }   
                
            // Data ok, update
            if (annotationUpdate && docsMetadataState) {
                resultState = true;
                try {
                    this.getConnection().commit();
                } catch (RepositoryException ex) {
                    LOGGER.error("Error during commit Triplestore statements: ", ex);
                }
            } else {
                // retour en arrière sur la transaction
                try {
                    this.getConnection().rollback();
                } catch (RepositoryException ex) {
                    LOGGER.error("Error during rollback Triplestore statements : ", ex);
                }
            }    
        }
        results = new POSTResultsReturn(resultState, annotationUpdate, docsMetadataState);
        results.statusList = updateStatusList;
        if (resultState && !updatedResourcesURIList.isEmpty()) {
            results.createdResources = updatedResourcesURIList;
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, updatedResourcesURIList.size() + " new resource(s) created."));
        }

        return results;
    }
    
    /**
     * check new metadata and update if no error
     * @param objectsToUpdate
     * @return the update result, with errors or informations
     */
    public POSTResultsReturn checkAndUpdateList(List<DocumentMetadataDTO> objectsToUpdate) {
        POSTResultsReturn postResult;
        
        postResult = this.checkAndUpdateDocumentsMetadataList(objectsToUpdate);
        
        return postResult;
    }
}
