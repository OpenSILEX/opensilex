//**********************************************************************************************
//                                       DocumentsDaoSesame.java 
//
// Author(s): Arnaud Charleroy, Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 12 2017 (add status on documents : linked/unlinked)
// Subject: A Dao specific to insert the metadata of a document inside the triplestore
//***********************************************************************************************

//SILEX:warning
//After the update of the June 12, 2018 document's metadata are inserted inside 
//the triplestore and in mongodb, the document is updated (linked/unlinked)
//\SILEX:warning

//SILEX:conception
//If the object concerned by the document does not exist in the triplestore, 
//dont add the triplet (element rdf:type elementType). It allows more genericity
//but might need to be updated in the future
//\SILEX:conception
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
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
import phis2ws.service.configuration.DocumentStatus;
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
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Document;
import phis2ws.service.view.model.phis.Experiment;

public class DocumentDaoSesame extends DAOSesame<Document> {
    final static Logger LOGGER = LoggerFactory.getLogger(DocumentDaoSesame.class);
    public String uri;
    public String documentType;
    public String creator;
    public String language;
    public String title;
    public String creationDate;
    public String format;
    public String comment;
    //List of the elements concerned by the document
    public List<String> concernedItemsUris = new ArrayList<>();
    //Document's status. Equals to linked if the document has been linked to at 
    //least one element (concernedItems). Unlinked if the document isnt linked 
    //to any element
    public String status;

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
     * save the document in mongodb
     * @param filePath the file path of the document to save in mongodb
     * @return true document saved in mongodb
     *         false an error occurred
     */
    private POSTResultsReturn saveFileInMongoDB(String filePath, String fileURI) {
        DocumentDaoMongo documentDaoMongo = new DocumentDaoMongo();
        return documentDaoMongo.insertFile(filePath, fileURI);
    }
    
