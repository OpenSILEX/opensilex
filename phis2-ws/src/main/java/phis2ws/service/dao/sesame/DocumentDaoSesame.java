//******************************************************************************
//                                       DocumentsDaoSesame.java
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: Aug, 2016
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
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
import phis2ws.service.configuration.DocumentStatus;
import phis2ws.service.configuration.SortingValues;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.mongo.DocumentDaoMongo;
import phis2ws.service.dao.phis.ExperimentDao;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.model.User;
import phis2ws.service.ontologies.Contexts;
import phis2ws.service.ontologies.DublinCore;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Vocabulary;
import phis2ws.service.resources.dto.ConcernItemDTO;
import phis2ws.service.resources.dto.DocumentMetadataDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.ResourcesUtils;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
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
    public static final String COMMENTS = "comments";

    /**
     * Sort document by date
     * Allowable values : asc, desc
     */
    public String sortByDate;
    
    //List of the elements concerned by the document
    public List<String> concernedItemsUris = new ArrayList<>();
    public static final String CONCERN = "concern";
    public static final String CONCERN_TYPE = "concernType";
    
    //Document's status. Equals to linked if the document has been linked to at 
    //least one element (concernedItems). Unlinked if the document isnt linked 
    //to any element
    public String status;
    public static final String STATUS = "status";

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
            if (!(documentMetadata.getStatus().equals(DocumentStatus.LINKED.toString()) || documentMetadata.getStatus().equals(DocumentStatus.UNLINKED.toString()))) {
                dataOk = false;
                checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                        "Wrong status value given : " + documentMetadata.getStatus() + ". Expected : \"" + DocumentStatus.LINKED.toString() + "\" or \"" + DocumentStatus.UNLINKED.toString() + "\"" ));
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
                nameExist = exist(Contexts.DOCUMENTS.toString() + "/" + fileName, null, null);
            } catch (MalformedQueryException | QueryEvaluationException ex) {
                LOGGER.error(ex.getMessage(), ex);
                break;
            }
        }
        return Contexts.DOCUMENTS.toString() + "/" + fileName;
    }

    /**
     * Prepare insert query for document metadata
     * 
     * @param documentMetadata
     * @return update request
     */
    private UpdateRequest prepareInsertQuery(DocumentMetadataDTO documentMetadata) {
        UpdateBuilder spql = new UpdateBuilder();
        Node graph = NodeFactory.createURI(Contexts.DOCUMENTS.toString());

        Node documentUri = NodeFactory.createURI(documentMetadata.getUri());

        Node documentType = NodeFactory.createURI(documentMetadata.getDocumentType());
        spql.addInsert(graph, documentUri, RDF.type, documentType);

        Property relationCreator = ResourceFactory.createProperty(DublinCore.RELATION_CREATOR.toString());
        spql.addInsert(graph, documentUri, relationCreator, documentMetadata.getCreator());

        Property relationLanguage = ResourceFactory.createProperty(DublinCore.RELATION_LANGUAGE.toString());
        spql.addInsert(graph, documentUri, relationLanguage, documentMetadata.getLanguage());

        Property relationTitle = ResourceFactory.createProperty(DublinCore.RELATION_TITLE.toString());
        spql.addInsert(graph, documentUri, relationTitle, documentMetadata.getTitle());

        Property relationDate = ResourceFactory.createProperty(DublinCore.RELATION_DATE.toString());
        spql.addInsert(graph, documentUri, relationDate, documentMetadata.getCreationDate());

        Property relationStatus = ResourceFactory.createProperty(Vocabulary.RELATION_STATUS.toString());
        spql.addInsert(graph, documentUri, relationStatus, documentMetadata.getStatus());
            
        if (documentMetadata.getExtension() != null) {
            Property relationFormat = ResourceFactory.createProperty(DublinCore.RELATION_FORMAT.toString());
            spql.addInsert(graph, documentUri, relationFormat, documentMetadata.getExtension());
        }
        
        if (documentMetadata.getComment() != null && !documentMetadata.getComment().equals("")) {
            spql.addInsert(graph, documentUri, RDFS.comment, documentMetadata.getComment());
        }

        if (!(documentMetadata.getConcern() == null) && !documentMetadata.getConcern().isEmpty()) {
            for (ConcernItemDTO concernedItem : documentMetadata.getConcern()) {
                Node concernedItemUri = NodeFactory.createURI(concernedItem.getUri());
                Node concernedItemType = NodeFactory.createURI(concernedItem.getTypeURI());
                Property relationConcern = ResourceFactory.createProperty(Vocabulary.RELATION_CONCERN.toString());

                spql.addInsert(graph, documentUri, relationConcern, concernedItemUri);
                spql.addInsert(graph, concernedItemUri, RDF.type, concernedItemType);
            }
        }

        return spql.buildRequest();
    } 
    
    /**
     * Prepare delete query for document metadata
     * 
     * @param documentMetadata
     * @return delete request
     */
    private UpdateRequest prepareDeleteQuery(Document document) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.DOCUMENTS.toString());
        Node documentUri = NodeFactory.createURI(document.getUri());
        
        Property relationCreator = ResourceFactory.createProperty(DublinCore.RELATION_CREATOR.toString());
        spql.addDelete(graph, documentUri, relationCreator, document.getCreator());

        Property relationLanguage = ResourceFactory.createProperty(DublinCore.RELATION_LANGUAGE.toString());
        spql.addDelete(graph, documentUri, relationLanguage, document.getLanguage());

        Property relationTitle = ResourceFactory.createProperty(DublinCore.RELATION_TITLE.toString());
        spql.addDelete(graph, documentUri, relationTitle, document.getTitle());

        Property relationDate = ResourceFactory.createProperty(DublinCore.RELATION_DATE.toString());
        spql.addDelete(graph, documentUri, relationDate, document.getCreationDate());

        Node documentType = NodeFactory.createURI(document.getDocumentType());
        spql.addDelete(graph, documentUri, RDF.type, documentType);
        
        Property relationStatus = ResourceFactory.createProperty(Vocabulary.RELATION_STATUS.toString());
        spql.addDelete(graph, documentUri, relationStatus, document.getStatus());
        
        if (document.getComment() != null) {
            spql.addDelete(graph, documentUri, RDFS.comment, document.getComment());
        }

        for (ConcernItemDTO concernedItem : document.getConcernedItems()) {
            Node concernedItemUri = NodeFactory.createURI(concernedItem.getUri());
            Property relationConcern = ResourceFactory.createProperty(Vocabulary.RELATION_CONCERN.toString());
            spql.addDelete(graph, documentUri, relationConcern, concernedItemUri);
        }
                
        return spql.buildRequest();
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
            
            annotObject.setUri(documentName);
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
                UpdateRequest query = prepareInsertQuery(annotObject);

                try {
                    // transaction begining
                    this.getConnection().begin();
                    Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
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
        sparqlQ.appendSelect("?documentType");
        sparqlQ.appendTriplet("?documentType", "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Vocabulary.CONCEPT_DOCUMENT.toString(), null);
        sparqlQ.appendFilter("?documentType != <" + Vocabulary.CONCEPT_DOCUMENT.toString() +">");

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
    /**
     * @example
     * SELECT DISTINCT  ?documentUri ?documentType ?creator ?title ?creationDate ?format ?status (GROUP_CONCAT(DISTINCT ?comment; SEPARATOR=",") AS ?comments) WHERE {
     * GRAPH <http://www.phenome-fppn.fr/phis2/documents> { ?documentUri  rdf:type  ?documentType  . 
     *  ?documentUri  dc:creator  ?creator  . 
     *  ?documentUri  dc:language  "en"  . 
     *  ?documentUri  dc:title  ?title  . 
     *  ?documentUri  dc:date  ?creationDate  . 
     *  ?documentUri  dc:format  ?format  . 
     *  ?documentUri  <http://www.phenome-fppn.fr/vocabulary/2017#status>  ?status  . 
     *  ?documentUri  rdfs:comment  ?comment  . 
     * FILTER ( (regex(STR(?creator), 'admin', 'i')) && (regex(STR(?title), 'liste', 'i')) ) 
     * }}
     * GROUP BY  ?documentUri ?documentType ?creator ?title ?creationDate ?format ?status 
     * ORDER BY DESC(?creationDate) 
     * LIMIT 20 
     * OFFSET 0 
     * @return the query
     */
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
       SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
       sparqlQuery.appendDistinct(true);
       sparqlQuery.appendGraph(Contexts.DOCUMENTS.toString());
       String select;
       if (uri != null) {
           select = "<" + uri + ">";
       } else {
            select = "?" + URI;
            sparqlQuery.appendSelect(select);
            sparqlQuery.appendGroupBy(select);
       }

        if (documentType != null) {
             sparqlQuery.appendTriplet(select, Rdf.RELATION_TYPE.toString(), documentType, null);
        } else {
            sparqlQuery.appendGroupBy("?" + DOCUMENT_TYPE);
            sparqlQuery.appendSelect("?" + DOCUMENT_TYPE);
            sparqlQuery.appendTriplet(select, Rdf.RELATION_TYPE.toString(), "?" + DOCUMENT_TYPE, null);
        }
        
        sparqlQuery.appendGroupBy("?" + CREATOR);
        sparqlQuery.appendSelect("?" + CREATOR);
        sparqlQuery.appendTriplet(select, DublinCore.RELATION_CREATOR.toString(), "?" + CREATOR, null);
        
        if (creator != null) {
            sparqlQuery.appendAndFilter("regex(STR(?" + CREATOR +"), '" + creator + "', 'i')");
        }
        
        if (language != null) {
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_LANGUAGE.toString(), "\"" + language + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?" + LANGUAGE);
            sparqlQuery.appendGroupBy(" ?" + LANGUAGE);
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_LANGUAGE.toString(), "?" + LANGUAGE, null);
        }
        
        sparqlQuery.appendGroupBy("?" + TITLE);
        sparqlQuery.appendSelect("?" + TITLE);
        sparqlQuery.appendTriplet(select, DublinCore.RELATION_TITLE.toString(), "?" + TITLE, null);
        
        if (title != null) {
            sparqlQuery.appendAndFilter("regex(STR(?" + TITLE +"), '" + title + "', 'i')");
        }
        
        if (creationDate != null) {
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_DATE.toString(), "\"" + creationDate + "\"", null);
        } else {
            sparqlQuery.appendGroupBy("?" + CREATION_DATE);
            sparqlQuery.appendSelect("?" + CREATION_DATE);
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_DATE.toString(), "?" + CREATION_DATE, null);
        }
        
        if (format != null) {
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_FORMAT.toString(), "\"" + format + "\"", null);
        } else {
            sparqlQuery.appendGroupBy("?" + FORMAT);
            sparqlQuery.appendSelect("?" + FORMAT);
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_FORMAT.toString(), "?" + FORMAT, null);
        }
        
        if (!concernedItemsUris.isEmpty() && concernedItemsUris.size() > 0) {
            for (String concernedItemUri : concernedItemsUris) {
                sparqlQuery.appendTriplet(select, Vocabulary.RELATION_CONCERN.toString(), concernedItemUri, null);
            }
        } 
        
        if (status != null) {
            sparqlQuery.appendTriplet(select, Vocabulary.RELATION_STATUS.toString(), "\"" + status + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?" + STATUS);
            sparqlQuery.appendGroupBy("?" + STATUS);
            sparqlQuery.appendTriplet(select, Vocabulary.RELATION_STATUS.toString(), "?" + STATUS, null);
        }
        
        //SILEX:info
        // Add group concat to prevent multiple triplestore requests
        // the query return a list with comma separated value in one column
        //\SILEX:info
        sparqlQuery.appendSelectConcat("?" + COMMENT, SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR, "?" + COMMENTS);
        sparqlQuery.beginBodyOptional();
        sparqlQuery.appendTriplet(select, Rdfs.RELATION_COMMENT.toString(), "?" + COMMENT, null);
        sparqlQuery.endBodyOptional();
        if (comment != null) {
            sparqlQuery.appendFilter("regex(STR(?" + COMMENT +"), '" + comment + "', 'i')");
        }
        
        if (sortByDate != null && sortByDate.equals("asc")) {
            sparqlQuery.appendOrderBy("ASC(?" + CREATION_DATE + ")");
        } else if (sortByDate != null && sortByDate.equals("desc")) {
            sparqlQuery.appendOrderBy("DESC(?" + CREATION_DATE + ")");
        }
        
        sparqlQuery.appendLimit(getPageSize());
        sparqlQuery.appendOffset(getPage() * getPageSize());
        LOGGER.debug(SPARQL_SELECT_QUERY + sparqlQuery.toString());
        
       return sparqlQuery;
    }
    
    /**
     * prepare the query to search the comments of a document
     * @param uriDocument
     * @return the document's comments search query
     */
    private SPARQLQueryBuilder prepareSearchCommentQuery(String uriDocument) {
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendGraph(Contexts.DOCUMENTS.toString());
        sparqlQuery.appendSelect("?comment");
        sparqlQuery.appendTriplet(uriDocument, Rdfs.RELATION_COMMENT.toString(), "?comment", null);
        
        if (sortByDate != null) {
            sparqlQuery.appendOrderBy(sortByDate.toUpperCase() + "(?" + CREATION_DATE + ")");
        } else {
            // Use by default DESC if the sortByDate parameter is null
            sparqlQuery.appendOrderBy(SortingValues.DESC.toString().toUpperCase() + "(?" + CREATION_DATE + ")");
        }
        
        sparqlQuery.appendLimit(this.getPageSize());
        sparqlQuery.appendOffset(this.getPage() * this.getPageSize());
        
        LOGGER.debug(SPARQL_SELECT_QUERY + sparqlQuery.toString());
        
        return sparqlQuery;
    }
    
    /**
     * Prepare the query to search the elements which are concerned by the document
     * @param uriDocument
     * @return the search query
     */
    private SPARQLQueryBuilder prepareSearchConcernQuery(String uriDocument) {
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendGraph(Contexts.DOCUMENTS.toString());

        sparqlQuery.appendSelect(" ?" + CONCERN + " ?" + CONCERN_TYPE);
        sparqlQuery.appendTriplet(uriDocument, Vocabulary.RELATION_CONCERN.toString(), "?" + CONCERN, null);
        sparqlQuery.appendTriplet("?concern", Rdf.RELATION_TYPE.toString(), "?" + CONCERN_TYPE, null);

        LOGGER.debug(SPARQL_SELECT_QUERY + sparqlQuery.toString());

        return sparqlQuery;
    }

     /**
     * Count query generated by the searched parameters (class attributes)
 (uri, documentType, creator, language, title, creationDate, format, comment, sortByDate). 
     * Must be done to find the total of instances
     * found in the triplestore using these search parameters because the query
     * is paginated (reduce the amount of data retrieved and the time to process
     * data before to send it to the client) 
     * @example
     * SELECT (COUNT(DISTINCT ?documentUri) as ?count) WHERE {
     * GRAPH <http://www.phenome-fppn.fr/phis2/documents> { 
     *  ?documentUri  rdf:type  ?documentType  . 
     *  ?documentUri  dc:creator  ?creator  . 
     *  ?documentUri  dc:language  "en"  . 
     *  ?documentUri  dc:title  ?title  . 
     *  ?documentUri  dc:date  ?creationDate  . 
     *  ?documentUri  dc:format  ?format  . 
     *  ?documentUri  <http://www.phenome-fppn.fr/vocabulary/2017#status>  ?status  . 
     *  ?documentUri  rdfs:comment  ?comment  . 
     * FILTER ( (regex(STR(?creator), 'admin', 'i')) && (regex(STR(?title), 'liste', 'i')) ) 
     * }}
     * @return query generated with the searched parameters
     */
    private SPARQLQueryBuilder prepareCount() {
        SPARQLQueryBuilder query = this.prepareSearchQuery();
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.clearOrderBy();
        query.appendSelect("( COUNT( DISTINCT ?" + URI + " ) AS ?" + COUNT_ELEMENT_QUERY + " )");
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
                if (concernItem.getTypeURI().equals(Vocabulary.CONCEPT_EXPERIMENT.toString())) {
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
                    //SILEX:info
                    // Group concat on comment may return empty line
                    // @example
                    //             DocumentUri	DocumentType	Creator	Title	CreationDate	Format	Status	Comments
                    // Document 1                                                                                  ""
                    //SILEX:info
                    if(bindingSet.getValue(URI) != null){
                        document.setUri(bindingSet.getValue(URI).stringValue());
                    }
                }
                // See SILEX:info above
                if(document.getUri() != null){
                    if (documentType != null) {
                        document.setDocumentType(documentType);
                    } else {
                        document.setDocumentType(bindingSet.getValue(DOCUMENT_TYPE).stringValue());
                    }

                    document.setCreator(bindingSet.getValue(CREATOR).stringValue());

                    if (language != null) {
                        document.setLanguage(language);
                    } else {
                        document.setLanguage(bindingSet.getValue(LANGUAGE).stringValue());
                    }

                    document.setTitle(bindingSet.getValue(TITLE).stringValue());

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

            UpdateRequest deleteQuery = null;
            //1.2 Delete metatada associated to the URI
            if (documentsCorresponding.size() > 0) {
                 deleteQuery = prepareDeleteQuery(documentsCorresponding.get(0));
            }

            //2. Insert updated metadata
            UpdateRequest query = prepareInsertQuery(documentMetadata);
            
            try {
                // début de la transaction : vérification de la requête
                this.getConnection().begin();
                if (deleteQuery != null) {
                    Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery.toString());
                    LOGGER.debug(getTraceabilityLogs() + " query : " + prepareDelete.toString());
                    prepareDelete.execute();
                }
                Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
                LOGGER.debug(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
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
