//******************************************************************************
//                               StudyDAO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 22 August 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************

package opensilex.service.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.utils.sql.SQLQueryBuilder;
import opensilex.service.model.User;
import opensilex.service.utils.sql.JoinAttributes;
import opensilex.service.model.Experiment;
import opensilex.service.model.ContactBrapi;
import opensilex.service.dao.manager.DAO;
import opensilex.service.model.StudyDetails;

/**
 * Study DAO.
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class StudyDAO extends DAO<StudyDetails>{
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
     
    /**
     * Corresponding between base fields and service research fields.
     * @return createSQLFields
     */
    public Map<String, String> relationFieldsObject() {
        Map<String, String> createSQLFields = new HashMap<>();
        createSQLFields.put("studyDbId","uri");
              
        return createSQLFields;
    }
   
    /**
     * Collects a list of Experiment objects corresponding to the user search. 
     * @return expList list of Experiment
     */
    public ArrayList<Experiment> getExperimentsList() {
        ExperimentSQLDAO experimentDAO = new ExperimentSQLDAO();
        ArrayList<Experiment> expList = new ArrayList();
        
        try (final Connection connection = experimentDAO.getDataSource().getConnection();
                final Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            ) {            
            SQLQueryBuilder query = new SQLQueryBuilder();
            Map<String, String> sqlFields = relationFieldsObject();
            
            //SILEX:info
            // Building the query
            query.appendFrom("trial", "tr");   
            query.appendANDWhereConditionIfNeeded(sqlFields.get("studyDbId"), studyDbId, "ILIKE", null, "tr");           
            //\SILEX:info
            
            try (final ResultSet queryResult = statement.executeQuery(query.toString())) {
                UserDAO userDao = new UserDAO();
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
    
    /**
    * Fills all the StudyDetails attributes.
    * @return study the StudyDetails object corresponding to the study requested by the user
    * @throws SQLException
    */
    public StudyDetails getStudyInfo() throws SQLException {
        StudyDetails study = transformExptoStudyDetails();
        if (study.getStudyDbId() != null) {
            study = getStudyContacts(study);
            study = getStudyActive(study); 
            //SILEX : todo
            //add these information : trialDBId, trialName, StudyType, StudyDescription, License, Location, Datalinks, lastupdate
            //\SILEX:todo 
        }
        return study;        
    }

    public Integer count(){
        ExperimentSQLDAO experimentDAO = new ExperimentSQLDAO();
        experimentDAO.uri = studyDbId;
        return experimentDAO.count();
    }    
    
    /**
    * Transforms the collected Experiments into a list of StudiesDetails objects.
    * @return studiesList list of StudiesSearch objects
    */
    private StudyDetails transformExptoStudyDetails() {
        StudyDetails study = new StudyDetails();
        
        //SILEX : info
        //retrieve a list of experiments based on the study URI, the study list has only one element;
        ArrayList<Experiment> expList = getExperimentsList();
        //retrieve the requested study;
        if (expList.size() > 0) {
            Experiment exp = expList.get(0); 
            study.setStudyDbId(exp.getUri());
            study.setStudyName(exp.getAlias());
            study.setStudyDescription(exp.getComment());
            study.setStartDate(exp.getStartDate());
            study.setEndDate(exp.getEndDate());
            ArrayList<String> seasons = new ArrayList();
            seasons.add(exp.getCampaign());
            study.setSeasons(seasons);
            study.setCommonCropName(exp.getCropSpecies());
            study.setDataLinks(new ArrayList());
        }
        //\SILEX : info      
        
        return study;
    }
    
    /**
    * Gets the BrapiContacts of a StudyDetails.
    * @return study 
    */
    private StudyDetails getStudyContacts(StudyDetails study) throws SQLException {
        ExperimentSQLDAO experimentDAO = new ExperimentSQLDAO();      

        try (final Connection connection = experimentDAO.getDataSource().getConnection();
                final Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            ) {             
            SQLQueryBuilder query = new SQLQueryBuilder();
            query.appendSelect("u.email, u.first_name, u.family_name, tu.type, u.affiliation, u.orcid");
            query.appendFrom("at_trial_users", "tu");
            query.appendANDWhereConditionIfNeeded("trial_uri", study.getStudyDbId(), "=", null, "tu");
            query.appendJoin(JoinAttributes.INNERJOIN, "users", "u", "u.email = tu.users_email");

            ResultSet queryResult = statement.executeQuery(query.toString());
            while (queryResult.next()) {
                ContactBrapi contact = new ContactBrapi();
                contact.setContactDbId(queryResult.getString("email"));
                contact.setEmail(queryResult.getString("email"));
                String fullName = queryResult.getString("first_name") + " " + queryResult.getString("family_name");
                contact.setName(fullName);                
                contact.setType(queryResult.getString("type"));
                contact.setInstituteName(queryResult.getString("affiliation"));
                contact.setOrcid(queryResult.getString("orcid"));
                study.addContact(contact);
            }            
        }
        return study;
    }

    /**
    * Fills the active attribute of a StudyDetails.
    * @return study 
    */
    private StudyDetails getStudyActive(StudyDetails study) throws SQLException {
        ExperimentSQLDAO experimentDAO = new ExperimentSQLDAO();
        
            try (final Connection connection = experimentDAO.getDataSource().getConnection();
            final Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            ) {             
            SQLQueryBuilder query = new SQLQueryBuilder();
            query.appendSelect("t.uri, t.start_date, t.end_date");
            query.appendFrom("trial", "t");
            query.appendANDWhereConditionIfNeeded("uri", study.getStudyDbId(), "=", null, "t");

            ResultSet queryResult = statement.executeQuery(query.toString());
            while (queryResult.next()) {
                Timestamp startDate = queryResult.getTimestamp("start_date");
                Timestamp endDate = queryResult.getTimestamp("end_date");
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                if (startDate.compareTo(timestamp) <= 0  && (endDate==null | endDate.compareTo(timestamp) >= 0 )) {
                    study.setActive(true);
                } else {
                    study.setActive(false);                        
                }
            }            
        }
        return study;        
    }

    @Override
    public List<StudyDetails> create(List<StudyDetails> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<StudyDetails> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<StudyDetails> update(List<StudyDetails> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StudyDetails find(StudyDetails object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StudyDetails findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<StudyDetails> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void initConnection() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void closeConnection() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void startTransaction() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void commitTransaction() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void rollbackTransaction() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
