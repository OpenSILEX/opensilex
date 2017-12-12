//**********************************************************************************************
//                                       PhenotypeDaoMongo.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  September, 14 2017
// Subject: A specific Dao to retrieve data on phenotypes. 
//***********************************************************************************************
package phis2ws.service.dao.mongo;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.ws.rs.core.Response;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.dao.manager.DAOMongo;
import phis2ws.service.dao.phis.AgronomicalObjectDao;
import phis2ws.service.dao.sesame.AgronomicalObjectDaoSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.PhenotypeDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.AgronomicalObject;
import phis2ws.service.view.model.phis.Data;
import phis2ws.service.view.model.phis.Phenotype;

public class PhenotypeDaoMongo extends DAOMongo<Phenotype> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(PhenotypeDaoMongo.class);
    
    private final MongoCollection<Document> provenanceCollection = database.getCollection(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "provenance"));
    private final MongoCollection<Document> dataCollection = database.getCollection(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "data"));
    
    public String experiment;
    public String variable;
    public ArrayList<String> agronomicalObjects = new ArrayList<>();
    public String startDate;
    public String endDate;
    
    public PhenotypeDaoMongo() {
        super();
    }    
    
    @Override
    protected BasicDBObject prepareSearchQuery() {
        BasicDBObject query = new BasicDBObject();
        
        if (variable != null) {
            query.append("variable", variable);
        }
        
        if (startDate != null && endDate != null) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                Date start = df.parse(startDate);
                Date end = df.parse(endDate);
                
                query.append("date", BasicDBObjectBuilder.start("$gte", start).add("$lte", end).get());

            } catch (ParseException ex) {
                java.util.logging.Logger.getLogger(PhenotypeDaoMongo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if (agronomicalObjects != null && !agronomicalObjects.isEmpty()) {
            if (agronomicalObjects.size() > 1) {
                BasicDBList or = new BasicDBList();
                for (String agronomicalObject : agronomicalObjects) {
                    BasicDBObject clause = new BasicDBObject("agronomicalObject", agronomicalObject);
                    or.add(clause);
                }
                query.append("$or", or);
            } else {
                query.append("agronomicalObject", agronomicalObjects.get(0));
            }
        }
        
        return query;
    }

    /**
     * @action récupère la liste des objets agronomiques de l'expérimentation pour
     *         les ajouter à la liste des objets agronomiques recherchés.
     * @param experiment 
     */
    private void updateAgronomicalObjectsWithExperimentsAgronomicalObjects() {
        AgronomicalObjectDaoSesame agronomicalObjectDaoSesame = new AgronomicalObjectDaoSesame();
        agronomicalObjectDaoSesame.experiment = experiment;
        
        ArrayList<AgronomicalObject> agronomicalObjects = agronomicalObjectDaoSesame.allPaginate();
        
        for (AgronomicalObject agronomicalObject : agronomicalObjects) {
            this.agronomicalObjects.add(agronomicalObject.getUri());
        }
    }
    
    /**
     * 
     * @return liste de phénotypes, résultats de la recherche, vide si pas de résultats
     */
    @Override
    public ArrayList<Phenotype> allPaginate() {
        //Si on a une expérimentation, il faut récupérer la liste des objets agronomiques
        //pour la requête mongo
        if (experiment != null) {
            updateAgronomicalObjectsWithExperimentsAgronomicalObjects();
        }
        
        BasicDBObject query = prepareSearchQuery();
        
        LOGGER.trace(getTraceabilityLogs() + " query : " + query.toString());
        FindIterable<Document> phenotypesMongo = dataCollection.find(query);

        ArrayList<Phenotype> phenotypes = new ArrayList<>();
        Phenotype phenotype = new Phenotype();
        phenotype.setExperiment(experiment);
        phenotype.setVariableURI(variable);
        
        //SILEX:todo
        //récupérer les valeurs en fonction de la pagination...
        //il faut faire cette pagination sur les objets agronomiques..
        //en discuter avec Anne ?
        //pour l'instant, j'ai enlevé la pagination
        //\SILEX:todo
        
        try (MongoCursor<Document> phenotypesCursor = phenotypesMongo.iterator()) {
            while (phenotypesCursor.hasNext()) {
                Document phenotypeDocument = phenotypesCursor.next();
                
                Data data = new Data();
                data.setAgronomicalObject(phenotypeDocument.getString("agronomicalObject"));
                data.setDate(new SimpleDateFormat("yyyy-MM-dd").format(phenotypeDocument.getDate("date"))); // TODO: changer le format de la date pour le retour
                data.setValue(phenotypeDocument.getString("value"));
                data.setVariable(phenotypeDocument.getString("variable"));
                
                phenotype.addData(data);
            }
        }
        phenotypes.add(phenotype);
        
        return phenotypes;
    }
    
    private boolean isElementValid(PhenotypeDTO phenotypeDTO) {
        Map<String, Object> phenotypeOk = phenotypeDTO.isOk();
        return (boolean) phenotypeOk.get("state");
    }
    
    //SILEX:todo
    //Faire une fonction "check" commune entre l'insert et le update
    //\SILEX:todo
    private POSTResultsReturn checkAndInsertPhenotypesList(ArrayList<PhenotypeDTO> phenotypesDTO) throws Exception {
        List<Status> insertStatusList = new ArrayList<>();
        POSTResultsReturn result = null;
        
        ArrayList<Phenotype> phenotypes = new ArrayList<>();
        boolean dataState = true;
        
        
        for (PhenotypeDTO phenotypeDTO : phenotypesDTO) {
            if (isElementValid(phenotypeDTO)) {                
                for (Data data : phenotypeDTO.getData()) {
                    AgronomicalObjectDao agronomicalObjectDao = new AgronomicalObjectDao();
                    if (!agronomicalObjectDao.existInDB(new AgronomicalObject(data.getAgronomicalObject()))) {
                        dataState = false;
                        insertStatusList.add(new Status("Data error", StatusCodeMsg.ERR, "Unknown Agronomical Object URI : " + data.getAgronomicalObject()));
                    }
                }
                
                Phenotype phenotype = phenotypeDTO.createObjectFromDTO();
                phenotypes.add(phenotype);
                
            } else {
                dataState = false;
                insertStatusList.add(new Status("Data error", StatusCodeMsg.ERR, "Fields are missing in JSON Data"));
            }
        }
        
        if (dataState) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            //SILEX:todo
            //FAIRE UN TRUC EQUIVALENT AUX TRANSACTIONS
            //On doit faire l'insertion dans mongoDb
            for (Phenotype phenotype : phenotypes) {
                //1. Insertion de la provenance
                Document provenance = new Document();
                Date creationDate = df.parse(phenotype.getProvenance().getCreationDate());
                
                provenance.append("creationDate", creationDate);
                provenance.append("wasGeneratedBy", phenotype.getProvenance().getWasGeneratedBy().getWasGeneratedBy());
                provenance.append("wasGeneratedByDescription", phenotype.getProvenance().getWasGeneratedBy().getWasGeneratedByDescription());
                provenance.append("documents", phenotype.getProvenance().getDocumentsUris());
                provenance.append("uri", phenotype.getProvenance().getUri());
                
                LOGGER.trace("MongoDB insert : " + provenance.toJson());
                
                provenanceCollection.insertOne(provenance);
                
                ArrayList<Document> dataToInsert = new ArrayList<>();
                //2. Insertion des data
                for (Data data : phenotype.getData()) {
                    Document d = new Document();
                    Date date = df.parse(data.getDate());
                    
                    d.append("date", date);
                    d.append("variable", phenotype.getVariableURI());
                    d.append("value", data.getValue());
                    d.append("agronomicalObject", data.getAgronomicalObject());
                    //SILEX:todo
                    //Regarder DBRef (https://docs.mongodb.com/manual/reference/database-references/#dbref-explanation)
                    d.append("provenance", provenance.get("_id"));
                    //\SILEX:todo
                    
                    LOGGER.trace("MongoDB insert : " + d.toJson());
                   
                    dataToInsert.add(d);
                }
                
                dataCollection.insertMany(dataToInsert);
            }
            
            insertStatusList.add(new Status("Resource created", StatusCodeMsg.INFO, "phenotypes inserted"));;
            result = new POSTResultsReturn(dataState);
            result.setHttpStatus(Response.Status.CREATED);
            result.statusList = insertStatusList;
            //\SILEX:todo
        } else {
            result = new POSTResultsReturn(dataState);
            result.setHttpStatus(Response.Status.BAD_REQUEST);
            result.statusList = insertStatusList;
        }
        
        return result;
    }
    
    /**
     * @action enregistre les données dans MongoDB
     * @param phenotypesDTO liste de phénotypes à enregistrer en BD
     * @return le résultat de l'insertion
     */
    public POSTResultsReturn checkAndInsert(ArrayList<PhenotypeDTO> phenotypesDTO) {
        POSTResultsReturn postResult;
        
        try {
            postResult = this.checkAndInsertPhenotypesList(phenotypesDTO);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            postResult = new POSTResultsReturn(false, Response.Status.INTERNAL_SERVER_ERROR, ex.toString());
        }
        
        return postResult;
    }

//TODO
    
}
