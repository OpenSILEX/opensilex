//******************************************************************************
//                                StudySQLDAO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 mai 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import opensilex.service.dao.manager.PostgreSQLDAO;
import opensilex.service.datasource.PostgreSQLDataSource;
import opensilex.service.model.Experiment;
import opensilex.service.resource.dto.experiment.StudyDTO;
import opensilex.service.utils.sql.SQLQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Study SQL DAO 
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class StudySQLDAO extends PostgreSQLDAO<StudyDTO> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(StudySQLDAO.class);
    
    public String active;
    public ArrayList<String> commonCropNames;
    public ArrayList<String> seasonDbIds;
    public ArrayList<String> studyDbIds;
    public ArrayList<String> studyNames;
    public String sortBy;
    public String sortOrder;
    public Integer page;
    public Integer pageSize;
    
    @Override
    public Map<String, String> pkeySQLFieldLink() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, String> relationFieldsJavaSQLObject() {
        Map<String, String> createSQLFields = new HashMap<>();
        createSQLFields.put("studyDbId","uri");
        createSQLFields.put("startDate", "start_date");
        createSQLFields.put("endDate", "end_date");
        createSQLFields.put("studyName", "alias");
        createSQLFields.put("commonCropName", "crop_species");  
        createSQLFields.put("season", "campaign");  
        return createSQLFields;
    }

    @Override
    public StudyDTO findByFields(Map<String, Object> Attr, String table) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StudyDTO single(int id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<StudyDTO> all() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StudyDTO get(ResultSet resultReturnedFromDatabase) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    @Override
    public ArrayList<StudyDTO> allPaginate() {
        ResultSet queryResult = null;
        Connection connection = null;
        Statement statement = null;
        ArrayList<StudyDTO> studies = new ArrayList();
        
        try {
            if (dataSource == null) {
                dataSource = PostgreSQLDataSource.getInstance();
            }
            connection = dataSource.getConnection();
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            SQLQueryBuilder query = prepareSearchQuery();
            
            queryResult = statement.executeQuery(query.toString());
            
            LOGGER.debug (query.toString());
            
            UserDAO userDao = new UserDAO();
            userDao.isAdmin(user);
            boolean isAdmin = (user.getAdmin().equals("t") || user.getAdmin().equals("true"));

            while (queryResult.next()) {  
                StudyDTO study = new StudyDTO();
                Map<String, String> sqlFields = relationFieldsJavaSQLObject();
                study.setStudyDbId(queryResult.getString(sqlFields.get("studyDbId")));
                study.setStudyName(queryResult.getString(sqlFields.get("studyName")));
                study.setName(queryResult.getString(sqlFields.get("studyName")));
                study.setCommonCropName(queryResult.getString(sqlFields.get("commonCropName")));
                study.setStartDate(queryResult.getString(sqlFields.get("startDate")));
                study.setEndDate(queryResult.getString(sqlFields.get("endDate")));
                ArrayList<String> seasons = new ArrayList();
                seasons.add(queryResult.getString(sqlFields.get("season")));
                study.setSeasons(seasons);
                Timestamp startDate = queryResult.getTimestamp("start_date");
                Timestamp endDate = queryResult.getTimestamp("end_date");
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                if (startDate.compareTo(timestamp) <= 0  && (endDate==null | endDate.compareTo(timestamp) >= 0 )) {
                    study.setActive("true");
                } else {
                    study.setActive("false");                        
                }
//              
                //SILEX:INFO
                //check if the user has access to the study  
                Experiment experiment = new Experiment(study.getStudyDbId());
                ExperimentSQLDAO expDAO = new ExperimentSQLDAO();
                if (isAdmin || expDAO.canUserSeeExperiment(user, experiment)) {
                    studies.add(study);
                }
                //\SILEX:INFO
            }
                      
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(StudySQLDAO.class.getName()).log(Level.SEVERE, null, ex);
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
                java.util.logging.Logger.getLogger(ExperimentSQLDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return studies;
    }

    @Override
    public Integer count() {
        SQLQueryBuilder countQuery = prepareSearchQuery();
        countQuery.appendCount();
        countQuery.appendDistinct();
        countQuery.appendSelect(tableAlias + ".uri");
        countQuery.orderBy = null;
        
        LOGGER.debug (countQuery.toString());
        
        Connection connection = null;
        ResultSet resultSet = null;
        Statement statement = null;

        try {
            if (dataSource == null) {
                dataSource = PostgreSQLDataSource.getInstance();
            }
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(countQuery.toString());

            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return 0;
            }
        } catch (SQLException e) {
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
    protected StudyDTO compareAndMergeObjects(StudyDTO firstObject, StudyDTO secondObject) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Builds the SQL query to get studies filtered on different attributes
     * @example 
     * SELECT * FROM trial
     * WHERE alias IN ("studyA","studyB")
     *
     * @return the SQL query
     */
    @Override
    protected SQLQueryBuilder prepareSearchQuery() {
        
        SQLQueryBuilder query = new SQLQueryBuilder();
        tableAlias = "tr";
        table = "trial";
        query.appendFrom(table, tableAlias);
        Map<String, String> sqlFields = relationFieldsJavaSQLObject();
        
        if (studyDbIds != null) {
            if (!studyDbIds.isEmpty()) {
                query.appendINConditions(sqlFields.get("studyDbId"), studyDbIds, tableAlias);
            }
        }        
        if (studyNames != null) {
            if (!studyNames.isEmpty()) {
                query.appendINConditions(sqlFields.get("studyName"), studyNames, tableAlias);
            }
        }
        if (commonCropNames != null) {
            if (!commonCropNames.isEmpty()) {
                query.appendINConditions(sqlFields.get("commonCropName"), commonCropNames, tableAlias);
            }
        }
        if (seasonDbIds != null) {
            if (!seasonDbIds.isEmpty()) {
                query.appendINConditions(sqlFields.get("season"), seasonDbIds, tableAlias);
            }
        }
        
        if (active != null) {
            if (query.where.length() > 0) {
                query.addAND();
            }             
            query.where += "(";
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date todayDate = new Date();
            if ("true".equals(active)) {                
                query.appendWhereConditions("start_date", dateFormat.format(todayDate), "<=", null, tableAlias);
                query.appendANDWhereConditions("end_date", dateFormat.format(todayDate), ">=", null, tableAlias);
            } else if ("false".equals(active)) {
                query.appendWhereConditions("start_date", dateFormat.format(todayDate), ">", null, tableAlias);
                query.appendORWhereConditions("end_date", dateFormat.format(todayDate), "<", null, tableAlias);
            }
            query.where += ")";
        }
        
        if (sortBy != null) {
            if (sortOrder == null) {
                sortOrder = "ASC";
            }
            query.appendOrderBy(sqlFields.get(sortBy), sortOrder);                     
        }
        query.appendLimit(String.valueOf(pageSize));
        query.appendOffset(Integer.toString(this.getPage() * this.getPageSize()));

        return query;
    }
    
    private String ListToString(ArrayList<String> list) {
        String listInString = "(";
        for (String element:list) {
            listInString = listInString + "'" + element + "',";
            
        }
        //remove last ","
        listInString = listInString.substring(0, listInString.length()-1);

        listInString = listInString + ")";    

        return listInString;
    }

    @Override
    public List<StudyDTO> create(List<StudyDTO> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<StudyDTO> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<StudyDTO> update(List<StudyDTO> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StudyDTO findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<StudyDTO> objects) throws DAOPersistenceException, DAODataErrorAggregateException, DAOPersistenceException, ResourceAccessDeniedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
        
}
