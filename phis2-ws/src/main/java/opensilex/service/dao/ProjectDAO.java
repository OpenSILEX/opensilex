//******************************************************************************
//                                ProjectDAO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: March 2017
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
import opensilex.service.ontology.Oeso;
import opensilex.service.resource.dto.project.ProjectDTO;
import opensilex.service.resource.dto.project.ProjectPostDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sql.JoinAttributes;
import opensilex.service.utils.sql.SQLQueryBuilder;
import opensilex.service.view.brapi.Status;
import opensilex.service.model.Contact;
import opensilex.service.model.Project;

/**
 * Project DAO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ProjectDAO extends PhisDAO<Project, ProjectDTO> {

    final static Logger LOGGER = LoggerFactory.getLogger(ProjectDAO.class);
    
    public String uri;
    public String name;
    public String acronyme;
    public String subprojectType;
    public String financialSupport;
    public String financialName;
    public String dateStart;
    public String dateEnd;
    public String keywords;
    public String description;
    public String objective;
    public String parentProject;
    public String website;
    
    public ProjectDAO() {
        super();
        setTable("project");
        setTableAlias("p");
    }
    
    public ProjectDAO(String projectURI) {
        super();
        this.uri = projectURI;
        setTable("project");
        setTableAlias("p");
    }
    
    @Override
    public POSTResultsReturn checkAndInsert(ProjectDTO newObject) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private POSTResultsReturn checkAndInsertProjectList(List<ProjectPostDTO> newProjects) throws SQLException, Exception {
        //init resuts returned maps
        List<Status> insertStatusList = new ArrayList<>();
        boolean dataState = true;
        boolean resultState = true;
        boolean insertionState = true;
        POSTResultsReturn results = null;
        ArrayList<Project> projects = new ArrayList<>();
        ArrayList<String> createdResourcesURIs = new ArrayList<>();
        
        for (ProjectPostDTO projectDTO : newProjects) {
            Project project = projectDTO.createObjectFromDTO();
            projects.add(project);
        }
        if (dataState) {
            PreparedStatement insertPreparedStatementProject = null;
            PreparedStatement insertPreparedStatementProjectContact = null; 
            
            final String insertGabProject = "INSERT INTO \"project\""
                                   + "(\"uri\", \"name\", \"acronyme\", \"subproject_type\", \"financial_support\","
                                   + "\"financial_name\", \"date_start\", \"date_end\", \"keywords\", \"description\","
                                   + "\"objective\", \"parent_project\","
                                   + " \"website\")"
                                   + " VALUES (?, ?, ?, ?, ?, ?, to_date(?, 'YYYY:MM:DD'), to_date(?, 'YYYY:MM:DD'), ?, ?, ?, ?, ?)";
            final String insertGabProjectContact = "INSERT INTO \"at_project_users\""
                    + "(\"project_uri\", \"users_email\", \"type\")"
                    + " VALUES (?, ?, ?)";
            
            Connection connection = null;
            int inserted = 0;
            int exists = 0;
            
            try {
                //batch
                boolean insertionLeft = true;
                int count = 0;

                //connection + transaction preparation
                connection = dataSource.getConnection();
                connection.setAutoCommit(false);

                insertPreparedStatementProject = connection.prepareStatement(insertGabProject);
                insertPreparedStatementProjectContact = connection.prepareStatement(insertGabProjectContact);
                UriGenerator uriGenerator = new UriGenerator();
                
                for (Project project : projects) {
                    try {
                        // Generate project URI
                        project.setUri(uriGenerator.generateNewInstanceUri(Oeso.CONCEPT_PROJECT.toString(), null, project.getAcronyme()));
                        
                        insertionLeft = true;
                        insertPreparedStatementProject.setString(1, project.getUri());
                        insertPreparedStatementProject.setString(2, project.getName());
                        insertPreparedStatementProject.setString(3, project.getAcronyme());
                        insertPreparedStatementProject.setString(4, project.getSubprojectType());
                        insertPreparedStatementProject.setString(5, project.getFinancialSupport());
                        insertPreparedStatementProject.setString(6, project.getFinancialName());
                        insertPreparedStatementProject.setString(7, project.getDateStart());
                        insertPreparedStatementProject.setString(8, project.getDateEnd());
                        insertPreparedStatementProject.setString(9, project.getKeywords());
                        insertPreparedStatementProject.setString(10, project.getDescription());
                        insertPreparedStatementProject.setString(11, project.getObjective());
                        insertPreparedStatementProject.setString(12, project.getParentProject());
                        insertPreparedStatementProject.setString(13, project.getWebsite());

                        // Traceability
                        String log = "";
                        if (remoteUserAdress != null) {
                            log += "IP Addess " + remoteUserAdress + " - ";
                        }
                        if (user != null) {
                            log += "User : " + user.getEmail() + " - ";
                        }

                        insertPreparedStatementProject.execute();
                        createdResourcesURIs.add(project.getUri());
                        LOGGER.debug(log + " quert : " + insertPreparedStatementProject.toString());

                        for (Contact contact : project.getContacts()) {
                            insertPreparedStatementProjectContact.setString(1, project.getUri());
                            insertPreparedStatementProjectContact.setString(2, contact.getEmail());
                            insertPreparedStatementProjectContact.setString(3, contact.getType());
                            insertPreparedStatementProjectContact.execute();
                            LOGGER.debug(log + " quert : " + insertPreparedStatementProjectContact.toString());
                        }
                        inserted++;
                    } catch (AlreadyExists ex) {
                    // AlreadyExists throwed by the UriGenerator if the project uri generated already exists
                        exists++;
                        insertStatusList.add(new Status (StatusCodeMsg.ALREADY_EXISTING_DATA, StatusCodeMsg.INFO, project.getAcronyme()+ " already exists"));
                    }
                    
                    // Insertion by batch
                    if (++count % batchSize == 0) {
                        insertPreparedStatementProject.executeBatch();
                        insertPreparedStatementProjectContact.executeBatch();
                        insertionLeft = false;
                    }
                }
                
                if (insertionLeft) {
                    insertPreparedStatementProject.executeBatch(); // checkAndInsert remaining records
                    insertPreparedStatementProjectContact.executeBatch();
                }
                
                connection.commit(); //Envoi des données dans la BD
                
                /**
                 * //SILEX:todo
                 *  Tests to review
                 */
                
                // data inserted and existing
                if (exists > 0 && inserted > 0) {
                    results = new POSTResultsReturn(resultState, insertionState, dataState);
                    results.setHttpStatus(Response.Status.CREATED);
                    results.statusList = insertStatusList;
                } else {
                    results = new POSTResultsReturn(resultState, insertionState, dataState);
                    if (exists > 0) { // if existing and inserted data
                        results.setHttpStatus(Response.Status.CONFLICT); //409 
                    } else { // if non existing data and therefor inserted
                        insertStatusList.add(new Status("Data inserted", StatusCodeMsg.INFO, String.valueOf(inserted) + " projects inserted"));
                    }
                }  
                /**
                 * //\SILEX:todo
                 */
                
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
                    insertStatusList.add(new Status("Error", StatusCodeMsg.ERR, "Duplicated project in json or in database"));
                }
                results.statusList = insertStatusList;
            } finally {
                if (insertPreparedStatementProject != null) {
                    insertPreparedStatementProject.close();
                }
                if (insertPreparedStatementProjectContact != null) {
                    insertPreparedStatementProjectContact.close();
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
    

    /**
     * Checks projects and creates them.
     * @param newObjects list des projets
     * @return
     */
    public POSTResultsReturn checkAndInsert(List<ProjectPostDTO> newObjects) {
        POSTResultsReturn postResult;
        try {
            postResult = this.checkAndInsertProjectList(newObjects);
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
        createSQLFields.put("acronyme", "acronyme");
        createSQLFields.put("subprojectType", "subproject_type");
        createSQLFields.put("financialSupport", "financial_support");
        createSQLFields.put("financialName", "financial_name");
        createSQLFields.put("dateStart", "date_start");
        createSQLFields.put("dateEnd", "date_end");
        createSQLFields.put("keywords", "keywords");
        createSQLFields.put("description", "description");
        createSQLFields.put("objective", "objective");
        createSQLFields.put("parentProject", "parent_project");
        createSQLFields.put("website", "website");
        createSQLFields.put("type", "type");
        
        return createSQLFields;
    }

    @Override
    public Project findByFields(Map<String, Object> Attr, String table) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Project single(int id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<Project> all() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Project get(ResultSet result) throws SQLException {
        Project project = new Project();
        project.setUri(result.getString("uri"));
        project.setName(result.getString("name"));
        project.setAcronyme(result.getString("acronyme"));
        project.setSubprojectType(result.getString("subproject_type"));
        project.setFinancialSupport(result.getString("financial_support"));
        project.setFinancialName(result.getString("financial_name"));
        project.setDateStart(result.getString("date_start"));
        project.setDateEnd(result.getString("date_end"));
        project.setKeywords(result.getString("keywords"));
        project.setDescription(result.getString("description"));
        project.setObjective(result.getString("objective"));
        project.setParentProject(result.getString("parent_project"));
        project.setWebsite(result.getString("website"));
        
        return project;
    }

    /**
     * @param projects ArrayList<Project> projects from which the contacts are requested
     * @param statement
     * @return the projects containing their contacts
     * @throws SQLException
     */
    private ArrayList<Project> getProjectsContacts(ArrayList<Project> projects, Statement statement) throws SQLException{
        for (Project project : projects) {
            SQLQueryBuilder query = new SQLQueryBuilder();
            query.appendSelect("u.email, u.first_name, u.family_name, pu.type");
            query.appendFrom("at_project_users", "pu");
            query.appendANDWhereConditionIfNeeded("project_uri", project.getUri(), "=", null, "pu");
            query.appendJoin(JoinAttributes.INNERJOIN, "users", "u", "u.email = pu.users_email");
            
            ResultSet queryResult = statement.executeQuery(query.toString());
            while (queryResult.next()) {
                Contact contact = new Contact();
                contact.setEmail(queryResult.getString("email"));
                contact.setFirstName(queryResult.getString("first_name"));
                contact.setFamilyName(queryResult.getString("family_name"));
                contact.setType(queryResult.getString("type"));
                project.addContact(contact);
            }
        }
        
        return projects;
    }
    
    @Override
    public ArrayList<Project> allPaginate() {
        ResultSet queryResult = null;
        Connection connection = null;
        Statement statement = null;
        ArrayList<Project> projects = new ArrayList<>();
        
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            SQLQueryBuilder query = new SQLQueryBuilder();
            
            // Add request filters
            query.appendFrom(table, tableAlias);
            addFilters(query);
            
            query.appendLimit(String.valueOf(pageSize));
            query.appendOffset(Integer.toString(this.getPage() * this.getPageSize()));
            
            queryResult = statement.executeQuery(query.toString());
            
            while (queryResult.next()) {
                projects.add(get(queryResult));
            }
            
            projects = getProjectsContacts(projects, statement);
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(ProjectDAO.class.getName()).log(Level.SEVERE, null, ex);
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
                java.util.logging.Logger.getLogger(ProjectDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return projects;
    }

    /**
     * Adds filter for query project search.
     * @param query 
     */
    private void addFilters(SQLQueryBuilder query) {
        Map<String, String> sqlFields = relationFieldsJavaSQLObject();
                
        query.appendANDWhereConditionIfNeeded(sqlFields.get("uri"), uri, "ILIKE", null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("name"), name, SQLQueryBuilder.CONTAINS_OPERATOR, null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("acronyme"), acronyme, SQLQueryBuilder.CONTAINS_OPERATOR, null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("subprojectType"), subprojectType, SQLQueryBuilder.CONTAINS_OPERATOR, null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("financialSupport"), financialSupport, SQLQueryBuilder.CONTAINS_OPERATOR, null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("financialName"), financialName, SQLQueryBuilder.CONTAINS_OPERATOR, null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("dateStart"), dateStart, ">=", null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("dateEnd"), dateEnd, "<=", null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("keywords"), keywords, SQLQueryBuilder.CONTAINS_OPERATOR, null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("description"), description, SQLQueryBuilder.CONTAINS_OPERATOR, null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("objective"), objective, SQLQueryBuilder.CONTAINS_OPERATOR, null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("parentProject"), parentProject, SQLQueryBuilder.CONTAINS_OPERATOR, null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("website"), website, "ILIKE", null, tableAlias);    
    }
    
    @Override
    public Integer count() {
        SQLQueryBuilder query = new SQLQueryBuilder();
        query.appendCount();
        query.appendDistinct();
        query.appendSelect(tableAlias + ".uri");
        
        //Add request filters
        query.appendFrom(table, tableAlias);
        addFilters(query);
            
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
    protected Project compareAndMergeObjects(Project fromDB, Project object) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected SQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private POSTResultsReturn checkAndUpdateProjectList(List<ProjectDTO> updateProjects) throws SQLException, Exception {
        // init result returned maps
        List<Status> insertStatusList = new ArrayList<>();
        boolean allProjectsAlreadyInDB = true;
        POSTResultsReturn results = null;
        
        ArrayList<Project> projects = new ArrayList<>();
        
        String log = getTraceabilityLogs();
        
        // 1. Get projects and check they exist
        if (updateProjects != null && !updateProjects.isEmpty()) {
            for (ProjectDTO projectDTO : updateProjects) {
                Project project = projectDTO.createObjectFromDTO();
                if (existInDB(project)) {
                    projects.add(project);
                } else {
                    allProjectsAlreadyInDB = false;
                    insertStatusList.add(new Status("Data error", StatusCodeMsg.ERR, "Unknown project"));
                }
            }
        }
        
        //2. Update the database with the data sent
        if (allProjectsAlreadyInDB) { //Si tous les projets sont présents en BD on peut continuer
            PreparedStatement updatePreparedStatementProject = null;
            PreparedStatement deletePreparedStatementContacts = null;
            PreparedStatement insertPreparedStatementContacts = null;
            Connection connection = null;
            
            final String updateProject = "UPDATE \"project\" SET \"name\" = ?, \"subproject_type\" = ?, "
                    + "\"financial_support\" = ?, \"financial_name\" = ?, \"date_start\" = to_date(?, 'YYYY:MM:DD'), \"date_end\" = to_date(?, 'YYYY:MM:DD'),"
                    + "\"keywords\" = ?, \"description\" = ?, \"objective\" = ?, \"parent_project\" = ?,"
                    + "\"website\" = ? "
                    + "WHERE \"uri\" = ?";
            final String deleteContacts = "DELETE FROM \"at_project_users\" WHERE \"project_uri\" = ?";
            final String insertContact = "INSERT INTO \"at_project_users\" (\"project_uri\", \"users_email\", \"type\") "
                    + "VALUES (?, ?, ?)";
            try {
                // Batch
                int count = 0;
                boolean insertionLeft = true;

                // Connection + transaction preparation
                connection = dataSource.getConnection();
                connection.setAutoCommit(false);

                updatePreparedStatementProject = connection.prepareStatement(updateProject);
                deletePreparedStatementContacts = connection.prepareStatement(deleteContacts);
                insertPreparedStatementContacts = connection.prepareStatement(insertContact);
                
                for (Project project : projects) {
                    //Update data in project table
                    updatePreparedStatementProject.setString(1, project.getName());
                    updatePreparedStatementProject.setString(2, project.getSubprojectType());
                    updatePreparedStatementProject.setString(3, project.getFinancialSupport());
                    updatePreparedStatementProject.setString(4, project.getFinancialName());
                    updatePreparedStatementProject.setString(5, project.getDateStart());
                    updatePreparedStatementProject.setString(6, project.getDateEnd());
                    updatePreparedStatementProject.setString(7, project.getKeywords());
                    updatePreparedStatementProject.setString(8, project.getDescription());
                    updatePreparedStatementProject.setString(9, project.getObjective());
                    updatePreparedStatementProject.setString(10, project.getParentProject());
                    updatePreparedStatementProject.setString(11, project.getWebsite());
                    updatePreparedStatementProject.setString(12, project.getUri());
                    LOGGER.trace(log + " quert : " + updatePreparedStatementProject.toString());
                    updatePreparedStatementProject.execute();
                    
                    // Delete contacts 
                    deletePreparedStatementContacts.setString(1, project.getUri());
                    deletePreparedStatementContacts.execute();
                    LOGGER.trace(log + " quert : " + deletePreparedStatementContacts.toString());
                    
                    // Insert contacts
                    if (project.getContacts() != null && !project.getContacts().isEmpty()) {
                        for (Contact contact : project.getContacts()) {
                            insertPreparedStatementContacts.setString(1, project.getUri());
                            insertPreparedStatementContacts.setString(2, contact.getEmail());
                            insertPreparedStatementContacts.setString(3, contact.getType());
                            insertPreparedStatementContacts.execute();
                            LOGGER.trace(log + " quert : " + insertPreparedStatementContacts.toString());
                        }
                    }
                    
                    // Insertion by batch
                    if (++count % batchSize == 0) {
                        updatePreparedStatementProject.executeBatch();
                        deletePreparedStatementContacts.executeBatch();
                        insertPreparedStatementContacts.executeBatch();
                        insertionLeft = false;
                    }
                }
                
                if (insertionLeft) {
                    updatePreparedStatementProject.executeBatch();
                    deletePreparedStatementContacts.executeBatch();
                    insertPreparedStatementContacts.executeBatch();
                }
                
                connection.commit();
                
                insertStatusList.add(new Status("Data updated", StatusCodeMsg.INFO, "projects updated"));
                results = new POSTResultsReturn(true, true, allProjectsAlreadyInDB);
                
            } catch (SQLException e) {
                LOGGER.error(e.getMessage(), e);

                // Rollback
                if (connection != null) {
                    connection.rollback();
                }

                results = new POSTResultsReturn(false, true, allProjectsAlreadyInDB);
                insertStatusList.add(new Status("Error", StatusCodeMsg.POSTGRESQL_ERROR, e.getMessage()));
                if (e.getNextException() != null) {
                    insertStatusList.add(new Status("Error", StatusCodeMsg.POSTGRESQL_ERROR, e.getNextException().getMessage()));
                }
                results.statusList = insertStatusList;
            } finally {
                if (updatePreparedStatementProject != null) {
                    updatePreparedStatementProject.close();
                }
                if (deletePreparedStatementContacts != null) {
                    deletePreparedStatementContacts.close();
                }
                if (insertPreparedStatementContacts != null) {
                    insertPreparedStatementContacts.close();
                }
                if (connection != null) {
                    connection.close();
                }
            }
        } else { // Some projects doesn't exist in the database
            results = new POSTResultsReturn(true, true, allProjectsAlreadyInDB);
            results.statusList = insertStatusList;
        }
        
        return results;
    }

    @Override
    public POSTResultsReturn checkAndUpdateList(List<ProjectDTO> newObjects) {
        POSTResultsReturn postResult;
        try {
            postResult = this.checkAndUpdateProjectList(newObjects);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            postResult = new POSTResultsReturn(false, Response.Status.INTERNAL_SERVER_ERROR, e.toString());
        }
        return postResult;
    }

    @Override
    public POSTResultsReturn checkAndInsertList(List<ProjectDTO> newObjects) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Project> create(List<Project> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Project> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Project> update(List<Project> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Project findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Project> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
