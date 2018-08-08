//******************************************************************************
//                                       StudyDAO.java
//
// Author(s): boizetal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 30 juil. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  30 juil. 2018
// Subject:
//******************************************************************************
package phis2ws.service.dao.phis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.dao.manager.DAOPhisBrapi;
import phis2ws.service.utils.sql.SQLQueryBuilder;
import static phis2ws.service.dao.phis.ExperimentDao.LOGGER;
import phis2ws.service.model.User;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.sql.JoinAttributes;
import phis2ws.service.view.model.phis.Group;
import phis2ws.service.view.model.phis.Project;
import phis2ws.service.view.model.phis.StudiesSearch;


/**
 *
 * @author boizetal
 */
public class StudyDAO extends DAOPhisBrapi<StudiesSearch, StudiesSearch>{
    final static Logger LOGGER = LoggerFactory.getLogger(StudyDAO.class);
    
    public String studyType; 
    public String trialDbId;
    public String programDbId;
    public String commonCropName;
    public String locationDbId;
    public String seasonDbId;    
    public String studyDbId;
    public List<String> germplasmDbIds;
    public ArrayList<String> observationVariableDbIds;
    public Boolean active;
    public String sortBy;
    public String sortOrder;


    public StudyDAO() {
        super();
        setTable("trial");
        setTableAlias("tr");
    }
    
    public StudyDAO(String studyDbId) {
        super();
        this.studyDbId = studyDbId;
        setTable("trial");
        setTableAlias("tr");
    }
    
    
    @Override
    public Map<String, String> pkeySQLFieldLink() {
       Map<String, String> pkeySQLFieldLink = new HashMap<>();
       pkeySQLFieldLink.put("studyDbId", "uri");
       return pkeySQLFieldLink;
    }
    
    
    
    @Override
    public Map<String, String> relationFieldsJavaSQLObject() {
        Map<String, String> createSQLFields = new HashMap<>();
        createSQLFields.put("studyDbId","uri");
        createSQLFields.put("programDbId","project_uri");
        //createSQLFields.put("trialDbId", "");
        //createSQLFields.put("location", "field");
        createSQLFields.put("seasonDbId", "campaign");
        createSQLFields.put("commonCropName", "crop_species");
               
        return createSQLFields;
    }
    

//            
//        

    

