//******************************************************************************
//                                  UserDAO.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: May 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr, 
//          morgane.vidal@inra.fr
//******************************************************************************
package opensilex.service.dao;

import opensilex.service.model.User;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.dao.manager.PhisDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.ontology.Foaf;
import opensilex.service.resource.dto.UserDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.ResourcesUtils;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sql.JoinAttributes;
import opensilex.service.utils.sql.SQLQueryBuilder;
import opensilex.service.view.brapi.Status;
import opensilex.service.model.Group;

/**
 * User DAO.
 * @update [Morgane Vidal] Apr. 2017: deletion of isAdmin, role and type 
 * attributes in User table which caused the deletion of isAdmin,
 * getProjectUserType, getUserGroup, getUserRole and getUserExperiment functions
 * @update [Arnaud Charleroy] Jul. 2018: Add URI generation from e-mail
 * @update [Arnaud Charleroy] Sept. 2018: Pagination fixed
 * @update [Morgane Vidal] 8 Nov. 2018: fix users update (orcid) 
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class UserDAO extends PhisDAO<User, UserDTO> {

    final static Logger LOGGER = LoggerFactory.getLogger(UserDAO.class);

    public String email;
    public String password;
    public String firstName;
    public String familyName;
    public String address;
    public String phone;
    public String affiliation;
    public String orcid;
    public Boolean admin;
    public String available;
    public String uri; // search by uri

    public UserDAO() {
        super();
        setTable("users");
        setTableAlias("u");
    }

    public UserDAO(String email) {
        super();
        this.email = email;
        setTable("users");
        setTableAlias("u");
    }

    /**
     * @param userEmail
     * @return the password corresponding to the user's email sent
     */
    public String getPasswordFromDb(String userEmail) {
        ResultSet result = null;
        Connection con = null;
        Statement stat = null;
        try {
            con = dataSource.getConnection();
            String query = "SELECT password FROM users WHERE email = '" + userEmail + "'";
            stat = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            result = stat.executeQuery(query);
            while (result.next()) {
                return result.getString("password");
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (SQLException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
            if (stat != null) {
                try {
                    stat.close();
                } catch (SQLException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }

        return "";
    }

    /**
     * Checks if a user is admin.
     * @param user
     * @return
     */
    public Boolean isAdmin(User user) {
        if (user.getAdmin() == null) {
            ResultSet result = null;
            Connection con = null;
            Statement stat = null;
            try {
                con = dataSource.getConnection();
                String query = "SELECT isadmin FROM users WHERE email='" + user.getEmail() + "'";
                stat = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
                result = stat.executeQuery(query);

                while (result.next()) {
                    user.setAdmin(result.getString("isadmin"));
                }

                return ResourcesUtils.getStringBooleanValue(user.getAdmin());
            } catch (SQLException ex) {
                LOGGER.error(ex.getMessage(), ex);
            } finally {
                if (result != null) {
                    try {
                        result.close();
                    } catch (SQLException ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }
                }
                if (stat != null) {
                    try {
                        stat.close();
                    } catch (SQLException ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }
                }
            }
        } else {
            return ResourcesUtils.getStringBooleanValue(user.getAdmin());
        }
        return false;
    }

    /**
     * Checks if a user URI exists
     * @param userUri user
     * @return boolean true if user URI exists
     *                 false if not
     */
    public Boolean existUserUri(String userUri) {
        boolean valid = false;
        ResultSet result = null;
        Connection con;
        Statement stat = null;
        try {
            con = dataSource.getConnection();
            SQLQueryBuilder sqlQueryBuilder = new SQLQueryBuilder();
            sqlQueryBuilder.appendSelect("*");
            sqlQueryBuilder.appendFrom(table, tableAlias);
            sqlQueryBuilder.appendANDWhereConditionIfNeeded("uri", userUri, "=", null, tableAlias);
            
            stat = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            result = stat.executeQuery(sqlQueryBuilder.toString());
            LOGGER.debug(SQL_SELECT_QUERY + " " + sqlQueryBuilder.toString());
            if (result.next()) {
                valid = true;
            }

        } catch (SQLException ex) {
            LOGGER.error(ex.getMessage(), ex);
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (SQLException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
            if (stat != null) {
                try {
                    stat.close();
                } catch (SQLException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }

        return valid;
    }

    @Override
    public Integer count() {
        SQLQueryBuilder query = new SQLQueryBuilder();
        query.appendCount();
        query.appendDistinct();
        query.appendSelect(tableAlias + ".email");
        query.appendFrom(table, tableAlias);
        
        Map<String, String> sqlFields = relationFieldsJavaSQLObject();
        query.appendANDWhereConditionIfNeeded(sqlFields.get("email"), email, "ILIKE", null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("firstName"), firstName, "ILIKE", null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("familyName"), familyName, "ILIKE", null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("address"), address, "ILIKE", null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("phone"), phone, "ILIKE", null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("affiliation"), affiliation, "ILIKE", null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("orcid"), orcid, "ILIKE", null, tableAlias);
        if (admin != null) {
            query.appendANDWhereConditionIfNeeded(sqlFields.get("admin"), String.valueOf(admin), "=", null, tableAlias);
        }
        if (available != null) {
            query.appendANDWhereConditionIfNeeded(sqlFields.get("available"), String.valueOf(available), "=", null, tableAlias);
        }
        if (uri != null) {
            query.appendANDWhereConditionIfNeeded(sqlFields.get("uri"), String.valueOf(uri), "=", null, tableAlias);
        }

        Connection connection = null;
        ResultSet resultSet = null;
        Statement statement = null;

        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            LOGGER.trace(SQL_SELECT_QUERY + query.toString());
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
    public Map<String, String> pkeySQLFieldLink() {
        Map<String, String> pkeySQLFieldLink = new HashMap<>();
        pkeySQLFieldLink.put("email", "email");
        return pkeySQLFieldLink;
    }

    @Override
    public Map<String, String> relationFieldsJavaSQLObject() {
        Map<String, String> createSQLFields = new HashMap<>();
        createSQLFields.put("email", "email");
        createSQLFields.put("password", "password");
        createSQLFields.put("firstName", "first_name");
        createSQLFields.put("familyName", "family_name");
        createSQLFields.put("address", "address");
        createSQLFields.put("phone", "phone");
        createSQLFields.put("affiliation", "affiliation");
        createSQLFields.put("orcid", "orcid");
        createSQLFields.put("admin", "isadmin");
        createSQLFields.put("available", "available");

        return createSQLFields;
    }

    @Override
    public User findByFields(Map<String, Object> Attr, String table) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public User single(int id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<User> all() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public User get(ResultSet result) throws SQLException {
        User userToReturn = new User(result.getString("email"));
        userToReturn.setPassword(result.getString("password"));
        userToReturn.setFirstName(result.getString("first_name"));
        userToReturn.setFamilyName(result.getString("family_name"));
        userToReturn.setAddress(result.getString("address"));
        userToReturn.setPhone(result.getString("phone"));
        userToReturn.setAffiliation(result.getString("affiliation"));
        userToReturn.setOrcid(result.getString("orcid"));
        userToReturn.setAdmin(result.getString("isadmin"));
        userToReturn.setAvailable(result.getString("available"));
        // Arnaud Charleroy add URI 
        userToReturn.setUri(result.getString("uri"));

        return userToReturn;
    }

    /**
     * Gets all the users emails from the storage.
     * @return the list of the users
     */
    public ArrayList<User> getAllUsersEmails() {
        ResultSet queryResult = null;
        Connection connection = null;
        Statement statement = null;

        ArrayList<User> users = new ArrayList<>();
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            SQLQueryBuilder query = new SQLQueryBuilder();

            Map<String, String> sqlFields = relationFieldsJavaSQLObject();
            
            //Query database
            query.appendFrom(table, tableAlias);
            query.appendSelect(sqlFields.get("email"));
            query.appendLimit(String.valueOf(pageSize));
            query.appendOffset(Integer.toString(this.getPage()* this.getPageSize()));

            queryResult = statement.executeQuery(query.toString());

            //Manipulates result
            while (queryResult.next()) {
                User u = new User(queryResult.getString("email"));
                users.add(u);
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(Level.SEVERE, null, ex);
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
                java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return users;
    }
    
    @Override
    public ArrayList<User> allPaginate() {
        ResultSet queryResult = null;
        Connection connection = null;
        Statement statement = null;

        ArrayList<User> users = new ArrayList<>();

        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            SQLQueryBuilder query = new SQLQueryBuilder();

            Map<String, String> sqlFields = relationFieldsJavaSQLObject();

            //Ajout des conditions à la requête
            query.appendFrom(table, tableAlias);
            query.appendANDWhereConditionIfNeeded(sqlFields.get("email"), email, "ILIKE", null, tableAlias);
            query.appendANDWhereConditionIfNeeded(sqlFields.get("firstName"), firstName, "ILIKE", null, tableAlias);
            query.appendANDWhereConditionIfNeeded(sqlFields.get("familyName"), familyName, "ILIKE", null, tableAlias);
            query.appendANDWhereConditionIfNeeded(sqlFields.get("address"), address, "ILIKE", null, tableAlias);
            query.appendANDWhereConditionIfNeeded(sqlFields.get("phone"), phone, "ILIKE", null, tableAlias);
            query.appendANDWhereConditionIfNeeded(sqlFields.get("affiliation"), affiliation, "ILIKE", null, tableAlias);
            query.appendANDWhereConditionIfNeeded(sqlFields.get("orcid"), orcid, "ILIKE", null, tableAlias);
            if (admin != null) {
                query.appendANDWhereConditionIfNeeded(sqlFields.get("admin"), String.valueOf(admin), "=", null, tableAlias);
            }
            if (available != null) {
                query.appendANDWhereConditionIfNeeded(sqlFields.get("available"), String.valueOf(available), "=", null, tableAlias);
            }
            if (uri != null) {
                query.appendANDWhereConditionIfNeeded(sqlFields.get("uri"), String.valueOf(uri), "=", null, tableAlias);
            }
            query.appendLimit(String.valueOf(pageSize));
            query.appendOffset(Integer.toString(this.getPage() * this.getPageSize()));

            queryResult = statement.executeQuery(query.toString());

            while (queryResult.next()) {
                users.add(get(queryResult));
            }

            for (User u : users) {
                u.setGroupList(this.getUserGroups(u));
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(Level.SEVERE, null, ex);
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
                java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return users;
    }

    @Override
    protected User compareAndMergeObjects(User fromDB, User object) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param user
     * @return the groups to which the user belongs
     * @throws SQLException
     */
    public ArrayList<Group> getUserGroups(User user) throws SQLException {
        ResultSet result = null;
        ResultSet resultDefaultGroup = null;
        Connection connection = null;
        Statement statement = null;
        Statement statementDefaultGroup = null;
        ArrayList<Group> userGroups = new ArrayList<>();

        try {
            if (this.existInDB(user)) {
                //Récupération des groupes dans la table at_group_users
                SQLQueryBuilder query = new SQLQueryBuilder();
                query.appendSelect("gp.uri, gp.level, gp.name");
                query.appendFrom("at_group_users", "gu");
                query.appendANDWhereConditions("users_email", user.getEmail(), "=", null, null);
                query.appendJoin(JoinAttributes.INNERJOIN, "group", "gp", "gu.group_uri = gp.uri");

                connection = dataSource.getConnection();
                statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);

                result = statement.executeQuery(query.toString());
                while (result.next()) {
                    Group gp = new Group();
                    gp.setUri(result.getString("uri"));
                    gp.setLevel(result.getString("level"));
                    gp.setName(result.getString("name"));
                    userGroups.add(gp);
                }
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(Level.SEVERE, null, ex);
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
            if (resultDefaultGroup != null) {
                resultDefaultGroup.close();
            }
            if (statementDefaultGroup != null) {
                statementDefaultGroup.close();
            }
        }

        return userGroups;
    }

    @Override
    protected SQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private POSTResultsReturn checkAndInsertUserList(List<UserDTO> newUsers) throws SQLException, Exception {
        //init result returned maps
        List<Status> insertStatusList = new ArrayList<>();
        boolean dataState = true;
        boolean resultState = true;
        boolean insertionState = true;
        POSTResultsReturn results = null;
        ArrayList<User> users = new ArrayList<>();

        for (UserDTO userDTO : newUsers) {
            User u = userDTO.createObjectFromDTO();
            users.add(u);
        }

        if (dataState) {
            PreparedStatement insertPreparedStatementUser = null;
            PreparedStatement insertPreparedStatementAtGroupUsers = null;

            final String insertGab = "INSERT INTO \"users\""
                    + "(\"email\", \"password\", \"first_name\", \"family_name\", \"address\","
                    + "\"phone\", \"affiliation\", \"orcid\", \"isadmin\", \"uri\")"
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, cast(? as boolean), ?)";

            final String insertGabAtUserGroup = "INSERT INTO \"at_group_users\" "
                    + "(\"users_email\", \"group_uri\")"
                    + " VALUES (?, ?)";

            Connection connection = null;
            int inserted = 0;
            int exists = 0;

            try {
                //batch
                boolean insertionLeft = true;
                int count = 0;

                // connection + transaction preparation
                connection = dataSource.getConnection();
                connection.setAutoCommit(false);

                insertPreparedStatementUser = connection.prepareStatement(insertGab);
                insertPreparedStatementAtGroupUsers = connection.prepareStatement(insertGabAtUserGroup);

                for (User u : users) {
                    if (!existInDB(u)) {
                        insertionLeft = true;
                        insertPreparedStatementUser.setString(1, u.getEmail());
                        insertPreparedStatementUser.setString(2, u.getPassword());
                        insertPreparedStatementUser.setString(3, u.getFirstName());
                        insertPreparedStatementUser.setString(4, u.getFamilyName());
                        insertPreparedStatementUser.setString(5, u.getAddress());
                        insertPreparedStatementUser.setString(6, u.getPhone());
                        insertPreparedStatementUser.setString(7, u.getAffiliation());
                        insertPreparedStatementUser.setString(8, u.getOrcid());
                        if (u.getAdmin() != null) {
                            insertPreparedStatementUser.setString(9, u.getAdmin());
                        } else {
                            insertPreparedStatementUser.setString(9, "f");
                        }
                        
                        if (u.getOrcid() != null) {
                            u.setUri(u.getOrcid());
                        } else {
                            // create uri suffix
                            String userUriSuffix = ResourcesUtils.createUserUriSuffix(u.getFirstName(), u.getFamilyName());
                            // set uri to agent
                            u.setUri(UriGenerator.generateNewInstanceUri(Foaf.CONCEPT_AGENT.toString(), null, userUriSuffix));
                        }
                        insertPreparedStatementUser.setString(10, u.getUri());

                        // Traceability
                        String log = "";
                        if (remoteUserAdress != null) {
                            log += "IP Addess " + remoteUserAdress + " - ";
                        }
                        if (user != null) {
                            log += "User : " + user.getEmail() + " - ";
                        }

                        LOGGER.debug(insertPreparedStatementUser.toString());
                        insertPreparedStatementUser.execute();

                        // Add the links to the groups to which the user belongs
                        for (Group group : u.getGroups()) {
                            insertPreparedStatementAtGroupUsers.setString(1, u.getEmail());
                            insertPreparedStatementAtGroupUsers.setString(2, group.getUri());
                            LOGGER.trace(log + " query : " + insertPreparedStatementAtGroupUsers.toString());
                            insertPreparedStatementAtGroupUsers.execute();
                        }

                        inserted++;
                    } else {
                        exists++;
                    }

                    // Insertion by batch
                    if (++count % batchSize == 0) {
                        insertPreparedStatementUser.executeBatch();
                        insertPreparedStatementAtGroupUsers.executeBatch();
                        insertionLeft = false;
                    }
                }

                if (insertionLeft) {
                    insertPreparedStatementUser.executeBatch(); // checkAndInsert remaining records
                    insertPreparedStatementAtGroupUsers.executeBatch();
                }
                connection.commit(); //Envoi des données dans la BD

                /**
                 * //SILEX:todo
                 * Tests to review
                 */
                
                // If data exists
                if (exists > 0 && inserted > 0) {
                    results = new POSTResultsReturn(resultState, insertionState, dataState);
                    insertStatusList.add(new Status("Already existing data", StatusCodeMsg.INFO, "All users already exist"));
                    results.setHttpStatus(Response.Status.OK);
                    results.statusList = insertStatusList;
                } else {
                    if (exists > 0) { // iIf data exists and not inserted
                        insertStatusList.add(new Status("Already existing data", StatusCodeMsg.INFO, String.valueOf(exists) + " user already exists"));
                    } else { // If data non existant and therefor inserted
                        insertStatusList.add(new Status("Data inserted", StatusCodeMsg.INFO, String.valueOf(inserted) + " users inserted"));
                    }
                }
                /**
                 * //\SILEX:todo
                 */
                results = new POSTResultsReturn(resultState, insertionState, dataState);

            } catch (SQLException e) {
                LOGGER.error(e.getMessage(), e);

                //Rollback
                if (connection != null) {
                    connection.rollback();
                }

                results = new POSTResultsReturn(false, insertionState, dataState);
                insertStatusList.add(new Status("Error", StatusCodeMsg.POSTGRESQL_ERROR, e.getMessage()));
                if (e.getNextException() != null) {
                    insertStatusList.add(new Status("Error", StatusCodeMsg.POSTGRESQL_ERROR, e.getNextException().getMessage()));
                    insertStatusList.add(new Status("Error", StatusCodeMsg.ERR, "Duplicated user in json or in database"));
                }
                results.statusList = insertStatusList;
            } finally {
                if (insertPreparedStatementUser != null) {
                    insertPreparedStatementUser.close();
                }
                if (insertPreparedStatementAtGroupUsers != null) {
                    insertPreparedStatementAtGroupUsers.close();
                }
                if (connection != null) {
                    connection.close();
                }
            }
        } else {
            results = new POSTResultsReturn(resultState, insertionState, dataState);
            results.statusList = insertStatusList;
        }

        return results;
    }

    @Override
    public POSTResultsReturn checkAndInsertList(List<UserDTO> newObjects) {
        POSTResultsReturn postResult;
        try {
            postResult = this.checkAndInsertUserList(newObjects);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            postResult = new POSTResultsReturn(false, Response.Status.INTERNAL_SERVER_ERROR, e.toString());
        }
        return postResult;
    }

    @Override
    public POSTResultsReturn checkAndInsert(UserDTO newObject) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Checks objects integrity and updates in the storage.
     * @param updatedUsers
     * @return result
     */
    private POSTResultsReturn checkAndUpdateUserList(List<UserDTO> updatedUsers) throws SQLException, Exception {
        //init result returned maps
        List<Status> insertStatusList = new ArrayList<>();
        boolean allUsersAlreadyInDB = true;
        POSTResultsReturn results = null;

        ArrayList<User> users = new ArrayList<>();

        String log = getTraceabilityLogs();

        //1. Get users and check they exist in the storage
        if (updatedUsers != null && !updatedUsers.isEmpty()) {
            for (UserDTO userDTO : updatedUsers) {
                User u = userDTO.createObjectFromDTO();
                if (existInDB(u)) {
                    users.add(u);
                } else {
                    allUsersAlreadyInDB = false;
                    insertStatusList.add(new Status("Data error", StatusCodeMsg.ERR, "Unknown user"));
                }
            }
        }

        //2. Update the database if all users exist
        if (allUsersAlreadyInDB) {
            PreparedStatement updatePreparedStatementUser = null;
            PreparedStatement deletePreparedStatementUserGroup = null;
            PreparedStatement insertPreparedStatementUserGroup = null;
            PreparedStatement updatePreparedStatementUserPassword = null;
            Connection connection = null;

            final String updateGabUser = "UPDATE \"users\" SET \"first_name\" = ?, \"family_name\" = ?, \"address\" = ?,"
                    + " \"available\" = cast(? as boolean), \"phone\" = ?, "
                    + "\"affiliation\" = ?, \"isadmin\" = cast(? as boolean), \"orcid\" = ? "
                    + " WHERE \"email\" = ?";
            final String deleteGabUserGroup = "DELETE FROM \"at_group_users\" WHERE \"users_email\" = ?";
            final String insertGabUserGroup = "INSERT INTO \"at_group_users\" (\"group_uri\", \"users_email\")"
                    + " VALUES(?, ?)";

            final String updateGabUSerPassword = "UPDATE \"users\" SET \"password\"=  ? WHERE \"email\" = ?";
            try {
                // Batch
                int count = 0;
                boolean insertionLeft = true;

                // connection + prepare transaction
                connection = dataSource.getConnection();
                connection.setAutoCommit(false);

                updatePreparedStatementUser = connection.prepareStatement(updateGabUser);
                deletePreparedStatementUserGroup = connection.prepareStatement(deleteGabUserGroup);
                insertPreparedStatementUserGroup = connection.prepareStatement(insertGabUserGroup);
                updatePreparedStatementUserPassword = connection.prepareStatement(updateGabUSerPassword);

                for (User u : users) {
                    //Update des données de la table user
                    updatePreparedStatementUser.setString(1, u.getFirstName());
                    updatePreparedStatementUser.setString(2, u.getFamilyName());
                    updatePreparedStatementUser.setString(3, u.getAddress());
                    if (u.getAvailable() != null) {
                        updatePreparedStatementUser.setString(4, u.getAvailable());
                    } else {
                        updatePreparedStatementUser.setString(4, "f");
                    }
                    updatePreparedStatementUser.setString(5, u.getPhone());
                    updatePreparedStatementUser.setString(6, u.getAffiliation());
                    if (u.getAdmin() != null) {
                        updatePreparedStatementUser.setString(7, u.getAdmin());
                    } else {
                        updatePreparedStatementUser.setString(7, "f");
                    }
                    updatePreparedStatementUser.setString(8, u.getOrcid());
                    updatePreparedStatementUser.setString(9, u.getEmail());
                    updatePreparedStatementUser.execute();
                    LOGGER.debug(log + " query : " + updatePreparedStatementUser.toString());
                    // Delete  user / group links
                    deletePreparedStatementUserGroup.setString(1, u.getEmail());
                    deletePreparedStatementUserGroup.execute();
                    LOGGER.debug(log + " query : " + deletePreparedStatementUserGroup.toString());

                    if (u.getPassword() != null && !u.getPassword().equals("")) {
                        updatePreparedStatementUserPassword.setString(1, u.getPassword());
                        updatePreparedStatementUserPassword.setString(2, u.getEmail());
                        updatePreparedStatementUserPassword.execute();
                        LOGGER.debug(log + " query : " + updatePreparedStatementUserPassword.toString());
                    }

                    // Insert new users / email links
                    if (u.getGroups() != null && !u.getGroups().isEmpty()) {
                        for (Group group : u.getGroups()) {
                            insertPreparedStatementUserGroup.setString(1, group.getUri());
                            insertPreparedStatementUserGroup.setString(2, u.getEmail());
                            insertPreparedStatementUserGroup.execute();
                            LOGGER.trace(log + " quert : " + insertPreparedStatementUserGroup.toString());
                        }
                    }

                    // Insertion by batch
                    if (++count % batchSize == 0) {
                        updatePreparedStatementUser.executeBatch();
                        deletePreparedStatementUserGroup.executeBatch();
                        insertPreparedStatementUserGroup.executeBatch();
                        updatePreparedStatementUserPassword.executeBatch();
                        insertionLeft = false;
                    }
                }

                if (insertionLeft) {
                    updatePreparedStatementUser.executeBatch();
                    deletePreparedStatementUserGroup.executeBatch();
                    insertPreparedStatementUserGroup.executeBatch();
                    updatePreparedStatementUserPassword.executeBatch();
                }

                connection.commit();

                insertStatusList.add(new Status("Data updated", StatusCodeMsg.INFO, "users updated"));
                results = new POSTResultsReturn(true, true, allUsersAlreadyInDB);
            } catch (SQLException e) {
                LOGGER.error(e.getMessage(), e);

                //Rollback
                if (connection != null) {
                    connection.rollback();
                }

                results = new POSTResultsReturn(false, true, allUsersAlreadyInDB);
                insertStatusList.add(new Status("Error", StatusCodeMsg.POSTGRESQL_ERROR, e.getMessage()));
                if (e.getNextException() != null) {
                    insertStatusList.add(new Status("Error", StatusCodeMsg.POSTGRESQL_ERROR, e.getNextException().getMessage()));
                }
                results.statusList = insertStatusList;
            } finally {
                if (updatePreparedStatementUser != null) {
                    updatePreparedStatementUser.close();
                }
                if (deletePreparedStatementUserGroup != null) {
                    deletePreparedStatementUserGroup.close();
                }
                if (insertPreparedStatementUserGroup != null) {
                    insertPreparedStatementUserGroup.close();
                }
                if (updatePreparedStatementUserPassword != null) {
                    updatePreparedStatementUserPassword.close();
                }
                if (connection != null) {
                    connection.close();
                }
            }

        } else { // Some users are not in the database
            results = new POSTResultsReturn(true, true, allUsersAlreadyInDB);
            results.statusList = insertStatusList;
        }

        return results;
    }

    @Override
    public POSTResultsReturn checkAndUpdateList(List<UserDTO> newObjects) {
        POSTResultsReturn postResult;
        try {
            postResult = this.checkAndUpdateUserList(newObjects);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            postResult = new POSTResultsReturn(false, Response.Status.INTERNAL_SERVER_ERROR, e.toString());
        }
        return postResult;
    }

    @Override
    public List<User> create(List<User> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<User> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<User> update(List<User> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public User findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<User> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
