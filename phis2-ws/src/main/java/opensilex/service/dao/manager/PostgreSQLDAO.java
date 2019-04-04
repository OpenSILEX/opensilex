//******************************************************************************
//                             PostgreSQLDAO.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: August 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.manager;

import com.nimbusds.jwt.JWTClaimsSet;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.postgis.Geometry;
import opensilex.service.authentication.TokenManager;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.datasource.PostgreSQLDataSource;
import opensilex.service.model.User;
import opensilex.service.utils.JsonConverter;
import opensilex.service.utils.sql.SQLQueryBuilder;

/**
 * DAO to query a PostgreSQL database.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 * @param <T>
 */
public abstract class PostgreSQLDAO<T> extends DAO<T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(PostgreSQLDAO.class);
    protected final static String DUPLICATE_KEY_ERROR_POSTGRE = "23505";

    static final Map<String, DataSource> JWT_ISSUER_DATASOURCE;
    protected static final String PHIS_MODEL_DB_LOCATION = "Phis";
    protected static final String GNPIS_MODEL_DB_LOCATION = "GnpIS";

    // To manage multiple database switch 
    static {
        Map<String, DataSource> tmpMap = new HashMap<>();
        tmpMap.put(PhisDAO.PHIS_MODEL_DB_LOCATION, PostgreSQLDataSource.getInstance());
        tmpMap.put(PhisDAO.GNPIS_MODEL_DB_LOCATION, PostgreSQLDataSource.getInstance());
        JWT_ISSUER_DATASOURCE = Collections.unmodifiableMap(tmpMap);
    }

    // For query logging
    protected static final String SQL_SELECT_QUERY = "SQL query : ";
    
    public User user;
    protected Integer page;
    protected Integer pageSize;

    public String remoteUserAdress;
    
    /**
     * DAO main table
     */
    protected String table;
    protected String tableAlias;
    protected DataSource dataSource;

    /**
     * For batch
     */
    protected final int batchSize = 1000;
    
    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getTableAlias() {
        return tableAlias;
    }

    public void setTableAlias(String alias) {
        this.tableAlias = alias;
    }

    /**
     * @return current page number
     * BrApi page starts at 0.
     */
    public Integer getPage() {
        if (page == null || pageSize < 0) {
            return 0;
        }
        return page;
    }

    /**
     * Gets BrAPI page for database querying.
     * @return current page + 1
     */
    public Integer getPageForDBQuery() {
        if (page == null || pageSize < 0) {
            return 1;
        }
        return page + 1;
    }

    /**
     * @param page
     */
    public void setPage(Integer page) {
        if (page < 0) {
            this.page = Integer.valueOf(DefaultBrapiPaginationValues.PAGE);
        }
        this.page = page;
    }

    /**
     * @return Integer taille de la page
     */
    public Integer getPageSize() {
        if (pageSize == null || pageSize < 0) {
            return Integer.valueOf(DefaultBrapiPaginationValues.PAGE_SIZE);
        }
        return pageSize;
    }

    /**
     * @param pageSize
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * @param id
     */
    public void setUser(String id) {
        if (TokenManager.Instance().getSession(id).getUser() == null) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        } else {
            this.user = TokenManager.Instance().getSession(id).getUser();
        }
    }

    public void setTable(String table) {
        this.table = table;
    }

    /**
     * Not practical the resources (statement and resultset) stay open. 
     * Execute a search request from a DAO linked to a relational database.
     * @param query
     * @return 
     * @deprecated
     * @throws SQLException
     */
    public ResultSet selectQueryFromDAO(String query) throws SQLException {
        Connection con;
        Statement stat;
        con = dataSource.getConnection();
        stat = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
        return stat.executeQuery(query);
    }

    /**
     * Runs a query from a DAO.
     * @param query
     * @return 
     * @throws SQLException
     */
    public Integer insertOrUpdateOrDeleteQueryFromDAO(String query) throws SQLException {
        Connection connection = dataSource.getConnection();
        Statement stat = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
        Integer executeUpdate = stat.executeUpdate(query);
        stat.close();
        connection.close();
        return executeUpdate;
    }

    /**
     * Prepares a SQL query using a string with the following format: 
     * "variable1, variable2".
     * @param variables
     * @param request
     * @param column column to which the variables will be added
     * @return The complete query string
     *
     */
    public static String formatMultipleValueQuery(String variables, String column, String request) {
        StringTokenizer st = new StringTokenizer(variables, ",");
        String result = "(";
        result = result + column + " = '" + st.nextToken() + "'";
        while (st.hasMoreTokens()) {
            result = result + " OR " + column + " = '" + st.nextToken() + "'";
        }
        result = result + ")";
        return request + " AND " + result;
    }

    /**
     * Constructs and execute the query the permits to know if a T type object 
     * exists in the database
     * @param objectToSearch
     * @return true if exists, false otherwise
     * @throws Exception
     */
    public boolean existInDB(T objectToSearch) throws Exception {
        String query = new StringBuilder("SELECT * ")
                .append("FROM ")
                .append("\"").append(table).append("\"")
                .append(" WHERE ")
                .append(makeFindSQLConditionQuery(objectToSearch, false)).toString();
        ResultSet rs = null;
        PreparedStatement statement = null;
        Connection con = null;

        LOGGER.debug(query);
        try {
            con = dataSource.getConnection();
            statement = con.prepareStatement(query);
            rs = statement.executeQuery();
            if (rs != null) {
                return rs.next();
            }
            return false;
        } catch (SQLException e) {
            LOGGER.error("SQL error Exist Request Method ", e);
            LOGGER.error(query);
            return false;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                LOGGER.error(ex.getMessage());
            }
        }
    }

    /**
     * Runs a query to find a T type object in a relational database.
     * @param objectToFind
     * @return the object found
     * @throws Exception
     */
    @Override
    public T find(T objectToFind) throws Exception {
        StringBuilder strSQLBuilder = new StringBuilder();
        // Requete SELECT préparée
        strSQLBuilder.append("SELECT * ")
                .append("FROM ")
                .append("\"").append(table).append("\"")
                .append(" WHERE ")
                .append(makeFindSQLConditionQuery(objectToFind, false));
        LOGGER.debug(strSQLBuilder.toString());
        Statement Statement = null;
        ResultSet rs = null;
        Connection con = null;
        Map<String, String> objectFields = relationFieldsJavaSQLObject();
        final Field[] attributes = objectToFind.getClass().getDeclaredFields();
        LOGGER.debug(JsonConverter.ConvertToJson(objectToFind));
        try {
            con = dataSource.getConnection();
            Statement = con.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE, 
                    ResultSet.CONCUR_READ_ONLY, 
                    ResultSet.HOLD_CURSORS_OVER_COMMIT);
            rs = Statement.executeQuery(strSQLBuilder.toString());
            if (rs != null && rs.first()) {
                for (Field field : attributes) {
                    field.setAccessible(true);
                    if (objectFields.containsKey(field.getName())
                            && rs.getObject(objectFields.get(field.getName())) != null) {
                        if (rs.getObject(objectFields.get(field.getName())) instanceof Date) {
                            if (field.getType() == String.class) {
                                LocalDate fromDateFields = LocalDate.fromDateFields((Date)rs.getObject(objectFields.get(field.getName())));
                                field.set(objectToFind, fromDateFields.toString("yyyy-MM-dd"));
                            } else if (field.getType() == DateTime.class) {
                                Timestamp ts = new Timestamp(((Date) rs.getObject(objectFields.get(field.getName()))).getTime());
                                field.set(objectToFind, new DateTime(ts));
                            }
                        } else if (rs.getObject(objectFields.get(field.getName())) instanceof Geometry) {
                            if (field.getType() == String.class) {
                                field.set(objectToFind, field.toString());
                            }
                        } else {
                            field.set(objectToFind, rs.getObject(objectFields.get(field.getName()).replaceAll("\"", "")).toString());
                        }
                    }
                }
            }
            return objectToFind;
        } catch (SQLException e) {
            LOGGER.error("SQL error Exist Request ", e);
            LOGGER.error(strSQLBuilder.toString());
            return null;
        } finally {
            if (Statement != null) {
                try {
                    Statement.close();
                } catch (SQLException ex) {
                    LOGGER.error(ex.getMessage());
                }
            }
            if (rs != null) {
                rs.close();
            }
            if (con != null) {
                con.close();
            }
        }
    }

    /**
     * Runs a query to insert a T type object in a relational database.
     * @param objectToInsert
     * @return the insertion result
     * @throws Exception
     */
    public Boolean create(T objectToInsert) throws Exception {
        String query = new StringBuilder("INSERT INTO ")
                .append("\"").append(table).append("\" ").toString();
        Connection con = null;
        PreparedStatement preparedStatement = null;
        try {
            con = dataSource.getConnection();
            preparedStatement = makeCreatePreparedSQLConditionQuery(con, query, objectToInsert);
            preparedStatement.executeUpdate();
            String log = "";
            if (remoteUserAdress != null) {
                log += " IP Adress : " + remoteUserAdress + " - ";
            }
            if (user != null) {
                log += "User : " + user.getEmail() + "-";
            }
            LOGGER.trace(log + " query : " + preparedStatement.toString());
            return true;
        } catch (SQLException e) {
            if (e.getSQLState().contains(DUPLICATE_KEY_ERROR_POSTGRE)) {
                return null;
            } else {
                LOGGER.error("SQL error Create Request " + e.getErrorCode() + e.getSQLState(), e);
                return false;
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (con != null) {
                con.close();
            }
        }
    }

    /**
     * HashMap describing the attributes corresponding to the primary key
     * of the object and their label in the database.
     * @return Map<String,String>
     */
    public abstract Map<String, String> pkeySQLFieldLink();

    /**
     * HashMap describing the attributes corresponding to the object and their 
     * label in the database.
     * @return Map<String,String>
     */
    public abstract Map<String, String> relationFieldsJavaSQLObject();

    /**
     * Creates a query to get an element in a database from its primary key.
     * @param obj
     * @param like
     * @return the query built
     */
    public String makeFindSQLConditionQuery(T obj, boolean like) {
        final Map<String, String> pkeyLink = this.pkeySQLFieldLink();
        StringBuilder strBuilder = new StringBuilder();
        List<Field> attributes = new ArrayList<>();
        for (Class<?> c = obj.getClass(); c != null; c = c.getSuperclass()) {
            attributes.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        try {
            for (Field field : attributes) {
                field.setAccessible(true);
                Object fieldObject = field.get(obj);
                if (pkeyLink.containsKey(field.getName()) && fieldObject != null) {
                    if (strBuilder.length() > 0) {
                        strBuilder.append(" AND ");
                    }
                    String sqlField = "\"" + pkeyLink.get(field.getName()) + "\"";
                    if (fieldObject instanceof DateTime) {
                        if (like) {
                            final DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
                            final String finalDate = fmt.print((DateTime) fieldObject);
                            strBuilder.append(sqlField).append("::text LIKE '").append(finalDate).append("%'");
                        } else {
                            final DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ");
                            final String finalDate = fmt.print((DateTime) fieldObject).substring(0, 22);
                            strBuilder.append(sqlField).append("='").append(finalDate).append("'");
                        }
                    } else if (like) {
                        strBuilder.append(sqlField).append(" LIKE '").append(fieldObject).append("%'");
                    } else {
                        strBuilder.append(sqlField).append("='").append(fieldObject).append("'");
                    }
                }
            }
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            LOGGER.error(ex.getMessage(), ex);
            return null;
        }
        return strBuilder.toString();
    }

    /**
     * Builds a query to insert an element in a database from its primary key.
     * @param objectToInsert
     * @return
     */
    public String makeCreateSQLConditionQuery(T objectToInsert) {
        final Map<String, String> createSQLField = this.relationFieldsJavaSQLObject();
        StringBuilder attributesBuilder = new StringBuilder();
        StringBuilder valuesBuilder = new StringBuilder();
        final Field[] attributes = objectToInsert.getClass().getDeclaredFields();
        try {
            for (Field field : attributes) {
                field.setAccessible(true);
                Object fieldObject = field.get(objectToInsert);
                if (createSQLField.containsKey(field.getName())) {
                    if (attributesBuilder.length() > 0) {
                        attributesBuilder.append(", ");
                    }
                    String sqlField = createSQLField.get(field.getName());
                    attributesBuilder.append(sqlField);

                    if (valuesBuilder.length() > 0) {
                        valuesBuilder.append(", ");
                    }
                    if (fieldObject == null) {
                        valuesBuilder.append("NULL");
                    } else if (fieldObject instanceof DateTime) {
                        final DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ");
                        final String finalDate = fmt.print((DateTime) fieldObject).substring(0, 22);
                        valuesBuilder.append("'").append(finalDate).append("'");
                    } else {
                        valuesBuilder.append("'").append(fieldObject).append("'");
                    }
                }
            }
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            LOGGER.error(ex.getMessage(), ex);
            return null;
        }
        final String createSQLValuesQuery = "(" + attributesBuilder.toString() + ")"
                + " VALUES (" + valuesBuilder.toString() + ")";
        return createSQLValuesQuery;
    }

    /**
     * Builds a query to insert an element in a database from its primary key.
     * @param connection
     * @param query
     * @param objectToInsert
     * @return
     */
    public PreparedStatement makeCreatePreparedSQLConditionQuery(Connection connection, String query, T objectToInsert) {
        PreparedStatement preparedStatement = null;
        final Map<String, String> createSQLField = this.relationFieldsJavaSQLObject();
        StringBuilder attributesBuilder = new StringBuilder();
        StringBuilder valuesBuilder = new StringBuilder();
        final Field[] attributes = objectToInsert.getClass().getDeclaredFields();

        try {
            for (Field field : attributes) {
                field.setAccessible(true);
                if (createSQLField.containsKey(field.getName())) {
                    if (attributesBuilder.length() > 0) {
                        attributesBuilder.append(", ");
                    }
                    String sqlField = createSQLField.get(field.getName());
                    attributesBuilder.append(sqlField);

                    if (valuesBuilder.length() > 0) {
                        valuesBuilder.append(", ");
                    }
                    valuesBuilder.append("?");
                }
            }
            final String createSQLValuesQuery = query + "(" + attributesBuilder.toString() + ")"
                    + " VALUES (" + valuesBuilder.toString() + ")";

            connection = dataSource.getConnection();
            preparedStatement = connection.prepareStatement(createSQLValuesQuery);
            int fieldCount = 1;
            for (Field field : attributes) {
                field.setAccessible(true);
                Object fieldObject = field.get(objectToInsert);
                if (createSQLField.containsKey(field.getName())) {
                    if (fieldObject == null) {
                        preparedStatement.setObject(fieldCount, null);
                    } else if (fieldObject instanceof DateTime) {
                        preparedStatement.setTimestamp(fieldCount, new Timestamp(((DateTime) fieldObject).getMillis()));
                    } else {
                        preparedStatement.setObject(fieldCount, fieldObject);
                    }
                    fieldCount++;
                }

            }
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            LOGGER.error(ex.getMessage(), ex);
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(PostgreSQLDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return preparedStatement;
    }

    public abstract T findByFields(Map<String, Object> Attr, String table);

    public abstract T single(int id);

    public abstract ArrayList<T> all();

    /**
     * Transforms a ResultSet object in a T type object.
     * @param resultReturnedFromDatabase
     * @return a T type object
     * @throws SQLException
     */
    public abstract T get(ResultSet resultReturnedFromDatabase) throws SQLException;

    /**
     * Gets the paginated results from the query.
     * @return the list of the objects found
     */
    public abstract ArrayList<T> allPaginate();

    /**
     * Counts the results returned by the query.
     * @return Integer
     */
    public abstract Integer count();

    /**
     * Compares and merges two T objects.
     * @param firstObject
     * @param secondObject
     * @return a T object which is the result of the merge
     */
    protected abstract T compareAndMergeObjects(T firstObject, T secondObject);

    /**
     * Prepares a search query.
     * @return SQLQueryBuilder
     */
    protected abstract SQLQueryBuilder prepareSearchQuery();

    /**
     * Switch database according to JWT payload information.
     * @param jwtClaimsSet set of claims in the JWT payload
     */
    public void setDataSourceFromJwtClaimsSet(JWTClaimsSet jwtClaimsSet) {
        if (jwtClaimsSet != null) {
            String issuer = jwtClaimsSet.getIssuer();
            if (JWT_ISSUER_DATASOURCE.containsKey(issuer)) {
                this.dataSource = JWT_ISSUER_DATASOURCE.get(issuer);
            }
        }
    }
}
