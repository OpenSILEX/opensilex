//******************************************************************************
//                                       ${StudyDAO}
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************

package phis2ws.service.dao.phis;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.utils.sql.SQLQueryBuilder;
import phis2ws.service.model.User;
import phis2ws.service.utils.sql.JoinAttributes;
import phis2ws.service.view.model.phis.Experiment;
import phis2ws.service.view.model.phis.StudiesSearch;
import phis2ws.service.dao.phis.ExperimentDao;
import phis2ws.service.dao.sesame.AgronomicalObjectDAOSesame;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;


/**
 * Get Experiments filtered by StudiesSearchResourceService fields and add 
 * required attributes to get brapi studies
 * @author Alice Boizet
 */
public class StudyDAO {
    final static Logger LOGGER = LoggerFactory.getLogger(StudyDAO.class);
    
    public String studyType; 
    public String trialDbId;
    public String programDbId;
    public String commonCropName;
    public String locationDbId;
    public String seasonDbId;    
    public String studyDbId;
    public List<String> germplasmDbIds;
    public List<String> observationVariableDbIds;
    public Boolean active;
    public String sortBy;
    public String sortOrder;
    public int page;
    public int limit;
    public User user;

    public StudyDAO() {
    }
    
    public StudyDAO(String studyDbId) {
        this.studyDbId = studyDbId;
    }

    public int getPageSize() {
        return limit;
    }

    public void setPageSize(int pageSize) {
        this.limit = pageSize;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
 
    
    //correspondance entre champs en base et champs de recherche du web service
    public Map<String, String> relationFieldsObject() {
        Map<String, String> createSQLFields = new HashMap<>();
        createSQLFields.put("studyDbId","uri");
        createSQLFields.put("programDbId","project_uri");
        //createSQLFields.put("trialDbId", "");
        //createSQLFields.put("location", "field");
        createSQLFields.put("seasonDbId", "campaign");
        createSQLFields.put("commonCropName", "crop_species");
               
        return createSQLFields;
    }
    public ArrayList<String> getAgroObjectFromVar(List<String> observationVariableDbIds) {
        MongoClient mongoClient = new MongoClient(
            new MongoClientURI(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "url")));
        DB db = mongoClient.getDB("diaphen");
        DBCollection collection = db.getCollection("diaphen");
        ArrayList<String> agroObjectsList = new ArrayList();
        
        for (String var : observationVariableDbIds){
            BasicDBObject queryMongo = new BasicDBObject("variable", var);
            BasicDBObject projection = new BasicDBObject("agronomicalObject", 1);        
            try (DBCursor cursor = collection.find(queryMongo,projection)) {
                while(cursor.hasNext()) {
                    String agroObject = cursor.next().toString();
                    if (agroObjectsList.contains(agroObject)==false) {
                        agroObjectsList.add(agroObject);
                    }
                }
            }
        }
        return agroObjectsList;
    }
    
