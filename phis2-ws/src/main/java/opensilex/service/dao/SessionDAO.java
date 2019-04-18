//******************************************************************************
//                                SessionDAO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: May 2016
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.dao.manager.PhisDAO;
import opensilex.service.authentication.Session;
import opensilex.service.authentication.TokenManager;
import opensilex.service.model.User;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.sql.SQLQueryBuilder;

/**
 * Session DAO.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class SessionDAO extends PhisDAO<Session, Object> {

    final static Logger LOGGER = LoggerFactory.getLogger(SessionDAO.class);

    public SessionDAO() {
        super();
        setTable("session");
    }

    @Override
    public Integer count() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, String> pkeySQLFieldLink() {
        Map<String, String> pkeySQLFieldLink = new HashMap<>();
        pkeySQLFieldLink.put("id", "id");
        return pkeySQLFieldLink;
    }

    @Override
    public Map<String, String> relationFieldsJavaSQLObject() {
        Map<String, String> createSQLFields = new HashMap<>();
        createSQLFields.put("id", "id");
        createSQLFields.put("email", "email");
        createSQLFields.put("date", "date");
        createSQLFields.put("date_end", "date_end");
        return createSQLFields;
    }

    @Override
    public Session findByFields(Map<String, Object> Attr, String table) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Session single(int id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<Session> all() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Session get(ResultSet result) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<Session> allPaginate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected Session compareAndMergeObjects(Session fromDB, Session object) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected SQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void endSession(String sessionId) {
        Statement statement = null;
        Connection con = null;
        ResultSet rs = null;
        try {
            con = dataSource.getConnection();
            statement = con.createStatement();
            String query = "UPDATE " + table + " SET date_end = now() WHERE id = '" + sessionId + "'";
//            logger.debug(query);
            statement.executeUpdate(query);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {

            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOGGER.error(e.getMessage(), e);
                }

            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }
    
     public void reloadActiveSession() { 
        Statement statement = null; 
        Connection con = null; 
        ResultSet rs = null; 
        try { 
            con = dataSource.getConnection(); 
            statement = con.createStatement(); 
            SQLQueryBuilder query = new SQLQueryBuilder(); 
            query.appendFrom(table, null); 
            query.addISNULL("date_end"); 
            rs = statement.executeQuery(query.toString()); 
            if (rs != null) { 
                while (rs.next()) { 
                    if (rs.getString("email") != null) { 
                        String email = rs.getString("email"); 
                        User u = new User(email); 
                        u = loadFromDB(u); 
                        Session s = new Session(rs.getString("id"), email, u); 
                        if (u != null ) { 
                            TokenManager.Instance().createTokenFromBD(s); 
                        } 
                    } 
                } 
            } 
        } catch (SQLException e) { 
            LOGGER.error(e.getMessage(), e); 
        } finally { 
 
            if (rs != null) { 
                try { 
                    rs.close(); 
                } catch (SQLException e) { 
                    LOGGER.error(e.getMessage(), e); 
                } 
            } 
            if (statement != null) { 
                try { 
                    statement.close(); 
                } catch (SQLException e) { 
                    LOGGER.error(e.getMessage(), e); 
                } 
            } 
            if (con != null) { 
                try { 
                    con.close(); 
                } catch (SQLException e) { 
                    LOGGER.error(e.getMessage(), e); 
                } 
            } 
        } 
    } 
 
    /** 
     * Loads user attributes
     * @param user with email and password
     * @return a User
     */ 
    private User loadFromDB(User user) { 
        UserDAO userDao = new UserDAO(); 
        try { 
            user = userDao.find(user); 
        } catch (Exception ex) { 
            LOGGER.error(ex.getMessage(), ex); 
        } 
        return user; 
    } 

    @Override
    public POSTResultsReturn checkAndInsertList(List<Object> newObjects) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public POSTResultsReturn checkAndInsert(Object newObject) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public POSTResultsReturn checkAndUpdateList(List<Object> newObjects) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Session> create(List<Session> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Session> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Session> update(List<Session> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Session findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Session> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
} 
    