    @Override
    public StudiesSearch get(ResultSet result) throws SQLException {
        StudiesSearch study = new StudiesSearch();
        study.setStudyDbId(result.getString("uri"));
        study.setName(result.getString("alias"));
        study.setTrialDbId(trialDbId);
        study.setTrialName(trialDbId);
        study.setStudyType(studyType);
        study.setSeasons(result.getString("campaign"));
        study.setLocationDbId(locationDbId);
        study.setLocationName(locationDbId);
        study.setStartDate(result.getString("start_date"));
        study.setEndDate(result.getString("end_date"));
        study.setActive(active);

        
        ResultSet queryResult = null;
        Connection connection = null;
        Statement statement = null;
        ArrayList<String> programDbIds = new ArrayList();
        ArrayList<String> programDbNames = new ArrayList();
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            String query = "SELECT p.uri, p.name FROM project AS p INNER JOIN at_trial_project AS at ON at.project_uri=p.uri WHERE at.trial_uri='"+study.getStudyDbId()+"'";
            queryResult = statement.executeQuery(query);

            while (queryResult.next()) {
                String programId = queryResult.getString("uri");
                String programName = queryResult.getString("name");
                programDbIds.add(programId);
                programDbNames.add(programName);
                }
            study.setProgramDbIds(programDbIds);
            study.setProgramNames(programDbNames);
        }
        catch (SQLException ex) {
            java.util.logging.Logger.getLogger(StudyDAO.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (queryResult != null) {
                    queryResult.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(StudyDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return study;
    
    }
        

    
     /**
     * 
     * @param study Study
     * @return la liste des groupes ayant accès à l'expérimentation (lite des noms de groupes)
     * @throws java.sql.SQLException
     */
    public ArrayList<Group> getStudyGroups(StudiesSearch study) throws SQLException {
        ResultSet result = null;
        Connection connection = null;
        Statement statement = null;
        ArrayList<Group> experimentGroups = new ArrayList<>();
        
        try {
            if (this.existInDB(study)) {
                //Récupération des groupes dans les tables at_group_trial et group
                SQLQueryBuilder query = new SQLQueryBuilder();
                query.appendSelect("gp.uri, gp.level, gp.name");
                query.appendFrom("at_group_trial", "gt");
                query.appendANDWhereConditionIfNeeded("trial_uri", study.getStudyDbId(), "=", null, "gt");
                query.appendJoin(JoinAttributes.INNERJOIN, "group", "gp", "gt.group_uri = gp.uri");
                
                connection = dataSource.getConnection();
                statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,ResultSet.HOLD_CURSORS_OVER_COMMIT);

                result = statement.executeQuery(query.toString());
                while (result.next()) {
                    Group group = new Group(result.getString("uri"));
                    group.setLevel(result.getString("level"));
                    group.setName(result.getString("name"));
                    experimentGroups.add(group);
                }
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(ExperimentDao.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (connection != null) {
                connection.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (result != null) {
                result.close();
            }
        }
        
        return experimentGroups;
    }
    
    
    
    /**
     * 
     * @param u User 
     * @param experiment Experiment
     * @return true si l'utilisateur fait partie d'un des groupes ayant accès à l'experimentation, false sinon
     */
    public boolean canUserSeeStudy(User u, StudiesSearch study) {
        try {
            UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();

            ArrayList<Group> studyGroups = this.getStudyGroups(study);
            //Quand l'essai n'est dans aucun groupe, il est public donc l'utilisateur peut le consulter
            if (studyGroups.isEmpty()) { 
                return true;
            } else {
                ArrayList<Group> userGroups = userDao.getUserGroups(u);
                for (Group userGroup : userGroups) {
                    for (Group experimentGroup : studyGroups) {
                        if (userGroup.getUri().equals(experimentGroup.getUri())) {
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(ExperimentDao.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return false;
    }
    
    
    

    
    

    @Override
    public ArrayList<StudiesSearch> allPaginate() {
        ResultSet queryResult = null;
        Connection connection = null;
        Statement statement = null;
        ArrayList<StudiesSearch> studiesList = new ArrayList();
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            SQLQueryBuilder query = new SQLQueryBuilder();
            
            Map<String, String> sqlFields = relationFieldsJavaSQLObject();
            
            //Ajout des conditions dans la requête
            query.appendFrom("trial", "tr");
            if (programDbId != null) {
                query.appendJoin("INNER JOIN", "at_trial_project", "at","at.trial_uri=tr.uri");
            }
            query.appendANDWhereConditionIfNeeded(sqlFields.get("studyDbId"), studyDbId, "ILIKE", null, "tr");
            query.appendANDWhereConditionIfNeeded(sqlFields.get("programDbId"), programDbId, "ILIKE", null, "at");
            query.appendANDWhereConditionIfNeeded(sqlFields.get("seasonDbId"), seasonDbId,"ILIKE", null, "tr");
            if (commonCropName!=null){
                query.appendANDWhereConditionIfNeeded(sqlFields.get("commonCropName"), "%"+commonCropName+"%","ILIKE", null, "tr");
            }
            query.appendOrderBy(sqlFields.get(sortBy), sortOrder);
            query.appendLimit(String.valueOf(pageSize));
            
            queryResult = statement.executeQuery(query.toString());
            
            UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();
            userDao.isAdmin(user);
            boolean isAdmin = (user.getAdmin().equals("t") || user.getAdmin().equals("true"));

            while (queryResult.next()) {
                //SILEX:access
                StudiesSearch study = get(queryResult);
                if (isAdmin || canUserSeeStudy(user, study)) {
                    studiesList.add(get(queryResult));
                }
                //\SILEX:access
            }
            
//            for (Study study : studyList) {
//                study.setGroupList(this.getStudyGroups(study));
//            }    
            
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(StudyDAO.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (queryResult != null) {
                    queryResult.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(StudyDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return studiesList;
    }
    
    @Override
    public Integer count() {
     
     SQLQueryBuilder query = new SQLQueryBuilder();
     query.appendCount();
     query.appendDistinct();
     query.appendSelect(tableAlias + ".uri");
     query.appendFrom(table, tableAlias);
     
     if (studyDbId != null) {
         query.appendWhereConditions("uri", studyDbId, "=", null, tableAlias);
     }
     
     Connection connection = null;
     ResultSet resultSet = null;
     Statement statement = null;
     
     try {
         connection = dataSource.getConnection();
         statement = connection.createStatement();
         resultSet = statement.executeQuery(query.toString());
         
         if(resultSet.next()) {
             return resultSet.getInt(1);
         }else{
             return 0;
         }
         
         
     } catch(SQLException e) {
         LOGGER.error(e.getMessage(), e);
         return null;
     } finally {
         try {
             if (statement != null) {
                 statement.close();
             }
             if (resultSet != null) {
                 resultSet.close();
             }
             if (connection != null) {
                 connection.close();
             }
         } catch (SQLException ex) {
             LOGGER.error(ex.getMessage(), ex);
         }
    }
    }
    

    @Override
    protected StudiesSearch compareAndMergeObjects(StudiesSearch fromDB, StudiesSearch object) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected SQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
        
    @Override
    public POSTResultsReturn checkAndInsert(StudiesSearch newObject) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public POSTResultsReturn checkAndInsertList(List<StudiesSearch> newObjects) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public POSTResultsReturn checkAndUpdateList(List<StudiesSearch> newObjects) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    @Override
    public StudiesSearch findByFields(Map<String, Object> Attr, String table) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StudiesSearch single(int id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<StudiesSearch> all() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
   
}