    public ArrayList<String> getExpFromAgroObject(ArrayList<String> agroObjectsList) {
        ExperimentDao experimentDAO = new ExperimentDao();
        ArrayList<String> studiesList = new ArrayList(); 
        
        try (Connection connection = experimentDAO.getDataSource().getConnection();
                Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            ) {            
            SQLQueryBuilder query = new SQLQueryBuilder();
            query.appendDistinct();
            query.appendSelect("named_graph");
            query.appendFrom("agronomical_object", "ao");
            query.appendINConditions("uri", agroObjectsList, "ao");
            
            try (ResultSet queryResult = statement.executeQuery(query.toString())) {
                while (queryResult.next()) {
                String studyURI = queryResult.getString("named_graph");
                studiesList.add(studyURI);
                }
            }
            
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(StudyDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return studiesList;
    }
    
    
    public ArrayList<String> getAgroObjectsFromGermplasm(List<String> germplasmDbIds) {
        //retrieve agro objects associated to germplasm (variety)
        AgronomicalObjectDAOSesame daoSesame= new AgronomicalObjectDAOSesame();
        daoSesame.getConnection();
        ArrayList<String> agroObjectsList = new ArrayList();
        for (String gp : germplasmDbIds){ 
//            SPARQLQueryBuilder query = new SPARQLQueryBuilder();
//            query.appendPrefix("p1", "<http://www.phenome-fppn.fr/vocabulary/2017#>");
//            query.appendTriplet("?uri", "p1:fromVariety", gp, null);   
            String query = "Prefix p1:<http://www.phenome-fppn.fr/vocabulary/2017#>\n" +
            "Select ?uri\n" +
            "where {\n" +
            " ?uri p1:fromVariety <http://www.phenome-fppn.fr/platform/v/variety4>\n" +
            "}";
            
            TupleQuery selectQuery = daoSesame.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query);
            TupleQueryResult result = selectQuery.evaluate();
                                   
            while(result.hasNext()) {
                BindingSet bindingSet = result.next();
                String uriAgronomicalObject = bindingSet.getValue("uri").stringValue();
                if (agroObjectsList.contains(uriAgronomicalObject)==false) {
                    agroObjectsList.add(uriAgronomicalObject);
                }
            }            
        }
        daoSesame.getConnection().close();                
        return agroObjectsList;
    }
    
    public ArrayList<Experiment> getExperimentsList() {
        ExperimentDao experimentDAO = new ExperimentDao();
        ArrayList<Experiment> expList = new ArrayList();
        
        try (final Connection connection = experimentDAO.getDataSource().getConnection();
                final Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            ) {            
            SQLQueryBuilder query = new SQLQueryBuilder();
            Map<String, String> sqlFields = relationFieldsObject();
            
            //Ajout des conditions dans la requête
            query.appendFrom("trial", "tr");
            if (programDbId != null) {
                query.appendJoin("INNER JOIN", "at_trial_project", "at","at.trial_uri=tr.uri");
            }
            query.appendANDWhereConditionIfNeeded(sqlFields.get("studyDbId"), studyDbId, "ILIKE", null, "tr");
            if (observationVariableDbIds.size()>0 && observationVariableDbIds != null) {
                ArrayList<String> studiesFromVar = getExpFromAgroObject(getAgroObjectFromVar(observationVariableDbIds));
                query.appendINConditions(sqlFields.get("uri"), studiesFromVar, "tr");
            }
            if (germplasmDbIds.size()>0) {
                ArrayList<String> studiesFromGermplasm = getExpFromAgroObject(getAgroObjectsFromGermplasm(germplasmDbIds));
                query.appendINConditions(sqlFields.get("uri"), studiesFromGermplasm, "tr");
            }
            
            query.appendANDWhereConditionIfNeeded(sqlFields.get("programDbId"), programDbId, "ILIKE", null, "at");
            query.appendANDWhereConditionIfNeeded(sqlFields.get("seasonDbId"), seasonDbId,"ILIKE", null, "tr");
            if (commonCropName!=null){
                query.appendANDWhereConditionIfNeeded(sqlFields.get("commonCropName"), "%"+commonCropName+"%","ILIKE", null, "tr");
            }
            query.appendOrderBy(sqlFields.get(sortBy), sortOrder);
            query.appendLimit(String.valueOf(limit));
            
            try (final ResultSet queryResult = statement.executeQuery(query.toString())) {
                UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();
                userDao.isAdmin(user);
                boolean isAdmin = (user.getAdmin().equals("t") || user.getAdmin().equals("true"));
                

                while (queryResult.next()) {
                    //SILEX:access
                    Experiment study = experimentDAO.get(queryResult);                    
                    if (isAdmin || experimentDAO.canUserSeeExperiment(user, study)) {
                        expList.add(experimentDAO.get(queryResult));
                    }
                    //\SILEX:access
                }
            }
            
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(StudyDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return expList;
    }
    
    public ArrayList<StudiesSearch>  transformExptoStudy(){        
        ArrayList<StudiesSearch> studiesList = new ArrayList();
        ArrayList<Experiment> expList = getExperimentsList();
                
        for (Experiment exp : expList){
            StudiesSearch study = new StudiesSearch();
            study.setStudyDbId(exp.getUri());
            study.setName(exp.getAlias());
            study.setStartDate(exp.getStartDate());
            study.setEndDate(exp.getEndDate());
            ArrayList<String> seasons = new ArrayList();
            seasons.add(exp.getCampaign());
            study.setSeasons(seasons);
            studiesList.add(study);
        }

        return studiesList;
    }
    
    public ArrayList<StudiesSearch> getStudiesProjects(ArrayList<StudiesSearch> studies) throws SQLException {
        ExperimentDao experimentDAO = new ExperimentDao();      

        try (final Connection connection = experimentDAO.getDataSource().getConnection();
                final Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            ) {        
            for (StudiesSearch study : studies) {
                SQLQueryBuilder query = new SQLQueryBuilder();
                query.appendSelect("p.name, p.uri");
                query.appendFrom("at_trial_project", "tp");
                query.appendANDWhereConditionIfNeeded("trial_uri", study.getStudyDbId(), "=", null, "tp");
                query.appendJoin(JoinAttributes.INNERJOIN, "project", "p", "p.uri = tp.project_uri");
                ResultSet queryResult = statement.executeQuery(query.toString());
                ArrayList<String> projectsIds = new ArrayList();
                ArrayList<String> projectsNames = new ArrayList();
                
                while (queryResult.next()) {                                      
                    projectsIds.add(queryResult.getString("uri"));
                    projectsNames.add(queryResult.getString("name"));
                }
                
                study.setProgramDbIds(projectsIds);
                study.setProgramNames(projectsNames);
            }            
        }       
        return studies;
    }
    
    public ArrayList<StudiesSearch> completeStudies(ArrayList<StudiesSearch> studiesList) throws SQLException {
        //add projects
        studiesList = getStudiesProjects(studiesList);
        //TODO : trialDBId, trialName, StudyType, LocationDbId, locationName        
        return studiesList;
    }
    
    public ArrayList<StudiesSearch> getStudiesList() throws SQLException {
        ArrayList<StudiesSearch> studiesList = transformExptoStudy();
        studiesList = completeStudies(studiesList);
        return studiesList;
    }  

    public Integer count(){
        ExperimentDao experimentDAO = new ExperimentDao();
        experimentDAO.uri = studyDbId;
        return experimentDAO.count();
    }
}
