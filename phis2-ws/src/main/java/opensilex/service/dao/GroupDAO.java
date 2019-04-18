//******************************************************************************
//                                GroupDAO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: April 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.ws.rs.core.Response;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.apache.jena.sparql.AlreadyExists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.dao.manager.PhisDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.User;
import opensilex.service.ontology.Foaf;
import opensilex.service.resource.dto.group.GroupDTO;
import opensilex.service.resource.dto.group.GroupPostDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sql.JoinAttributes;
import opensilex.service.utils.sql.SQLQueryBuilder;
import opensilex.service.view.brapi.Status;
import opensilex.service.model.Group;

/**
 * Groups DAO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class GroupDAO extends PhisDAO<Group, GroupDTO> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(GroupDAO.class);
    
    public String uri;
    public String name;
    public String level;
    public String description;
    
    public GroupDAO() {
        super();
        setTable("group");
        setTableAlias("g");
    }
    
    public GroupDAO(String uri) {
        super();
        this.uri = uri;
        setTable("group");
        setTableAlias("g");
    }

    @Override
    public POSTResultsReturn checkAndInsert(GroupDTO newObject) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private POSTResultsReturn checkAndInsertGroupList(List<GroupPostDTO> newGroups) throws SQLException, Exception {
        // init result returned maps
        List<Status> insertStatusList = new ArrayList<>();
        boolean dataState = true;
        boolean resultState = true;
        boolean insertionState = true;
        POSTResultsReturn results = null;
        ArrayList<Group> groups = new ArrayList<>();
        ArrayList<String> createdResourcesURIs = new ArrayList<>();
        
        for (GroupPostDTO groupDTO : newGroups) {
            Group group = groupDTO.createObjectFromDTO();
            groups.add(group);
        }
        if (dataState) {
            PreparedStatement insertPreparedStatement = null;
            PreparedStatement insertPreparedStatementGroupUser = null;
            
            final String insertGab = "INSERT INTO \"group\""
                                   + "(\"uri\", \"name\", "
                                   + "\"level\", \"description\")"
                                   + "VALUES (?, ?, ?, ?)";
            final String insertGabGroupUsers = "INSERT INTO \"at_group_users\""
                                    + "(\"group_uri\", \"users_email\")"
                                    + " VALUES (?, ?)";
            Connection connection = null;
            int inserted = 0;
            int exists = 0;
            
            try {
            // batch
                boolean insertionLeft = true;
                int count = 0;

                // connection nd transaction preparation
                connection = dataSource.getConnection();
                connection.setAutoCommit(false);

                insertPreparedStatement = connection.prepareStatement(insertGab);
                insertPreparedStatementGroupUser = connection.prepareStatement(insertGabGroupUsers);
                UriGenerator uriGenerator = new UriGenerator();
                
                for (Group group : groups) {
                    try {
                        // Generate group URI
                        group.setUri(uriGenerator.generateNewInstanceUri(Foaf.CONCEPT_GROUP.toString(), null, group.getName()));
                        insertionLeft = true;
                        insertPreparedStatement.setString(1, group.getUri());
                        insertPreparedStatement.setString(2, group.getName());
                        insertPreparedStatement.setString(3, group.getLevel());
                        insertPreparedStatement.setString(4, group.getDescription());
                        
                        // Traceability
                        String log = "";
                        if (remoteUserAdress != null) {
                            log += "IP Addess " + remoteUserAdress + " - ";
                        }
                        if (user != null) {
                            log += "User : " + user.getEmail() + " - ";
                        }
                        
                        LOGGER.debug(log + " quert : " + insertPreparedStatement.toString());
                        insertPreparedStatement.execute();
                        createdResourcesURIs.add(group.getUri());
                        
                        for (User u : group.getUsers()) {
                            insertPreparedStatementGroupUser.setString(1, group.getUri());
                            insertPreparedStatementGroupUser.setString(2, u.getEmail());
                            LOGGER.debug(log + " quert : " + insertPreparedStatementGroupUser.toString());
                            insertPreparedStatementGroupUser.execute();
                        }
                        inserted++;
                    } catch (AlreadyExists ex) {
                        // AlreadyExists throwed by the UriGenerator if the group uri generated already exists
                        exists++;
                        insertStatusList.add(new Status (StatusCodeMsg.ALREADY_EXISTING_DATA, StatusCodeMsg.INFO, group.getName() + " already exists"));
                    }
                    
                    // Insertion by batch
                    if (++count % batchSize == 0) {
                        insertPreparedStatement.executeBatch();
                        insertPreparedStatementGroupUser.execute();
                        insertionLeft = false;
                    }
                }
                
                if (insertionLeft) {
                    insertPreparedStatement.executeBatch(); // checkAndInsert remaining records
                    insertPreparedStatementGroupUser.executeBatch();
                }
                connection.commit();

                /**
                 * //SILEX:todo
                 * Tests to check
                 * //\SILEX:todo
                 */
                // If data existing and inserted
                if (exists > 0 && inserted > 0) {
                    results = new POSTResultsReturn(resultState, insertionState, dataState);
                    insertStatusList.add(new Status("Already existing data", StatusCodeMsg.INFO, "All groups already exist"));
                    results.setHttpStatus(Response.Status.CREATED);
                    results.statusList = insertStatusList;
                } else {
                    results = new POSTResultsReturn(resultState, insertionState, dataState);
                    if (exists > 0) { // if existing data and non inserted
                        results.setHttpStatus(Response.Status.CONFLICT); //409
                    } else { // If non existing data and therefor inserted
                        insertStatusList.add(new Status("Data inserted", StatusCodeMsg.INFO, String.valueOf(inserted) + " groups inserted"));
                    }
                }   
                results.createdResources = createdResourcesURIs;
                results.statusList = insertStatusList;
                
            } catch (SQLException e) {
                 LOGGER.error(e.getMessage(), e);
                
                // Rollback
                if (connection != null) {
                    connection.rollback();
                }
                
                results = new POSTResultsReturn(false, insertionState, dataState);
                insertStatusList.add(new Status("Error", StatusCodeMsg.POSTGRESQL_ERROR, e.getMessage()));
                if (e.getNextException() != null) {
                    insertStatusList.add(new Status("Error", StatusCodeMsg.POSTGRESQL_ERROR, e.getNextException().getMessage()));
                    insertStatusList.add(new Status("Error", StatusCodeMsg.ERR, "Duplicated group in json or in database"));
                }
                results.statusList = insertStatusList;
            } finally {
                if (insertPreparedStatement != null) {
                    insertPreparedStatement.close();
                }
                if (insertPreparedStatementGroupUser != null) {
                    insertPreparedStatementGroupUser.close();
                }
                if (connection != null) {
                    connection.close();
                }
            }
        } else {
            results = new POSTResultsReturn(resultState, insertionState, dataState);
            results.statusList = insertStatusList;
            results.createdResources = createdResourcesURIs;
        }
        
        return results;
    }

    public POSTResultsReturn checkAndInsertGroups(List<GroupPostDTO> newObjects) {
        POSTResultsReturn postResult;
        try {
            postResult = this.checkAndInsertGroupList(newObjects);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            postResult = new POSTResultsReturn(false, Response.Status.INTERNAL_SERVER_ERROR, e.toString());
        }
        return postResult;
    }

    @Override
    public Map<String, String> pkeySQLFieldLink() {
        Map<String, String> pkeySQLFieldLink = new HashMap<>();
        pkeySQLFieldLink.put("uri", "uri");
        return pkeySQLFieldLink;
    }

    @Override
    public Map<String, String> relationFieldsJavaSQLObject() {
        Map<String, String> createSQLFields = new HashMap<>();
        createSQLFields.put("uri", "uri");
        createSQLFields.put("name", "name");
        createSQLFields.put("level", "level");
        createSQLFields.put("description", "description");
        
        return createSQLFields;
    }

    @Override
    public Group findByFields(Map<String, Object> Attr, String table) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group single(int id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<Group> all() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group get(ResultSet result) throws SQLException {
        Group group = new Group();
        group.setUri(result.getString("uri"));
        group.setName(result.getString("name"));
        group.setLevel(result.getString("level"));
        group.setDescription(result.getString("description"));
        
        return group;
    }

    /**
     * 
     * @param group Group
     * @return list of users in group
     * @throws SQLException 
     */
    public ArrayList<User> getGroupUsers(Group group) throws SQLException {
        ResultSet result = null;
        Connection connection = null;
        Statement statement = null;
        ArrayList<User> groupUsers = new ArrayList<>();
        
        try {
            if (this.existInDB(group)) {
              // users recovery in at_group and users tables
              SQLQueryBuilder query = new SQLQueryBuilder();
              query.appendSelect("u.email, u.first_name, u.family_name");
              query.appendFrom("at_group_users", "gu");
              query.appendANDWhereConditionIfNeeded("group_uri", group.getUri(), "=", null, "gu");
              query.appendJoin(JoinAttributes.INNERJOIN, "users", "u", "gu.users_email = u.email");
              
              connection = dataSource.getConnection();
              statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,ResultSet.HOLD_CURSORS_OVER_COMMIT);
              
              result = statement.executeQuery(query.toString());
              while(result.next()) {
                  User u = new User(result.getString("email"));
                  u.setFirstName(result.getString("first_name"));
                  u.setFamilyName(result.getString("family_name"));
                  groupUsers.add(u);
              }
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(GroupDAO.class.getName()).log(Level.SEVERE, null, ex);
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
        return groupUsers;
    }
    
    @Override
    public ArrayList<Group> allPaginate() {
        ResultSet queryResult = null;
        Connection connection = null;
        Statement statement = null;
        
        ArrayList<Group> groups = new ArrayList<>();
        
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            SQLQueryBuilder query = new SQLQueryBuilder();
            
            Map<String, String> sqlFields = relationFieldsJavaSQLObject();
            
            // Add conditions to request
            query.appendFrom(table, tableAlias);
            query.appendANDWhereConditionIfNeeded(sqlFields.get("uri"), uri, "ILIKE", null, tableAlias);
            query.appendANDWhereConditionIfNeeded(sqlFields.get("name"), name, "ILIKE", null, tableAlias);
            query.appendANDWhereConditionIfNeeded(sqlFields.get("level"), level, "ILIKE", null, tableAlias);
            query.appendANDWhereConditionIfNeeded(sqlFields.get("description"), description, "ILIKE", null, tableAlias);
            query.appendLimit(String.valueOf(pageSize));
            query.appendOffset(Integer.toString(this.getPage() * this.getPageSize()));
            
            queryResult = statement.executeQuery(query.toString());
            
            while(queryResult.next()) {
                groups.add(get(queryResult));
            }
            
            for (Group group : groups) {
                group.setUserList(this.getGroupUsers(group));
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(GroupDAO.class.getName()).log(Level.SEVERE, null, ex);
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
                java.util.logging.Logger.getLogger(GroupDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return groups;
    }

    @Override
    public Integer count() {
        SQLQueryBuilder query = new SQLQueryBuilder();
        query.appendCount();
        query.appendDistinct();
        query.appendSelect(tableAlias + ".uri");
        query.appendFrom(table, tableAlias);
        
        if (uri != null) {
            query.appendANDWhereConditionIfNeeded("uri", uri, "=", null, tableAlias);
        }
        
        Connection connection = null;
        ResultSet resultSet = null;
        Statement statement = null;
        
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query.toString());
            
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return 0;
            }
        } catch (SQLException ex) {
            LOGGER.error(ex.getMessage(), ex);
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
    protected Group compareAndMergeObjects(Group fromDB, Group object) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected SQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Checks and updates groups.
     * @param updatedGroups
     * @return
     */
    private POSTResultsReturn checkAndUpdateGroupList(List<GroupDTO> updatedGroups) throws SQLException, Exception {
        // init result returned maps
        List<Status> insertStatusList = new ArrayList<>();
        boolean allGroupsAlreadyInDB = true;
        POSTResultsReturn results = null;

        ArrayList<Group> groups = new ArrayList<>();

        // Traceability
        String log = getTraceabilityLogs();

        // 1. Get user list and check of existance in database
        for (GroupDTO groupDTO : updatedGroups) {
            Group group = groupDTO.createObjectFromDTO();
            if (existInDB(group)) {
                groups.add(group);
            } else {
                allGroupsAlreadyInDB = false;
                insertStatusList.add(new Status("Data error", StatusCodeMsg.ERR, "Unknown group(s)"));
            }
        }

        //2. Database update according to given data.
        if (allGroupsAlreadyInDB) { // if all users in database, we can continue
            PreparedStatement updatePreparedStatementGroup = null;
            PreparedStatement deletePreparedStatementUserGroup = null;
            PreparedStatement insertPreparedStatementUserGroup = null;
            Connection connection = null;

            final String updateGabGroup = "UPDATE \"group\" SET \"level\" = ?, \"description\" = ? "
                    + "WHERE \"uri\" = ?";
            final String deleteGabUserGroup = "DELETE FROM \"at_group_users\" WHERE \"group_uri\" = ?";
            final String insertGabUserGroup = "INSERT INTO \"at_group_users\" (\"group_uri\", \"users_email\")"
                    + " VALUES(?, ?)";
            try {
                // Batch
                int count = 0;
                boolean insertionLeft = true;

                // Connection + transaction peparation
                connection = dataSource.getConnection();
                connection.setAutoCommit(false);

                updatePreparedStatementGroup = connection.prepareStatement(updateGabGroup);
                deletePreparedStatementUserGroup = connection.prepareStatement(deleteGabUserGroup);
                insertPreparedStatementUserGroup = connection.prepareStatement(insertGabUserGroup);

                for (Group group : groups) {
                    // Update group table
                    updatePreparedStatementGroup.setString(1, group.getLevel());
                    updatePreparedStatementGroup.setString(2, group.getDescription());
                    updatePreparedStatementGroup.setString(3, group.getUri());
                    updatePreparedStatementGroup.execute();
                    LOGGER.trace(log + " quert : " + updatePreparedStatementGroup.toString());
                    
                    // Delete Group/User links
                    deletePreparedStatementUserGroup.setString(1, group.getUri());
                    deletePreparedStatementUserGroup.execute();
                    LOGGER.trace(log + " quert : " + deletePreparedStatementUserGroup.toString());

                    // Insert new user/email links
                    if (group.getUsers() != null && !group.getUsers().isEmpty()) {
                        for (User u : group.getUsers()) {
                            insertPreparedStatementUserGroup.setString(1, group.getUri());
                            insertPreparedStatementUserGroup.setString(2, u.getEmail());
                            insertPreparedStatementUserGroup.execute();
                            LOGGER.trace(log + " quert : " + insertPreparedStatementUserGroup);
                        }
                    }

                    // Insertion by batch
                    if (++count % batchSize == 0) {
                        updatePreparedStatementGroup.executeBatch();
                        deletePreparedStatementUserGroup.executeBatch();
                        insertPreparedStatementUserGroup.executeBatch();
                        insertionLeft = false;
                    }
                }

                if (insertionLeft) {
                    updatePreparedStatementGroup.executeBatch();
                    deletePreparedStatementUserGroup.executeBatch();
                    insertPreparedStatementUserGroup.executeBatch();
                }

                connection.commit();

                insertStatusList.add(new Status("Data inserted", StatusCodeMsg.INFO, "groups updated"));
                results = new POSTResultsReturn(true, true, allGroupsAlreadyInDB);
            } catch (SQLException e) {
                LOGGER.error(e.getMessage(), e);

                //Rollback
                if (connection != null) {
                    connection.rollback();
                }

                results = new POSTResultsReturn(false, true, allGroupsAlreadyInDB);
                insertStatusList.add(new Status("Error", StatusCodeMsg.POSTGRESQL_ERROR, e.getMessage()));
                if (e.getNextException() != null) {
                    insertStatusList.add(new Status("Error", StatusCodeMsg.POSTGRESQL_ERROR, e.getNextException().getMessage()));
                }
                results.statusList = insertStatusList;
            } finally {
                if (updatePreparedStatementGroup != null) {
                    updatePreparedStatementGroup.close();
                }
                if (deletePreparedStatementUserGroup != null) {
                    deletePreparedStatementUserGroup.close();
                }
                if (insertPreparedStatementUserGroup != null) {
                    insertPreparedStatementUserGroup.close();
                }
                if (connection != null) {
                    connection.close();
                }
            }

        } else { // Some users are not already in the database
            results = new POSTResultsReturn(true, true, allGroupsAlreadyInDB);
            results.statusList = insertStatusList;
        }
        
        return results;
    }
    
    @Override
    public POSTResultsReturn checkAndUpdateList(List<GroupDTO> newObjects) {
         POSTResultsReturn postResult;
        try {
            postResult = this.checkAndUpdateGroupList(newObjects);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            postResult = new POSTResultsReturn(false, Response.Status.INTERNAL_SERVER_ERROR, e.toString());
        }
        return postResult;
    }

    @Override
    public POSTResultsReturn checkAndInsertList(List<GroupDTO> newObjects) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Group> create(List<Group> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Group> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Group> update(List<Group> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Group> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
