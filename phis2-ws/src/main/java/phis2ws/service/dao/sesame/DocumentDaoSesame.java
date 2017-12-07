//**********************************************************************************************
//                                       DocumentsDaoSesame.java 
//
// Author(s): Arnaud CHARLEROY, Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 12 2017 (Ajout status sur les documents (linked/unlinked)
// Subject: A Dao specific to documents insert into triplestore 
//***********************************************************************************************

// /!\ Suite à la maj de la semaine du 12 juin 2017, 
// des insertions sont faites dans le triplestore (metadonnées) ET dans mongodb (document - Utilisation de DocumentDaoMongo)
// il faudra renommer la classe... 

//SILEX:conception
// Si l'objet concerné par le document n'existe pas dans le triplestore, on n'ajoute pas de triplet 
// avec son type (element rdf:type elementType). C'est plus souple mais cela sera sûrement à modifier par la suite
//\SILEX:conception
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    public List<String> concernedItemsUris = new ArrayList<>();
    public String status;

    public DocumentDaoSesame() {
        super(); // Repository
        resourceType = "documents"; // type de la ressource
    }
    
    /**
     * Verifie si les metadonnes du document sont correctes
     * @param docsMetadata
     * @return 
     */
    public POSTResultsReturn check(List<DocumentMetadataDTO> docsMetadata) throws RepositoryException {
        // Résultats attendus
        POSTResultsReturn docsMetadataCheck = null;
        // list des statuts retournés
        List<Status> insertStatusList = new ArrayList<>(); // Echec ou Info

        boolean dataOk = true; 
        for (DocumentMetadataDTO documentMetadata : docsMetadata) {
            // Vérification des docsMetadata
            if ((boolean) documentMetadata.isOk().get("state")) { // Données attendues reçues
                //1. Récupération des types de documents de l'ontologie
                ArrayList<String> documentsSchemasUri = null;
                try {
                    documentsSchemasUri = getDocumentsTypes();
                } catch (RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }

                //2. Vérification du type du document
                if (documentsSchemasUri != null && !documentsSchemasUri.contains(documentMetadata.getDocumentType())) {
                    dataOk = false;
                    insertStatusList.add(new Status("Wrong value", StatusCodeMsg.ERR, "Wrong document type value. Authorized document type values : " + documentsSchemasUri.toString()));
                }
                
                //3. Vérification du status (doit être égal à "linked" ou "unlinked")
                if (!(documentMetadata.getStatus().equals("linked") || documentMetadata.getStatus().equals("unlinked"))) {
                    dataOk = false;
                    insertStatusList.add(new Status("Wrong value", StatusCodeMsg.ERR, 
                            "Wrong status value given : " + documentMetadata.getStatus() + ". Expected : \"linked\" or \"unlinked\"" ));
                }
            } else {
                // Format des données non attendu par rapport au schéma demandé
                dataOk = false;
                documentMetadata.isOk().remove("state");
                insertStatusList.add(new Status("Bad data format", StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.MISSINGFIELDS).append(documentMetadata.isOk()).toString()));
            }
        }
        docsMetadataCheck = new POSTResultsReturn(dataOk, null, dataOk);
        docsMetadataCheck.statusList = insertStatusList;
        return docsMetadataCheck;
    }
    
    /**
     * 
     * @param filePath le chemin du fichier à enregistrer en mongodb
     * @return true si le document a été enregistré dans mongo
     *         false sinon
     */
    private POSTResultsReturn saveFileInMongoDB(String filePath, String fileURI) {
        DocumentDaoMongo documentDaoMongo = new DocumentDaoMongo();
        return documentDaoMongo.insertFile(filePath, fileURI);
    }

    public POSTResultsReturn insert(List<DocumentMetadataDTO> listAnnotations) {
        // nom du document
        String name = null;
        // list des statuts retournés
        List<Status> insertStatusList = new ArrayList<>(); // Failed or Info
        List<String> createdResourcesURIList = new ArrayList<>(); // Failed or Info

        POSTResultsReturn results = null;

        boolean resultState = false; // Pour savoir si les données étaient bonnes et on bien été insérées

        boolean docsMetadataState = true; // Si toutes les données étaient bonnes ok !
        boolean AnnotationInsert = true; // Si l'insertion a bien été réalisée

        final Iterator<DocumentMetadataDTO> itAnot = listAnnotations.iterator();

        while (itAnot.hasNext() && AnnotationInsert) {
            DocumentMetadataDTO annotObject = itAnot.next();
            //1. Enreg dans mongoDB du document
            
            //Uri du document (en évitant les doublons de noms)
            //SILEX:conception
            final URINamespaces uriNS = new URINamespaces();
            boolean nameExist = true;
            final String uniqueID = ResourcesUtils.getUniqueID();
            final String documents = uriNS.getContextsProperty("documents");
            while (nameExist) {
                name = new StringBuilder("document").append(uniqueID).toString();
                try {
                    nameExist = exist(documents + "/" + name, null, null);
                } catch (RepositoryException ex) {
                    LOGGER.error("Triplestore access error. ", ex);
                    insertStatusList.add(new Status("Triplestore access error", StatusCodeMsg.ERR, "Triplestore access error : " + ex.getMessage()));
                    break;
                } catch (MalformedQueryException | QueryEvaluationException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                    break;
                }
            }
            final String documentName = documents + "/" + name;
            //\SILEX:conception 
            
            POSTResultsReturn saveFileResult = saveFileInMongoDB(annotObject.getServerFilePath(), documentName);
            insertStatusList.addAll(saveFileResult.statusList);
            
            if (saveFileResult.getResultState()) {
                //2. Enreg du rdf associé au document
                Map<String, Object> anotOk = annotObject.isOk(); //Vérification de la structure
                
                docsMetadataState = (boolean) anotOk.get("state");
                //SILEX:conception
                // C'est ici qu'il faudrait faire l'ajout du triplet correspondant à l'entité concernée 
                // par le document s'il n'existe pas
                //\SILEX:conception
                
                if (docsMetadataState) {                    
                    //3. Insertion des métadonnées dans le triplestore
                    SPARQLUpdateBuilder spqlInsert = new SPARQLUpdateBuilder();
                    spqlInsert.appendPrefix("dc", uriNS.getContextsProperty("pxDublinCore"));
                    
                    spqlInsert.appendGraphURI(uriNS.getContextsProperty("documents")); //Le document est mis dans un graphe nommé
                    spqlInsert.appendTriplet(documentName, "rdf:type", annotObject.getDocumentType(), null);
                    spqlInsert.appendTriplet(documentName, "dc:creator", "\"" + annotObject.getCreator() + "\"", null);
                    spqlInsert.appendTriplet(documentName, "dc:language", "\"" + annotObject.getLanguage() + "\"", null);
                    spqlInsert.appendTriplet(documentName, "dc:title", "\"" + annotObject.getTitle() + "\"", null);
                    spqlInsert.appendTriplet(documentName, "dc:date", "\"" + annotObject.getCreationDate() + "\"", null);
                    spqlInsert.appendTriplet(documentName, "dc:format", "\"" + annotObject.getExtension() + "\"", null);
                    spqlInsert.appendTriplet(documentName, uriNS.getRelationsProperty("rStatus"), "\"" + annotObject.getStatus() + "\"", null);
                    
                    if (annotObject.getComment() != null) {
                        spqlInsert.appendTriplet(documentName, "rdfs:comment", "\"" + annotObject.getComment() + "\"", null);
                    }
                    
                    if (!(annotObject.getConcern() == null) && !annotObject.getConcern().isEmpty()) {
                        for (ConcernItemDTO concernedItem : annotObject.getConcern()) {
                            spqlInsert.appendTriplet(documentName, uriNS.getRelationsProperty("rConcern"), concernedItem.getUri(), null);
                            spqlInsert.appendTriplet(concernedItem.getUri(),"rdf:type", concernedItem.getTypeURI(), null);
                        }
                    }
                        
                    try {
                        // début de la transaction : vérification de la requête
                        this.getConnection().begin();
                        Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spqlInsert.toString());
                        LOGGER.trace(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                        prepareUpdate.execute();
                        
                        createdResourcesURIList.add(documentName);
                    } catch (MalformedQueryException e) {
                        LOGGER.error(e.getMessage(), e);
                        AnnotationInsert = false;
                        insertStatusList.add(new Status("Query error", StatusCodeMsg.ERR, "Malformed insertion query: " + e.getMessage()));
                    }
                } else {
                    // JSON malformé de quelque sorte que ce soit
                    docsMetadataState = false;
                    anotOk.remove("state");
                    insertStatusList.add(new Status("Missing field error", StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.MISSINGFIELDS).append(anotOk).toString()));
                }
                
                 // JSON bien formé et pas de problème avant l'insertion
                if (AnnotationInsert && docsMetadataState) {
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
            
        results = new POSTResultsReturn(resultState, AnnotationInsert, docsMetadataState);
        results.statusList = insertStatusList;
        if (resultState && !createdResourcesURIList.isEmpty()) {
            results.createdResources = createdResourcesURIList;
            results.statusList.add(new Status("Resources created", StatusCodeMsg.INFO, createdResourcesURIList.size() + " new resource(s) created."));
        }

        return results;
    }
    /**
     * Retourne les types de documents disponibles
     * @return List de concepts de document 
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException 
     */
    public ArrayList<String> getDocumentsTypes() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        URINamespaces uriNS = new URINamespaces();
        ArrayList<String> documentsSchemasUri = new ArrayList<>();
        // phis query
        SPARQLQueryBuilder sparqlQ = new SPARQLQueryBuilder();
        sparqlQ.appendDistinct(true);
        sparqlQ.appendSelect("?documentType");
        sparqlQ.appendTriplet("?documentType", "rdfs:subClassOf*", uriNS.getObjectsProperty("cDocuments"), null);
        sparqlQ.appendFilter("?documentType != <" + uriNS.getObjectsProperty("cDocuments") +">");
        LOGGER.trace(sparqlQ.toString());
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
       final URINamespaces uriNS = new URINamespaces();
       SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
       sparqlQuery.appendPrefix("dc", uriNS.getContextsProperty("pxDublinCore"));
       sparqlQuery.appendDistinct(true);
       sparqlQuery.appendGraph(uriNS.getContextsProperty("documents"));
       String select;
       if (uri != null) {
           select = "<" + uri + ">";
           sparqlQuery.appendSelect("");
       } else {
           select = "?documentUri";
           sparqlQuery.appendSelect(select);
       }

        if (documentType != null) {
             sparqlQuery.appendTriplet(select, "rdf:type", documentType, null);
        } else {
            sparqlQuery.appendSelect(" ?documentType");
            sparqlQuery.appendTriplet(select, "rdf:type", "?documentType", null);
        }
        
        if (creator != null) {
            sparqlQuery.appendTriplet(select, "dc:creator", "\"" + creator + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?creator");
            sparqlQuery.appendTriplet(select, "dc:creator", "?creator", null);
        }
        
        if (language != null) {
            sparqlQuery.appendTriplet(select, "dc:language", "\"" + language + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?language");
            sparqlQuery.appendTriplet(select, "dc:language", "?language", null);
        }
        
        if (title != null) {
            sparqlQuery.appendTriplet(select, "dc:title", "\"" + title + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?title");
            sparqlQuery.appendTriplet(select, "dc:title", "?title", null);
        }
        
        if (creationDate != null) {
            sparqlQuery.appendTriplet(select, "dc:date", "\"" + creationDate + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?date");
            sparqlQuery.appendTriplet(select, "dc:date", "?date", null);
        }
        
        if (format != null) {
            sparqlQuery.appendTriplet(select, "dc:format", "\"" + format + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?format");
            sparqlQuery.appendTriplet(select, "dc:format", "?format", null);
        }
        
        if (!concernedItemsUris.isEmpty() && concernedItemsUris.size() > 0) {
            for (String concernedItemUri : concernedItemsUris) {
                sparqlQuery.appendTriplet(select, uriNS.getRelationsProperty("rConcern"), concernedItemUri, null);
            }
        } 
        
        if (status != null) {
            sparqlQuery.appendTriplet(select, uriNS.getRelationsProperty("rStatus"), "\"" + status + "\"", null);
        } else {
            sparqlQuery.appendSelect(" ?status");
            sparqlQuery.appendTriplet(select, uriNS.getRelationsProperty("rStatus"), "?status", null);
        }
        
        //else {
//            sparqlQuery.appendSelect(" ?concern");
//            sparqlQuery.appendTriplet(select, uriNS.getRelationsProperty("rConcern"), "?concern", null);
//        }
        LOGGER.trace("sparql select query : " + sparqlQuery.toString());
        
        
       return sparqlQuery;
    }
    
    /**
     * 
     * @param uriDocument
     * @return la requête permettant de récupérer le commentaire du document
     */
    private SPARQLQueryBuilder prepareSearchCommentQuery(String uriDocument) {
        final URINamespaces uriNS = new URINamespaces();
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendGraph(uriNS.getContextsProperty("documents"));
        sparqlQuery.appendSelect("?comment");
        sparqlQuery.appendTriplet(uriDocument, "rdfs:comment", "?comment", null);
        
        LOGGER.trace("sparql select query : " + sparqlQuery.toString());
        return sparqlQuery;
    }
    
    /**
     * 
     * @param uriDocument
     * @return la requête permettant de lister les entités liées au document (dans le triplestore)
     */
    private SPARQLQueryBuilder prepareSearchConcernQuery(String uriDocument) {
        final URINamespaces uriNS = new URINamespaces();
        SPARQLQueryBuilder sparqlQuery = new SPARQLQueryBuilder();
        sparqlQuery.appendDistinct(true);
        sparqlQuery.appendGraph(uriNS.getContextsProperty("documents"));

        sparqlQuery.appendSelect(" ?concern ?typeConcern");
        sparqlQuery.appendTriplet(uriDocument, uriNS.getRelationsProperty("rConcern"), "?concern", null);
        sparqlQuery.appendTriplet("?concern", "rdf:type", "?typeConcern", null);

        LOGGER.trace("sparql select query : " + sparqlQuery.toString());

        return sparqlQuery;
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * 
     * @param u
     * @param document
     * @return true si l'utilisateur peut voir le document, false sinon
     */
    private boolean canUserSeeDocument(User u, Document document) {
        UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();
        userDao.isAdmin(u);
        if (u.getAdmin().equals("t") || u.getAdmin().equals("true")) {
            return true;
        } else {
            ExperimentDao experimentDao = new ExperimentDao();
            URINamespaces uriNs = new URINamespaces();
            for (ConcernItemDTO concernItem : document.getConcernedItems()) {
                if (concernItem.getTypeURI().equals(uriNs.getContextsProperty("pVoc2017") + "#Experiment")) {
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
                
                //Si l'utilisateur a bien les droits d'accès sur le document, on récupère les informations manquantes et on ajoute le document

                
                //Après avoir récupéré les métadonnées obligatoires associées au document,
                //on regarde s'il a un rdfs:comment 
                SPARQLQueryBuilder sparqlQueryComment = prepareSearchCommentQuery(document.getUri());
                TupleQuery tupleQueryComment = this.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQueryComment.toString());
                TupleQueryResult resultComment = tupleQueryComment.evaluate();
                while (resultComment.hasNext()) {
                    BindingSet bindingSetComment = resultComment.next();
                    if (bindingSetComment.getValue("comment") != null) {
                        document.setComment(bindingSetComment.getValue("comment").stringValue());
                    }
                }

                //Après avoir récupéré les métadonnées associées au document, on regarde dans
                //le triplestore si le document n'est pas rattaché à des éléments
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
    //Faire une méthode commune de check des données pour l'insert et le update
    //\SILEX:todo
    private POSTResultsReturn checkAndUpdateDocumentsMetadataList(List<DocumentMetadataDTO> documentsMetadata) {
        
         // list des statuts retournés
        List<Status> updateStatusList = new ArrayList<>(); // Failed or Info
        List<String> updatedResourcesURIList = new ArrayList<>(); // Failed or Info
        POSTResultsReturn results;
        
        boolean annotationUpdate = true; // Si l'insertion a bien été réalisée
        boolean docsMetadataState = true;
        boolean resultState = false; // Pour savoir si les données étaient bonnes et on bien été mises à jour
        
        for (DocumentMetadataDTO documentMetadata : documentsMetadata) {
            
            //Vérification que les données fournies soient les bonnes
            Map<String, Object> metadataOk = documentMetadata.isOk();
            boolean documentMetadataState = (boolean) metadataOk.get("state");
            
            if (!(documentMetadata.getStatus().equals("linked") || documentMetadata.getStatus().equals("unlinked"))) {
                documentMetadataState = false;
                updateStatusList.add(new Status("Wrong value", StatusCodeMsg.ERR, 
                            "Wrong status value given : " + documentMetadata.getStatus() + ". Expected : \"linked\" or \"unlinked\"" ));
            }
            
            if (documentMetadataState) {
                //1.Suppression des métadonnées actuellement présente
                //1.1 Récupération des infos qui seront modifiées (pour supprimer les triplets)
                DocumentDaoSesame docDaoSesame = new DocumentDaoSesame();
                docDaoSesame.user = user;
                docDaoSesame.uri = documentMetadata.getUri();
                ArrayList<Document> documentsCorresponding = docDaoSesame.allPaginate();
                URINamespaces uriNamespaces = new URINamespaces();
                
                String deleteQuery = null;
                //1.2 Suppression des métadonnées associées à l'URI
                if (documentsCorresponding.size() > 0) {
                    //SILEX:conception
                    //De la même façon qu'un querybuilder pour les insert existe, il faudra 
                    //développer un querybuilder pour le delete et l'utiliser ici
                    deleteQuery = "PREFIX dc: <" + uriNamespaces.getContextsProperty("pxDublinCore") + "#> "
                                       + "DELETE WHERE { "
                                       + "<" + documentsCorresponding.get(0).getUri() + "> dc:creator \"" + documentsCorresponding.get(0).getCreator() + "\" . "
                                       + "<" + documentsCorresponding.get(0).getUri() + "> dc:language \"" + documentsCorresponding.get(0).getLanguage() + "\" . "
                                       + "<" + documentsCorresponding.get(0).getUri() + "> dc:title \"" + documentsCorresponding.get(0).getTitle() + "\" . "
                                       + "<" + documentsCorresponding.get(0).getUri() + "> dc:date \"" + documentsCorresponding.get(0).getCreationDate() + "\" . "
                                       + "<" + documentsCorresponding.get(0).getUri() + "> rdf:type <" + documentsCorresponding.get(0).getDocumentType() +"> . "
                                       + "<" + documentsCorresponding.get(0).getUri() + "> <" + uriNamespaces.getRelationsProperty("rStatus") + "> \"" + documentsCorresponding.get(0).getStatus() + "\" . ";
                                      
                    if (documentsCorresponding.get(0).getComment() != null) {
                        deleteQuery += "<" + documentsCorresponding.get(0).getUri() + "> rdfs:comment \"" + documentsCorresponding.get(0).getComment() + "\" . ";
                    }
                    
                    for (ConcernItemDTO concernedItem : documentsCorresponding.get(0).getConcernedItems()) {
                        deleteQuery += "<" + documentsCorresponding.get(0).getUri() + "> <" + uriNamespaces.getRelationsProperty("rConcern") + "> <" + concernedItem.getUri() + "> . ";
                    }
                    deleteQuery += "}";
                    //\SILEX:conception
                }
                
                //2. Insertion des nouvelles métadonnées
                SPARQLUpdateBuilder spqlInsert = new SPARQLUpdateBuilder();
                spqlInsert.appendPrefix("dc", uriNamespaces.getContextsProperty("pxDublinCore"));
                spqlInsert.appendGraphURI(uriNamespaces.getContextsProperty("documents")); 
                spqlInsert.appendTriplet(documentMetadata.getUri(), "rdf:type", documentMetadata.getDocumentType(), null);
                spqlInsert.appendTriplet(documentMetadata.getUri(), "dc:creator", "\"" + documentMetadata.getCreator() + "\"", null);
                spqlInsert.appendTriplet(documentMetadata.getUri(), "dc:language", "\"" + documentMetadata.getLanguage() + "\"", null);
                spqlInsert.appendTriplet(documentMetadata.getUri(), "dc:title", "\"" + documentMetadata.getTitle() + "\"", null);
                spqlInsert.appendTriplet(documentMetadata.getUri(), "dc:date", "\"" + documentMetadata.getCreationDate() + "\"", null);
                spqlInsert.appendTriplet(documentMetadata.getUri(), uriNamespaces.getRelationsProperty("rStatus"), "\"" + documentMetadata.getStatus() + "\"", null);
                
                if (documentMetadata.getComment() != null) {
                    spqlInsert.appendTriplet(documentMetadata.getUri(), "rdfs:comment", "\"" + documentMetadata.getComment() + "\"", null);
                }
                
                if (documentMetadata.getConcern() != null && !documentMetadata.getConcern().isEmpty() && documentMetadata.getConcern().size() > 0) {
                    for (ConcernItemDTO concernedItem : documentMetadata.getConcern()) {
                        spqlInsert.appendTriplet(documentMetadata.getUri(), uriNamespaces.getRelationsProperty("rConcern"), concernedItem.getUri(), null);
                        spqlInsert.appendTriplet(concernedItem.getUri(), "rdf:type", concernedItem.getTypeURI(), null);
                    }
                }
                    
                try {
                    // début de la transaction : vérification de la requête
                    this.getConnection().begin();
                    Update prepareDelete = this.getConnection().prepareUpdate(deleteQuery);
                    Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, spqlInsert.toString());
                    LOGGER.trace(getTraceabilityLogs() + " query : " + prepareDelete.toString());
                    LOGGER.trace(getTraceabilityLogs() + " query : " + prepareUpdate.toString());
                    prepareDelete.execute();
                    prepareUpdate.execute();

                    updatedResourcesURIList.add(documentMetadata.getUri());
                } catch (MalformedQueryException e) {
                    LOGGER.error(e.getMessage(), e);
                    annotationUpdate = false;
                    updateStatusList.add(new Status("Query error", StatusCodeMsg.ERR, "Malformed update query: " + e.getMessage()));
                }   
                    
            } else {
                // JSON malformé de quelque sorte que ce soit
                docsMetadataState = false;
                metadataOk.remove("state");
                updateStatusList.add(new Status("Missing field error", StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.MISSINGFIELDS).append(metadataOk).toString()));
            }
                
            // JSON bien formé et pas de problème avant l'insertion
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
            results.statusList.add(new Status("Resources created", StatusCodeMsg.INFO, updatedResourcesURIList.size() + " new resource(s) created."));
        }

        return results;
    }
    
    /**
     * @action modifie les metadonnées des documents 
     * @param objectsToUpdate
     * @return 
     */
    public POSTResultsReturn checkAndUpdateList(List<DocumentMetadataDTO> objectsToUpdate) {
        POSTResultsReturn postResult;
        
        postResult = this.checkAndUpdateDocumentsMetadataList(objectsToUpdate);
        
        return postResult;
    }
}