    /**
     * generate a unique document uri. 
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
     * insert document's metadata in the triplestore and the file in mongo
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

                spqlInsert.appendGraphURI(Contexts.DOCUMENTS.toString()); //Documents named graph
                spqlInsert.appendTriplet(documentName, Rdf.RELATION_TYPE.toString(), annotObject.getDocumentType(), null);
                spqlInsert.appendTriplet(documentName, DublinCore.RELATION_CREATOR.toString(), "\"" + annotObject.getCreator() + "\"", null);
                spqlInsert.appendTriplet(documentName, DublinCore.RELATION_LANGUAGE.toString(), "\"" + annotObject.getLanguage() + "\"", null);
                spqlInsert.appendTriplet(documentName, DublinCore.RELATION_TITLE.toString(), "\"" + annotObject.getTitle() + "\"", null);
                spqlInsert.appendTriplet(documentName, DublinCore.RELATION_DATE.toString(), "\"" + annotObject.getCreationDate() + "\"", null);
                spqlInsert.appendTriplet(documentName, DublinCore.RELATION_FORMAT.toString(), "\"" + annotObject.getExtension() + "\"", null);
                spqlInsert.appendTriplet(documentName, Vocabulary.RELATION_STATUS.toString(), "\"" + annotObject.getStatus() + "\"", null);

                if (annotObject.getComment() != null) {
                    spqlInsert.appendTriplet(documentName, Rdfs.RELATION_COMMENT.toString(), "\"" + annotObject.getComment() + "\"", null);
                }

                if (!(annotObject.getConcern() == null) && !annotObject.getConcern().isEmpty()) {
                    for (ConcernItemDTO concernedItem : annotObject.getConcern()) {
                        spqlInsert.appendTriplet(documentName, Vocabulary.RELATION_CONCERN.toString(), concernedItem.getUri(), null);
                        spqlInsert.appendTriplet(concernedItem.getUri(), Rdf.RELATION_TYPE.toString(), concernedItem.getTypeURI(), null);
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
        sparqlQ.appendSelect("?documentType");
        sparqlQ.appendTriplet("?documentType", Rdfs.RELATION_SUBPROPERTY_OF_MULTIPLE.toString(), Vocabulary.CONCEPT_DOCUMENT.toString(), null);
        sparqlQ.appendFilter("?documentType != <" + Vocabulary.CONCEPT_DOCUMENT.toString() +">");
        LOGGER.debug(sparqlQ.toString());
        TupleQuery tupleQueryTo = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQ.toString());
        
        try (TupleQueryResult resultTo = tupleQueryTo.evaluate()) {
            while (resultTo.hasNext()) {
                BindingSet bindingSet = resultTo.next();
                if (bindingSet.getValue("documentType") != null) {
                    documentsSchemasUri.add(bindingSet.getValue("documentType").stringValue());
                }
            }
        }
        return documentsSchemasUri;
    }

    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
       SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
       sparqlQuery.appendDistinct(true);
       sparqlQuery.appendGraph(Contexts.DOCUMENTS.toString());
       String select;
       if (uri != null) {
           select = "<" + uri + ">";
           sparqlQuery.appendSelect("");
       } else {
           select = "?documentUri";
           sparqlQuery.appendSelect(select);
       }

        if (documentType != null) {
             sparqlQuery.appendTriplet(select, Rdf.RELATION_TYPE.toString(), documentType, null);
        } else {
            sparqlQuery.appendSelect(" ?documentType");
            sparqlQuery.appendTriplet(select, Rdf.RELATION_TYPE.toString(), "?documentType", null);
        }
        
        if (creator != null) {
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_CREATOR.toString(), "\"" + creator + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?creator");
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_CREATOR.toString(), "?creator", null);
        }
        
        if (language != null) {
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_LANGUAGE.toString(), "\"" + language + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?language");
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_LANGUAGE.toString(), "?language", null);
        }
        
        if (title != null) {
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_TITLE.toString(), "\"" + title + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?title");
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_TITLE.toString(), "?title", null);
        }
        
        if (creationDate != null) {
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_DATE.toString(), "\"" + creationDate + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?date");
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_DATE.toString(), "?date", null);
        }
        
        if (format != null) {
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_FORMAT.toString(), "\"" + format + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?format");
            sparqlQuery.appendTriplet(select, DublinCore.RELATION_FORMAT.toString(), "?format", null);
        }
        
        if (!concernedItemsUris.isEmpty() && concernedItemsUris.size() > 0) {
            for (String concernedItemUri : concernedItemsUris) {
                sparqlQuery.appendTriplet(select, Vocabulary.RELATION_CONCERN.toString(), concernedItemUri, null);
            }
        } 
        
        if (status != null) {
            sparqlQuery.appendTriplet(select, Vocabulary.RELATION_STATUS.toString(), "\"" + status + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?status");
            sparqlQuery.appendTriplet(select, Vocabulary.RELATION_STATUS.toString(), "?status", null);
        }
        
       LOGGER.debug("sparql select query : " + sparqlQuery.toString());
        
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
        
        LOGGER.debug(SPARQL_SELECT_QUERY + sparqlQuery.toString());
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
        sparqlQuery.appendGraph(Contexts.DOCUMENTS.toString());

        sparqlQuery.appendSelect(" ?concern ?typeConcern");
        sparqlQuery.appendTriplet(uriDocument, Vocabulary.RELATION_CONCERN.toString(), "?concern", null);
        sparqlQuery.appendTriplet("?concern", Rdf.RELATION_TYPE.toString(), "?typeConcern", null);

        LOGGER.debug(SPARQL_SELECT_QUERY + sparqlQuery.toString());

        return sparqlQuery;
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * check if the given user has the right to see the document
     * @param u the user
     * @param document
     * @return true if the user can see the document
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
     * 
     * @return  liste de documents, résultat de la recherche, vide si pas de résultats.
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
                    document.setUri(bindingSet.getValue("documentUri").stringValue());
                }
                
                if (documentType != null) {
                    document.setDocumentType(documentType);
                } else {
                    document.setDocumentType(bindingSet.getValue("documentType").stringValue());
                }
                
                if (creator != null) {
                    document.setCreator(creator);
                } else {
                    document.setCreator(bindingSet.getValue("creator").stringValue());
                }
                
                if (language != null) {
                    document.setLanguage(language);
                } else {
                    document.setLanguage(bindingSet.getValue("language").stringValue());
                }
                
                if (title != null) {
                    document.setTitle(title);
                } else {
                    document.setTitle(bindingSet.getValue("title").stringValue());
                }
                
                if (creationDate != null) {
                    document.setCreationDate(creationDate);
                } else {
                    document.setCreationDate(bindingSet.getValue("date").stringValue());
                }
                
                if (format != null) {
                    document.setFormat(format);
                } else {
                    document.setFormat(bindingSet.getValue("format").stringValue());
                }
                
                if (status != null) {
                    document.setStatus(status);
                } else {
                    document.setStatus(bindingSet.getValue("status").stringValue());
                }

                //After having metadata, get comments
                SPARQLQueryBuilder sparqlQueryComment = prepareSearchCommentQuery(document.getUri());
                TupleQuery tupleQueryComment = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQueryComment.toString());
                TupleQueryResult resultComment = tupleQueryComment.evaluate();
                while (resultComment.hasNext()) {
                    BindingSet bindingSetComment = resultComment.next();
                    if (bindingSetComment.getValue("comment") != null) {
                        document.setComment(bindingSetComment.getValue("comment").stringValue());
                    }
                }

                //Check if document is linked to other elements
                SPARQLQueryBuilder sparqlQueryConcern = prepareSearchConcernQuery(document.getUri());
                TupleQuery tupleQueryConcern = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQueryConcern.toString());
                TupleQueryResult resultConcern = tupleQueryConcern.evaluate();
                while (resultConcern.hasNext()) {
                    BindingSet bindingSetConcern = resultConcern.next();
                    if (bindingSetConcern.getValue("concern") != null) {
                        ConcernItemDTO concernedItem = new ConcernItemDTO();
                        concernedItem.setTypeURI(bindingSetConcern.getValue("typeConcern").stringValue());
                        concernedItem.setUri(bindingSetConcern.getValue("concern").stringValue());
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
                deleteQuery =  "DELETE WHERE { "
                                   + "<" + documentsCorresponding.get(0).getUri() + "> " + DublinCore.RELATION_CREATOR.toString() + " \"" + documentsCorresponding.get(0).getCreator() + "\" . "
                                   + "<" + documentsCorresponding.get(0).getUri() + "> " + DublinCore.RELATION_LANGUAGE.toString() + " \"" + documentsCorresponding.get(0).getLanguage() + "\" . "
                                   + "<" + documentsCorresponding.get(0).getUri() + "> " + DublinCore.RELATION_TITLE.toString() + " \"" + documentsCorresponding.get(0).getTitle() + "\" . "
                                   + "<" + documentsCorresponding.get(0).getUri() + "> " + DublinCore.RELATION_DATE.toString() + " \"" + documentsCorresponding.get(0).getCreationDate() + "\" . "
                                   + "<" + documentsCorresponding.get(0).getUri() + "> <" + Rdf.RELATION_TYPE.toString() + "> <" + documentsCorresponding.get(0).getDocumentType() +"> . "
                                   + "<" + documentsCorresponding.get(0).getUri() + "> <" + Vocabulary.RELATION_STATUS.toString() + "> \"" + documentsCorresponding.get(0).getStatus() + "\" . ";

                if (documentsCorresponding.get(0).getComment() != null) {
                    deleteQuery += "<" + documentsCorresponding.get(0).getUri() + "> <" + Rdfs.RELATION_COMMENT.toString() + "> \"" + documentsCorresponding.get(0).getComment() + "\" . ";
                }

                for (ConcernItemDTO concernedItem : documentsCorresponding.get(0).getConcernedItems()) {
                    deleteQuery += "<" + documentsCorresponding.get(0).getUri() + "> <" + Vocabulary.RELATION_CONCERN.toString() + "> <" + concernedItem.getUri() + "> . ";
                }
                deleteQuery += "}";
                //\SILEX:conception
            }

            //2. Insert updated metadata
            SPARQLUpdateBuilder spqlInsert = new SPARQLUpdateBuilder();
            spqlInsert.appendGraphURI(Contexts.DOCUMENTS.toString()); 
            spqlInsert.appendTriplet(documentMetadata.getUri(), Rdf.RELATION_TYPE.toString(), documentMetadata.getDocumentType(), null);
            spqlInsert.appendTriplet(documentMetadata.getUri(), DublinCore.RELATION_CREATOR.toString(), "\"" + documentMetadata.getCreator() + "\"", null);
            spqlInsert.appendTriplet(documentMetadata.getUri(), DublinCore.RELATION_LANGUAGE.toString(), "\"" + documentMetadata.getLanguage() + "\"", null);
            spqlInsert.appendTriplet(documentMetadata.getUri(), DublinCore.RELATION_TITLE.toString(), "\"" + documentMetadata.getTitle() + "\"", null);
            spqlInsert.appendTriplet(documentMetadata.getUri(), DublinCore.RELATION_DATE.toString(), "\"" + documentMetadata.getCreationDate() + "\"", null);
            spqlInsert.appendTriplet(documentMetadata.getUri(), Vocabulary.RELATION_STATUS.toString(), "\"" + documentMetadata.getStatus() + "\"", null);

            if (documentMetadata.getComment() != null) {
                spqlInsert.appendTriplet(documentMetadata.getUri(), Rdfs.RELATION_COMMENT.toString(), "\"" + documentMetadata.getComment() + "\"", null);
            }

            if (documentMetadata.getConcern() != null && !documentMetadata.getConcern().isEmpty() && documentMetadata.getConcern().size() > 0) {
                for (ConcernItemDTO concernedItem : documentMetadata.getConcern()) {
                    spqlInsert.appendTriplet(documentMetadata.getUri(), Vocabulary.RELATION_CONCERN.toString(), concernedItem.getUri(), null);
                    spqlInsert.appendTriplet(concernedItem.getUri(), Rdf.RELATION_TYPE.toString(), concernedItem.getTypeURI(), null);
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
