//******************************************************************************
//                            ExperimentSQLDAO.java 
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: Jan. 2017
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.dao.manager.PhisDAO;
import opensilex.service.view.brapi.Status;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.User;
import opensilex.service.ontology.Oeso;
import opensilex.service.resource.AcquisitionSessionResourceService;
import opensilex.service.resource.dto.experiment.ExperimentDTO;
import opensilex.service.resource.dto.experiment.ExperimentPostDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sql.JoinAttributes;
import opensilex.service.utils.sql.SQLQueryBuilder;
import opensilex.service.model.Contact;
import opensilex.service.model.Group;
import opensilex.service.model.Project;
import opensilex.service.model.Experiment;

/**
 * Experiment DAO for a relational database. 
 * @update [Andreas Garcia] 14 Feb. 2019: update the method that returns the 
 * total number of experiment by making it return the last experiment URI 
 * because the experiment URI generator now use the last inserted experiment
 * number (instead of total number of experiment) to calculate a new 
 * experiment's number.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ExperimentSQLDAO extends PhisDAO<Experiment, ExperimentDTO> {

    final static Logger LOGGER = LoggerFactory.getLogger(ExperimentSQLDAO.class);
    
    //Search parameters
     
    /** 
     * The URI of an experiment.
     * @example http://www.phenome-fppn.fr/diaphen/DIA2017-1
     */
    public String uri;
    
    /**
     * The project URI related to an experiment.
     */
    public String projectUri;
    
    /**
     * The start date of an experiment
     * @example 2018-01-15
     */
    public String startDate;
    
    /**
     * The end date of an experiment
     * @example 2018-08-02
     */
    public String endDate;
    
    /**
     * The field of an experiment
     * @example field003
     */
    public String field;
    
    /**
     * The campaign of an experiment
     * @example 2017
     */
    public String campaign;
    
    /**
     * The place of an experiment
     * @example Madone
     */
    public String place;
    
    /**
     * The alias of the experiment
     * @example exp-16-03
     */
    public String alias;
    
    /**
     * The keywords of the experiment
     * @example maize, phenotyping
     */
    public String keyword;
    
    /**
     * The groups URIs that can access an experiment
     * @example http://www.phenome-fppn.fr/diaphen/DROPS,http://www.phenome-fppn.fr/diaphen/MISTEAGAMMA
     */
    public String groups;
    
    /**
     * The crop species of an experiment
     * @example Maize, Wheat
     */
    public String cropSpecies;
    
    public ExperimentSQLDAO() {
        super();
        setTable("trial");
        setTableAlias("t");
    }
    
    public ExperimentSQLDAO(String uri) {
        super();
        this.uri = uri;
        setTable("trial");
        setTableAlias("t");
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
        createSQLFields.put("uri","uri");
        createSQLFields.put("startDate", "start_date");
        createSQLFields.put("endDate", "end_date");
        createSQLFields.put("field", "field");
        createSQLFields.put("campaign", "campaign");
        createSQLFields.put("place", "place");
        createSQLFields.put("alias", "alias");
        createSQLFields.put("comment", "comment");
        createSQLFields.put("keywords", "keywords");
        createSQLFields.put("objective", "objective");
        createSQLFields.put("groups", "groups");
        createSQLFields.put("cropSpecies", "crop_species");

        return createSQLFields;
    }

    @Override
    public Experiment findByFields(Map<String, Object> Attr, String table) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Experiment single(int id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<Experiment> all() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Experiment get(ResultSet result) throws SQLException {
        Experiment experiment = new Experiment();
        experiment.setUri(result.getString("uri"));
        experiment.setStartDate(result.getString("start_date"));
        experiment.setEndDate(result.getString("end_date"));
        experiment.setField(result.getString("field"));
        experiment.setCampaign(result.getString("campaign"));
        experiment.setPlace(result.getString("place"));
        experiment.setAlias(result.getString("alias"));
        experiment.setComment(result.getString("comment"));
        experiment.setKeywords(result.getString("keywords"));
        experiment.setObjective(result.getString("objective"));
        experiment.setCropSpecies(result.getString("crop_species"));

        return experiment;
    }
    
    /**
     * Gets the list of the experiments, with only the minimal information 
     * required to generate the lists for 4P acquisition session files. 
     * Returned information for each experiment: alias, URI, species
     * @see AcquisitionSessionResourceService
     * @return the list of the experiments founded in the database
     */
    public ArrayList<Experiment> getAllExperimentsForAcquisitionSessionFile() {
        ResultSet queryResult = null;
        Connection connection = null;
        Statement statement = null;
        ArrayList<Experiment> experiments = new ArrayList();
        
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            SQLQueryBuilder query = new SQLQueryBuilder();
            
            // Queries database            
            Map<String, String> sqlFields = relationFieldsJavaSQLObject();
           
            query.appendFrom(table, tableAlias);
            query.appendSelect(sqlFields.get("uri") + ", " + sqlFields.get("alias") + ", " + sqlFields.get("cropSpecies"));
            query.appendLimit(String.valueOf(pageSize));
            query.appendOffset(Integer.toString(this.getPage()* this.getPageSize()));
            
            queryResult = statement.executeQuery(query.toString());
            
            // Manipulates database results
            while (queryResult.next()) {
                Experiment experiment = new Experiment();
                experiment.setUri(queryResult.getString(sqlFields.get("uri")));
                experiment.setAlias(queryResult.getString(sqlFields.get("alias")));
                experiment.setCropSpecies(queryResult.getString(sqlFields.get("cropSpecies")));
                experiments.add(experiment);
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(ExperimentSQLDAO.class.getName()).log(Level.SEVERE, null, ex);
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
        return experiments;
    }

    @Override
    public ArrayList<Experiment> allPaginate() {
        ResultSet queryResult = null;
        Connection connection = null;
        Statement statement = null;
        ArrayList<Experiment> experiments = new ArrayList();
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            SQLQueryBuilder query = new SQLQueryBuilder();
            
            // Adds conditions in query
            query.appendFrom(table, tableAlias);
            addFilters(query);
            
            query.appendLimit(String.valueOf(pageSize));
            query.appendOffset(Integer.toString(this.getPage() * this.getPageSize()));
            
            queryResult = statement.executeQuery(query.toString());
            
            LOGGER.debug (query.toString());
            
            UserDAO userDao = new UserDAO();
            userDao.isAdmin(user);
            boolean isAdmin = (user.getAdmin().equals("t") || user.getAdmin().equals("true"));

            while (queryResult.next()) {
                //SILEX:access
                Experiment experiment = get(queryResult);
                if (isAdmin || canUserSeeExperiment(user, experiment)) {
                    experiments.add(get(queryResult));
                }
                //\SILEX:access
            }
            
            //SILEX:dbjoin
            experiments = getExperimentsContacts(experiments, statement);
            //\SILEX:dbjoin
            experiments = getExperimentsProjects(experiments, statement);
            for (Experiment experiment : experiments) {
                experiment.setGroupList(this.getExperimentGroups(experiment));
            }
            
            // Gets experiments variables
            ExperimentRdf4jDAO experimentRdf4jDAO = new ExperimentRdf4jDAO();
            for (Experiment experiment : experiments) {
                HashMap<String, String> variables = experimentRdf4jDAO.getVariables(experiment.getUri());
                experiment.setVariables(variables);
                HashMap<String, String> sensors = experimentRdf4jDAO.getSensors(experiment.getUri());
                experiment.setSensors(sensors);
            }
            
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(ExperimentSQLDAO.class.getName()).log(Level.SEVERE, null, ex);
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
        return experiments;
    }
    
    /**
     * Gets experiments projects.
     * @param experiments ArrayList<Experiment> list of experiments for which 
     * the list of projects is also needed
     * @param statement Statement
     * @return the list given in parameter with the project list for each
     * experiment
     * @throws SQLException 
     */
    private ArrayList<Experiment> getExperimentsProjects(ArrayList<Experiment> experiments, Statement statement) 
            throws SQLException {
        for (Experiment experiment : experiments) {
            SQLQueryBuilder query = new SQLQueryBuilder();
            query.appendSelect("p.acronyme, p.uri");
            query.appendFrom("at_trial_project", "tp");
            query.appendANDWhereConditionIfNeeded("trial_uri", experiment.getUri(), "=", null, "tp");
            query.appendJoin(JoinAttributes.INNERJOIN, "project", "p", "p.uri = tp.project_uri");
            
            ResultSet queryResult = statement.executeQuery(query.toString());
            while (queryResult.next()) {
                Project project = new Project(queryResult.getString("uri"));
                project.setAcronyme(queryResult.getString("acronyme"));
                experiment.addProject(project);
            }
        }
        return experiments;
    }
    
    /**
     * Gets the contacts of an experiment.
     * @param statement Statement
     * @param experiments ArrayList<Experiment> experiment list
     * @return the experiment list with the contact list for each experiment
     */
    private ArrayList<Experiment> getExperimentsContacts(ArrayList<Experiment> experiments, Statement statement) 
            throws SQLException {
        for (Experiment experiment : experiments) {
            SQLQueryBuilder query = new SQLQueryBuilder();
            query.appendSelect("u.email, u.first_name, u.family_name, tu.type");
            query.appendFrom("at_trial_users", "tu");
            query.appendANDWhereConditionIfNeeded("trial_uri", experiment.getUri(), "=", null, "tu");
            query.appendJoin(JoinAttributes.INNERJOIN, "users", "u", "u.email = tu.users_email");
            
            LOGGER.debug(query.toString());
            
            ResultSet queryResult = statement.executeQuery(query.toString());
            while (queryResult.next()) {
                Contact contact = new Contact();
                contact.setEmail(queryResult.getString("email"));
                contact.setFirstName(queryResult.getString("first_name"));
                contact.setFamilyName(queryResult.getString("family_name"));
                contact.setType(queryResult.getString("type"));
                experiment.addContact(contact);
            }
        }
        
        return experiments;
    }
    
    /**
     * Adds filter for query experiment search.
     * @param query 
     */
    private void addFilters(SQLQueryBuilder query) {
        Map<String, String> sqlFields = relationFieldsJavaSQLObject();
        query.appendANDWhereConditionIfNeeded(sqlFields.get("uri"), uri, "ILIKE", null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("startDate"), startDate, ">=", null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("endDate"), endDate, "<=", null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("field"), field, SQLQueryBuilder.CONTAINS_OPERATOR, null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("campaign"), campaign, SQLQueryBuilder.CONTAINS_OPERATOR, null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("place"), place, SQLQueryBuilder.CONTAINS_OPERATOR, null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("alias"), alias, SQLQueryBuilder.CONTAINS_OPERATOR, null, tableAlias);
        query.appendANDWhereConditionIfNeeded(sqlFields.get("keywords"), keyword, SQLQueryBuilder.CONTAINS_OPERATOR, null, tableAlias);
                    
        if (projectUri != null) {
            query.appendJoin("LEFT JOIN", "at_trial_project", "attp", tableAlias + ".uri = attp.trial_uri");
            query.appendANDWhereConditionIfNeeded("project_uri", projectUri, "ILIKE", null, "attp");
        }
    }
    
    @Override
    public Integer count() {
        SQLQueryBuilder query = new SQLQueryBuilder();
        query.appendCount();
        query.appendDistinct();
        query.appendSelect(tableAlias + ".uri");
        query.appendFrom(table, tableAlias);

        if (uri != null) {
            query.appendWhereConditions("uri", uri, "=", null, tableAlias);
        }
        
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
    protected Experiment compareAndMergeObjects(Experiment fromDB, Experiment object) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected SQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private POSTResultsReturn checkAndInsertExperimentList(List<ExperimentPostDTO> newExperiments) throws SQLException, Exception {
        // inits results returned maps
        List<Status> insertStatusList = new ArrayList<>();
        boolean dataState = true;
        boolean resultState = true;
        boolean insertionState = true;
        POSTResultsReturn results = null;
        ArrayList<Experiment> experiments = new ArrayList<>();
        ArrayList<String> createdResourcesURIs = new ArrayList<>();
        
        for (ExperimentPostDTO experimentDTO : newExperiments) {
            Experiment experiment = experimentDTO.createObjectFromDTO();
            experiments.add(experiment);
        }
        
        if (dataState) {
            PreparedStatement insertPreparedStatementExperiments = null;
            PreparedStatement insertPreparedStatementAtExperimentProject = null;
            PreparedStatement insertPreparedStatementAtGroupExperiment = null;
            PreparedStatement insertPreparedStatementAtExperimentUsers = null;
            
            final String insertGabExperiment = "INSERT INTO \"trial\""
                + "(\"uri\", \"start_date\", \"end_date\", \"field\","
                + "\"campaign\", \"place\", \"alias\", \"comment\", \"keywords\","
                + " \"objective\", \"crop_species\")"
                + " VALUES (?, to_date(?, 'YYYY-MM-DD'), to_date(?, 'YYYY-MM-DD'), ?, ?, ?, ?, ?, ?, ?, ?)";
            
            final String insertGabAtExperimentProject = "INSERT INTO \"at_trial_project\""
                + "(\"project_uri\", \"trial_uri\")"
                + " VALUES (?, ?)";
            
            final String insertGabAtExperimentGroup = "INSERT INTO \"at_group_trial\""
                + "(\"group_uri\", \"trial_uri\")"
                + " VALUES (?, ?)";
            final String insertGabAtExperimentUsers = "INSERT INTO \"at_trial_users\""
                + "(\"trial_uri\", \"users_email\", \"type\")"
                + " VALUES (?, ?, ?)";
            
            Connection con = null;
            int inserted = 0;
            int exists = 0;
            
            try {
                // batch
                boolean insertionLeft = true;
                int count = 0;
                
                // connection + transaction preparation
                con = dataSource.getConnection();
                con.setAutoCommit(false);
                
                insertPreparedStatementExperiments = con.prepareStatement(insertGabExperiment);
                insertPreparedStatementAtExperimentProject = con.prepareStatement(insertGabAtExperimentProject);
                insertPreparedStatementAtGroupExperiment = con.prepareStatement(insertGabAtExperimentGroup);
                insertPreparedStatementAtExperimentUsers = con.prepareStatement(insertGabAtExperimentUsers);
                
                UriGenerator uriGenerator = new UriGenerator();
                
                for (Experiment experiment : experiments) {
                    if (!existInDB(experiment)) {
                        insertionLeft = true;
                        experiment.setUri(uriGenerator.generateNewInstanceUri(Oeso.CONCEPT_EXPERIMENT.toString(), experiment.getCampaign(), null));
                        insertPreparedStatementExperiments.setString(1, experiment.getUri());
                        insertPreparedStatementExperiments.setString(2, experiment.getStartDate());
                        insertPreparedStatementExperiments.setString(3, experiment.getEndDate()); 
                        insertPreparedStatementExperiments.setString(4, experiment.getField());
                        insertPreparedStatementExperiments.setString(5, experiment.getCampaign());
                        insertPreparedStatementExperiments.setString(6, experiment.getPlace());
                        insertPreparedStatementExperiments.setString(7, experiment.getAlias());
                        insertPreparedStatementExperiments.setString(8, experiment.getComment());
                        insertPreparedStatementExperiments.setString(9, experiment.getKeywords());
                        insertPreparedStatementExperiments.setString(10, experiment.getObjective());
                        insertPreparedStatementExperiments.setString(11, experiment.getCropSpecies());
                        
                        // Logs operating user with its IP
                        String log = "";
                        if (remoteUserAdress != null) {
                            log += "IP Address " + remoteUserAdress + " - ";
                        }
                        if (user != null) {
                            log += "User : " + user.getEmail() + " - ";
                        }
                       
                        LOGGER.debug(log + " query : " + insertPreparedStatementExperiments.toString());
                        insertPreparedStatementExperiments.execute();
                        createdResourcesURIs.add(experiment.getUri());
                        
                        // Inserts the trial project link
                        for (Project project : experiment.getProjects()) {
                            insertPreparedStatementAtExperimentProject.setString(1, project.getUri());
                            insertPreparedStatementAtExperimentProject.setString(2, experiment.getUri());
                            LOGGER.debug(log + " query : " + insertPreparedStatementAtExperimentProject.toString());
                            insertPreparedStatementAtExperimentProject.execute();
                        }
                        
                        // Inserts the trial group link
                        for (Group group : experiment.getGroups()) {
                            insertPreparedStatementAtGroupExperiment.setString(1, group.getUri());
                            insertPreparedStatementAtGroupExperiment.setString(2, experiment.getUri());
                            LOGGER.debug(log + " query : " + insertPreparedStatementAtExperimentProject.toString());
                            insertPreparedStatementAtGroupExperiment.execute();
                        }
                        
                        // Inserts link between the trial and the contact user
                        for (Contact contact : experiment.getContacts()) {
                            insertPreparedStatementAtExperimentUsers.setString(1, experiment.getUri());
                            insertPreparedStatementAtExperimentUsers.setString(2, contact.getEmail());
                            insertPreparedStatementAtExperimentUsers.setString(3, contact.getType());
                            insertPreparedStatementAtExperimentUsers.execute();
                            LOGGER.debug(log + " query : " + insertPreparedStatementAtExperimentUsers.toString());
                        }
                        
                        inserted++;
                    } else {
                        exists++;
                    }
                    
                    // Batch insertion
                    if (++count % batchSize == 0) {
                        insertPreparedStatementExperiments.executeBatch();
                        insertPreparedStatementAtExperimentProject.executeBatch();
                        insertPreparedStatementAtGroupExperiment.executeBatch();
                        insertPreparedStatementAtExperimentUsers.executeBatch();
                        insertionLeft = false;
                    }
                }
                
                if (insertionLeft) {
                    insertPreparedStatementExperiments.executeBatch(); //checkAndInsert remaining records
                    insertPreparedStatementAtExperimentProject.executeBatch();
                    insertPreparedStatementAtGroupExperiment.executeBatch();
                    insertPreparedStatementAtExperimentUsers.executeBatch();
                }
                
                con.commit(); //Envoi des données ds bd
                
                // WARNING, checking to re-check
                // If data inserted and existing
                if (exists > 0 && inserted > 0) {
                    results = new POSTResultsReturn(resultState, insertionState, dataState);
                    insertStatusList.add(new Status("Already existing data", StatusCodeMsg.INFO, "All experiments already exist"));
                    results.setHttpStatus(Response.Status.OK);
                    results.statusList = insertStatusList;
                } else {
                    if (exists > 0) { // If existing data and non onserted
                        insertStatusList.add(new Status ("Already existing data", StatusCodeMsg.INFO, String.valueOf(exists) + " experiment already exists"));
                    } else { // If non existing data and inserted
                        insertStatusList.add(new Status("Data inserted", StatusCodeMsg.INFO, String.valueOf(inserted) + " experiments inserted"));
                        //Add the experiments in the triplestore
                        ExperimentRdf4jDAO experimentRdf4jDao = new ExperimentRdf4jDAO();
                        POSTResultsReturn insertTriplestore = experimentRdf4jDao.insertExperiments(experiments);
                        if (!insertTriplestore.getDataState()) { //An error occurred
                            insertStatusList.addAll(insertTriplestore.getStatusList());
                            insertionState = false;
                        }
                    }
                }
                results = new POSTResultsReturn(resultState, insertionState, dataState);
                results.createdResources = createdResourcesURIs;
                
            } catch (SQLException e) {
                LOGGER.error(e.getMessage(), e);
                
                // Rollback
                if (con != null) {
                    con.rollback();
                }
                
                results = new POSTResultsReturn(false, insertionState, dataState);
                insertStatusList.add(new Status("Error", StatusCodeMsg.POSTGRESQL_ERROR, e.getMessage()));
                if (e.getNextException() != null) {
                    insertStatusList.add(new Status("Error", StatusCodeMsg.POSTGRESQL_ERROR, e.getNextException().getMessage()));
                    insertStatusList.add(new Status("Error", StatusCodeMsg.ERR, "Duplicated experiment in json or in database"));
                }
                results.statusList = insertStatusList;
            } finally {
                if (insertPreparedStatementExperiments != null) {
                    insertPreparedStatementExperiments.close();
                }
                if (insertPreparedStatementAtGroupExperiment != null) {
                    insertPreparedStatementAtGroupExperiment.close();
                }
                if (insertPreparedStatementAtExperimentProject != null) {
                    insertPreparedStatementAtExperimentProject.close();
                }
                if (insertPreparedStatementAtExperimentUsers != null) {
                    insertPreparedStatementAtExperimentUsers.close();
                }
                if (con != null) {
                    con.close();
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
     * Checks experiments and stores them.
     * @param newObjects experiment list
     * @return
     */
    public POSTResultsReturn checkAndInsertExperimentsList(List<ExperimentPostDTO> newObjects) {
        
         POSTResultsReturn postResult;
        try {
            postResult = this.checkAndInsertExperimentList(newObjects);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            postResult = new POSTResultsReturn(false, Response.Status.INTERNAL_SERVER_ERROR, e.toString());
        }
        
        return postResult;
    }
    

    @Override
    public POSTResultsReturn checkAndInsert(ExperimentDTO newObject) {
        throw new UnsupportedOperationException("Not supported yet.");
    }  
    
    /**
     * @param experiment Experiment
     * @return the group list having access to the experiment (groups names list)
     * @throws java.sql.SQLException
     */
    public ArrayList<Group> getExperimentGroups(Experiment experiment) throws SQLException {
        ResultSet result = null;
        Connection connection = null;
        Statement statement = null;
        ArrayList<Group> experimentGroups = new ArrayList<>();
        
        try {
            if (this.existInDB(experiment)) {
                // Get groups from at_group_trial and group tables 
                SQLQueryBuilder query = new SQLQueryBuilder();
                query.appendSelect("gp.uri, gp.level, gp.name");
                query.appendFrom("at_group_trial", "gt");
                query.appendANDWhereConditionIfNeeded("trial_uri", experiment.getUri(), "=", null, "gt");
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
            java.util.logging.Logger.getLogger(ExperimentSQLDAO.class.getName()).log(Level.SEVERE, null, ex);
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
     * Checks if a user can see an experiment.
     * @param user 
     * @param experiment
     * @return true if the user belongs to a group having access to the 
     * experiment.
     *         false otherwise.
     */
    public boolean canUserSeeExperiment(User user, Experiment experiment) {
        try {
            UserDAO userDao = new UserDAO();

            ArrayList<Group> experimentGroups = this.getExperimentGroups(experiment);
            
            // when the trial isn't in any group, it is public so the user can consult it
            if (experimentGroups.isEmpty()) { 
                return true;
            } else {
                ArrayList<Group> userGroups = userDao.getUserGroups(user);
                for (Group userGroup : userGroups) {
                    for (Group experimentGroup : experimentGroups) {
                        if (userGroup.getUri().equals(experimentGroup.getUri())) {
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(ExperimentSQLDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return false;
    }
    
    /**
     * @param experiment
     * @return true if the user can modify the experiment (admin or 
     * member of a data owner group)
     *         false otherwise
     * @throws SQLException 
     */
    private boolean canUserUpdateExperiment(Experiment experiment) throws SQLException {
        UserDAO userDao = new UserDAO();
        
        if (userDao.isAdmin(user)) {
            return true;
        }
        
        ArrayList<Group> userGroups = userDao.getUserGroups(user);
        ArrayList<Group> experimentGroups = this.getExperimentGroups(experiment);
        
        if (!experimentGroups.isEmpty() || !userGroups.isEmpty()) {
            for (Group userGroup : userGroups) {
                for (Group experimentGroup : experimentGroups) {
                    if ( userGroup.getLevel().equals("Owner")
                            && userGroup.getUri().equals(experimentGroup.getUri())) {

                        return true;
                    }
                }
            }
        }
        return false;                
    }
    
    /**
     * @param updateExperiments List<ExperimentDTO> experiments to modify
     * @return POSTResultReturn, updates result
     */
    private POSTResultsReturn checkAndUpdateExperimentList(List<ExperimentDTO> updateExperiments) 
            throws SQLException, Exception {
        //init result returned maps
        List<Status> insertStatusList = new ArrayList<>();
        boolean allExperimentsAlreadyInDB = true;
        POSTResultsReturn results = null;
        
        ArrayList<Experiment> experiments = new ArrayList<>();
        
        String log = getTraceabilityLogs();
        
        //1. gets experiments list and check that they exist in storage and that
        //   the user has the right to modify them
        if (updateExperiments != null && !updateExperiments.isEmpty()) {
            for (ExperimentDTO experimentDTO : updateExperiments) {
                Experiment experiment = experimentDTO.createObjectFromDTO();
                if (existInDB(experiment) && canUserUpdateExperiment(experiment)) {
                    experiments.add(experiment);
                } else {
                    allExperimentsAlreadyInDB = false;
                    insertStatusList.add(new Status("Data error", StatusCodeMsg.ERR, "Unknown experiment"));
                }
            }
        }
        
        //2. Storage modification accorgin to the data sent
        if (allExperimentsAlreadyInDB) {
            PreparedStatement updatePreparedStatementExperiment = null;
            PreparedStatement deletePreparedStatementProject = null;
            PreparedStatement insertPreparedStatementProject = null;
            PreparedStatement deletePreparedStatementGroup = null;
            PreparedStatement insertPreparedStatementGroup = null;
            PreparedStatement deletePreparedStatementContact = null;
            PreparedStatement insertPreparedStatementContact = null;
            Connection connection = null;
            
            final String updateExperiment = "UPDATE \"trial\" "
                    + "SET \"start_date\" = to_date(?, 'YYYY:MM:DD'), \"end_date\" = to_date(?, 'YYYY:MM:DD'),"
                    + " \"field\" = ?,"
                    + "\"place\" = ?, \"alias\" = ?, \"comment\" = ?, \"keywords\" = ?, \"objective\" = ?,"
                    + "\"crop_species\" = ? "
                    + "WHERE \"uri\" = ?";
            final String deleteProject = "DELETE FROM \"at_trial_project\" WHERE \"trial_uri\" = ?";
            final String insertProject = "INSERT INTO \"at_trial_project\" (\"trial_uri\", \"project_uri\") VALUES (?, ?)";
            final String deleteGroup = "DELETE FROM \"at_group_trial\" WHERE \"trial_uri\" = ?";
            final String insertGroup = "INSERT INTO \"at_group_trial\" (\"trial_uri\", \"group_uri\") VALUES (?, ?)";
            final String deleteContact = "DELETE FROM \"at_trial_users\" WHERE \"trial_uri\" = ?";
            final String insertContact = "INSERT INTO \"at_trial_users\" (\"trial_uri\", \"users_email\", \"type\") VALUES (?, ?, ?)";
            
            try {
                //Batch
                int count = 0;
                boolean insertionLeft = true;
                
                //Connection + transaction preparation
                connection = dataSource.getConnection();
                connection.setAutoCommit(false);
                
                updatePreparedStatementExperiment = connection.prepareStatement(updateExperiment);
                deletePreparedStatementProject = connection.prepareStatement(deleteProject);
                insertPreparedStatementProject = connection.prepareStatement(insertProject);
                deletePreparedStatementGroup = connection.prepareStatement(deleteGroup);
                insertPreparedStatementGroup = connection.prepareStatement(insertGroup);
                deletePreparedStatementContact = connection.prepareStatement(deleteContact);
                insertPreparedStatementContact = connection.prepareStatement(insertContact);
                
                //Update of experiments
                for (Experiment experiment : experiments) {
                    updatePreparedStatementExperiment.setString(1, experiment.getStartDate());
                    updatePreparedStatementExperiment.setString(2, experiment.getEndDate());
                    updatePreparedStatementExperiment.setString(3, experiment.getField());
                    updatePreparedStatementExperiment.setString(4, experiment.getPlace());
                    updatePreparedStatementExperiment.setString(5, experiment.getAlias());
                    updatePreparedStatementExperiment.setString(6, experiment.getComment());
                    updatePreparedStatementExperiment.setString(7, experiment.getKeywords());
                    updatePreparedStatementExperiment.setString(8, experiment.getObjective());
                    updatePreparedStatementExperiment.setString(9, experiment.getCropSpecies());
                    updatePreparedStatementExperiment.setString(10, experiment.getUri());
                    LOGGER.debug(log + " query : " + updatePreparedStatementExperiment.toString());
                    updatePreparedStatementExperiment.execute();
                    
                    // Deletion of projects
                    deletePreparedStatementProject.setString(1, experiment.getUri());
                    deletePreparedStatementProject.execute();
                    LOGGER.debug(log + " query : " + deletePreparedStatementProject.toString());
                    
                    // Insertion of projects
                    if (experiment.getProjects() != null && !experiment.getProjects().isEmpty()) {
                        for (Project project : experiment.getProjects()) {
                            insertPreparedStatementProject.setString(1, experiment.getUri());
                            insertPreparedStatementProject.setString(2, project.getUri());
                            insertPreparedStatementProject.execute();
                            LOGGER.debug(log + " query : " + insertPreparedStatementProject);
                        }
                    }
                    
                    // Deletion of groups
                    deletePreparedStatementGroup.setString(1, experiment.getUri());
                    deletePreparedStatementGroup.execute();
                    LOGGER.debug(log + " query : " + deletePreparedStatementGroup.toString());
                    
                    // Insertion of groups
                    if (experiment.getGroups() != null && !experiment.getGroups().isEmpty()) {
                        for (Group group : experiment.getGroups()) {
                            insertPreparedStatementGroup.setString(1, experiment.getUri());
                            insertPreparedStatementGroup.setString(2, group.getUri());
                            insertPreparedStatementGroup.execute();
                            LOGGER.debug(log + " query : " + insertPreparedStatementGroup.toString());
                        }
                    }
                    
                    // Deletion of contacts
                    deletePreparedStatementContact.setString(1, experiment.getUri());
                    deletePreparedStatementContact.execute();
                    LOGGER.debug(log + " query : " + deletePreparedStatementContact.toString());
                    
                    // Insertion of contacts
                    if (experiment.getContacts() != null && !experiment.getContacts().isEmpty()) {
                        for (Contact contact : experiment.getContacts()) {
                            insertPreparedStatementContact.setString(1, experiment.getUri());
                            insertPreparedStatementContact.setString(2, contact.getEmail());
                            insertPreparedStatementContact.setString(3, contact.getType());
                            insertPreparedStatementContact.execute();
                            LOGGER.debug(log + " query : " + insertPreparedStatementContact.toString());
                        }
                    }
                    
                    //Insertion by batch
                    if (++count % batchSize == 0) {
                        updatePreparedStatementExperiment.executeBatch();
                        deletePreparedStatementProject.executeBatch();
                        insertPreparedStatementProject.executeBatch();
                        deletePreparedStatementGroup.executeBatch();
                        insertPreparedStatementGroup.executeBatch();
                        deletePreparedStatementContact.executeBatch();
                        insertPreparedStatementContact.executeBatch();
                        insertionLeft = false;
                    }
                 }
                if (insertionLeft) {
                    updatePreparedStatementExperiment.executeBatch();
                    deletePreparedStatementProject.executeBatch();
                    insertPreparedStatementProject.executeBatch();
                    deletePreparedStatementGroup.executeBatch();
                    insertPreparedStatementGroup.executeBatch();
                    deletePreparedStatementContact.executeBatch();
                    insertPreparedStatementContact.executeBatch();
                }
                
                connection.commit(); // Send data to storage
                
                insertStatusList.add(new Status("Data updated", StatusCodeMsg.INFO, "experiments updated"));
                results = new POSTResultsReturn(true, true, allExperimentsAlreadyInDB);
            } catch (SQLException e) {
                LOGGER.error(e.getMessage(), e);

                // Rollback
                if (connection != null) {
                    connection.rollback();
                }

                results = new POSTResultsReturn(false, true, allExperimentsAlreadyInDB);
                insertStatusList.add(new Status("Error", StatusCodeMsg.POSTGRESQL_ERROR, e.getMessage()));
                if (e.getNextException() != null) {
                    insertStatusList.add(new Status("Error", StatusCodeMsg.POSTGRESQL_ERROR, e.getNextException().getMessage()));
                }
                results.statusList = insertStatusList;
            } finally {
                if (updatePreparedStatementExperiment != null) {
                    updatePreparedStatementExperiment.close();
                }
                if (deletePreparedStatementProject != null) {
                    deletePreparedStatementProject.close();
                }
                if (insertPreparedStatementProject != null) {
                    insertPreparedStatementProject.close();
                }
                if (deletePreparedStatementGroup != null) {
                    deletePreparedStatementGroup.close();
                }
                if (insertPreparedStatementGroup != null) {
                    insertPreparedStatementGroup.close();
                }
                if (deletePreparedStatementContact != null) {
                    deletePreparedStatementContact.close();
                }
                if (insertPreparedStatementContact != null) {
                    insertPreparedStatementContact.close();
                }
                if (connection != null) {
                    connection.close();
                }
            }
        } else {
            results = new POSTResultsReturn(true, true, allExperimentsAlreadyInDB);
            results.statusList = insertStatusList;
        }
        return results;
    }

    @Override
    public POSTResultsReturn checkAndUpdateList(List<ExperimentDTO> newObjects) {
        POSTResultsReturn postResult;
        try {
            postResult = this.checkAndUpdateExperimentList(newObjects);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            postResult = new POSTResultsReturn(false, Response.Status.INTERNAL_SERVER_ERROR, e.toString());
        }
        return postResult;
    }
    
    /**
     * Checks the given information and right on experiment to update the list of 
     * observed variables which are measured during the given experiment.
     * @param experimentUri
     * @param variables
     * @return the check result.
     */
    public POSTResultsReturn checkLinkedVariables(String experimentUri, List<String> variables) {
        POSTResultsReturn checkResult;
        List<Status> checkStatus = new ArrayList<>();
        
        boolean dataOk = true;
        
        Experiment experiment = new Experiment(experimentUri);
        try {
            //1. Check if the experimentUri exist in the database.
            if (existInDB(experiment) && canUserUpdateExperiment(experiment)) {
                //2. Check if the user has the rigth to update the experiment
                if (canUserUpdateExperiment(experiment)) {
                    //3. Check for each variable uri given if it exist and is a variable.
                    VariableDAO variableDao = new VariableDAO();
                    for (String variableUri : variables) {
                        if (!variableDao.existAndIsVariable(variableUri)) {
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                                StatusCodeMsg.UNKNOWN_URI + " " + variableUri));
                        }
                    }
                } else {
                    dataOk = false;
                    checkStatus.add(new Status(StatusCodeMsg.ACCESS_DENIED, StatusCodeMsg.ERR, 
                        StatusCodeMsg.ACCESS_DENIED + ". You cannot update " + experimentUri));
                }
            } else {
                dataOk = false;
                checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                    StatusCodeMsg.UNKNOWN_URI + " " + experimentUri));
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(ExperimentSQLDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        checkResult = new POSTResultsReturn(dataOk, null, dataOk);
        checkResult.statusList = checkStatus;
        return checkResult;
    }
    
    /**
     * Checks the given information and rights on experiment to update the list of 
     * sensors which participates in the given experiment.
     * @param experimentUri
     * @param sensors
     * @return the check result.
     */
    public POSTResultsReturn checkLinkedSensors(String experimentUri, List<String> sensors) {
        POSTResultsReturn checkResult;
        List<Status> checkStatus = new ArrayList<>();
        
        boolean dataOk = true;
        
        Experiment experiment = new Experiment(experimentUri);
        try {
            //1. Check if the experimentUri exist in the database.
            if (existInDB(experiment) && canUserUpdateExperiment(experiment)) {
                //2. Check if the user has the rigth to update the experiment
                if (canUserUpdateExperiment(experiment)) {
                    //3. Check for each sensor uri given if it exist and is a sensor.
                    SensorDAO sensorDAO = new SensorDAO();
                    for (String sensorUri : sensors) {
                        if (!sensorDAO.existAndIsSensor(sensorUri)) {
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                                StatusCodeMsg.UNKNOWN_URI + " " + sensorUri));
                        }
                    }
                } else {
                    dataOk = false;
                    checkStatus.add(new Status(StatusCodeMsg.ACCESS_DENIED, StatusCodeMsg.ERR, 
                        StatusCodeMsg.ACCESS_DENIED + ". You cannot update " + experimentUri));
                }
            } else {
                dataOk = false;
                checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                    StatusCodeMsg.UNKNOWN_URI + " " + experimentUri));
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(ExperimentSQLDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        checkResult = new POSTResultsReturn(dataOk, null, dataOk);
        checkResult.statusList = checkStatus;
        return checkResult;
    }
    
    /**
     * Updates the list of variables linked to the given experiment. 
     * /!\ Prerequisite: the information must have been checked before. 
     * @see ExperimentSQLDAO#checkLinkedVariables(java.lang.String, java.util.List)
     * @see ExperimentRdf4jDAO#updateLinkedVariables(java.lang.String, java.util.List) 
     * @param experimentUri
     * @param variables
     * @return the update result.
     */
    private POSTResultsReturn updateLinkedVariables(String experimentUri, List<String> variables) {
        ExperimentRdf4jDAO experimentRdf4jDao = new ExperimentRdf4jDAO();
        return experimentRdf4jDao.updateLinkedVariables(experimentUri, variables);
    }
    
    /**
     * Updates the list of sensors linked to the given experiment. 
     * /!\ Prerequisite: the information must have been checked before. 
     * @see ExperimentSQLDAO#checkLinkedSensors(java.lang.String, java.util.List)
     * @see ExperimentRdf4jDAO#updateLinkedSensors(java.lang.String, java.util.List)
     * @param experimentUri
     * @param sensors
     * @return the update result.
     */
    private POSTResultsReturn updateLinkedSensors(String experimentUri, List<String> sensors) {
        ExperimentRdf4jDAO experimentRdf4jDao = new ExperimentRdf4jDAO();
        return experimentRdf4jDao.updateLinkedSensors(experimentUri, sensors);
    }
    
    /**
     * Checks and updates the variables observed in the given experiment.
     * @param experimentUri
     * @param variables
     * @return the update result.
     */
    public POSTResultsReturn checkAndUpdateLinkedVariables(String experimentUri, List<String> variables) {
        POSTResultsReturn checkResult = checkLinkedVariables(experimentUri, variables);
        if (checkResult.getDataState()) {
             return updateLinkedVariables(experimentUri, variables);
        } else { // Error in the data
            return checkResult;
        }
    }
    
    /**
     * Checks and updates the sensors linked to the given experiment.
     * @param experimentUri
     * @param sensors
     * @return the update result.
     */
    public POSTResultsReturn checkAndUpdateLinkedSensors(String experimentUri, List<String> sensors) {
       POSTResultsReturn checkResult = checkLinkedSensors(experimentUri, sensors);
        if (checkResult.getDataState()) {
             return updateLinkedSensors(experimentUri, sensors);
        } else { // Error in the data
            return checkResult;
        } 
    }

    @Override
    public POSTResultsReturn checkAndInsertList(List<ExperimentDTO> newObjects) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Gets the campaign last experiment URI.
     * @example
     *  SELECT uri, regexp_matches(uri, '.*2019-[0-9]{1,2}$') 
     *  FROM trial
     *  WHERE campaign = '2019';
     * @param campaign
     * @return the campaign last experiment URI
     */
    public Integer getCampaignLastExperimentUri(String campaign) {
        String query = "SELECT uri, regexp_matches(uri, '.*" + campaign + "-[0-9]{1,2}$') "
                     + "FROM trial "
                     + "WHERE campaign = '" + campaign + "';";
        
        Connection connection = null;
        ResultSet resultSet = null;
        Statement statement = null;

        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            LOGGER.debug("query: " + query);
            resultSet = statement.executeQuery(query);
            Integer maxExperimentNumber = 0;
            while(resultSet.next()) {
                String[] foundedUriSplit = resultSet.getString("uri").split("-");
                if (Integer.parseInt(foundedUriSplit[1]) > maxExperimentNumber) {
                    maxExperimentNumber = Integer.parseInt(foundedUriSplit[1]);
                }
            }
            
            return maxExperimentNumber;
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
    public List<Experiment> create(List<Experiment> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Experiment> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Experiment> update(List<Experiment> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Experiment findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Experiment> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
