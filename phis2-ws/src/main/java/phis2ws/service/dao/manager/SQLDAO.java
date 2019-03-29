//******************************************************************************
//                                 SQLDAO.java 
// SILEX-PHIS
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.manager;

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
import phis2ws.service.authentication.TokenManager;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.dao.datasource.DataSourceDAOPhisBrapi;
import phis2ws.service.model.User;
import phis2ws.service.utils.JsonConverter;
import phis2ws.service.utils.sql.SQLQueryBuilder;

/**
 * DAO for relational database querying
 * @author Arnaud Charleroy
 * @param <T>
 */
public abstract class SQLDAO<T> extends DAO<T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SQLDAO.class);
    protected final static String DUPLICATE_KEY_ERROR_POSTGRE = "23505";

    static final Map<String, DataSource> JWT_ISSUER_DATASOURCE;
    protected static final String PHIS_MODEL_DB_LOCATION = "Phis";
    protected static final String GNPIS_MODEL_DB_LOCATION = "GnpIS";

    // to manage multiple database switch 
    static {
        Map<String, DataSource> tmpMap = new HashMap<>();
        tmpMap.put(DAOPhisBrapi.PHIS_MODEL_DB_LOCATION, DataSourceDAOPhisBrapi.getInstance());
        tmpMap.put(DAOPhisBrapi.GNPIS_MODEL_DB_LOCATION, DataSourceDAOPhisBrapi.getInstance());
        JWT_ISSUER_DATASOURCE = Collections.unmodifiableMap(tmpMap);
    }

    // For query logging
    protected static final String SQL_SELECT_QUERY = "SQL query : ";
    
    /**
     * user c'est l'objet qui représente l'utilisateur
     */
    public User user;
    protected Integer page;
    protected Integer pageSize;

    public String remoteUserAdress;
    /**
     * Nom de la table du dao (table principale)
     */
    protected String table;
    protected String tableAlias;

    /**
     * Connexion du DAO pool de con ;)
     */
    protected DataSource dataSource;

    /**
     * pour le batch
     */
    protected final int batchSize = 1000;

//    protected static Connection connectCommitFalse = null;
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
     * La page de l'api brapi commence à 0
     *
     * @return numéro de la page courante
     */
    public Integer getPage() {
        if (page == null || pageSize < 0) {
            return 0;
        }
        return page;
    }

    /**
     * La page de l'api brapi pour pouvoir l'utiliser pour la pagination dans
     * une base de données
     *
     * @return numéro de la page courante + 1
     */
    public Integer getPageForDBQuery() {
        if (page == null || pageSize < 0) {
            return 1;
        }
        return page + 1;
    }

    /**
     * Définit le paramètre page
     *
     * @param page
     */
    public void setPage(Integer page) {
        if (page < 0) {
            this.page = Integer.valueOf(DefaultBrapiPaginationValues.PAGE);
        }
        this.page = page;
    }

    /**
     * Retourne le paramètre taille de la page
     *
     * @return Integer taille de la page
     */
    public Integer getPageSize() {
        if (pageSize == null || pageSize < 0) {
            return Integer.valueOf(DefaultBrapiPaginationValues.PAGE_SIZE);
        }
        return pageSize;
    }

    /**
     * Définit le paramètre taille de page
     *
     * @param pageSize
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Définit un objet utilisateur à partir d'un identifiant
     *
     * @param id identifiant
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
     * Pas pratique les ressources (statement et resulset) restent ouvertes
     * Exécute une requête de recherche à partir d'un DAO qui est en lien vers
     * une BD relationnelle.
     *
     * @author Arnaud Charleroy
     * @param query
     * @return
     * @deprecated
     * @throws SQLException
     */
    public ResultSet selectQueryFromDAO(String query) throws SQLException {
        Connection con = null;
        Statement stat = null;
        con = dataSource.getConnection();
        stat = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
//        System.err.println(query);
        return stat.executeQuery(query);
    }

    /**
     *
     * Exécute une requête d'ajout de suppresion ou de mise à jour à partir d'un
     * DAO qui est en lien vers une BD relationnelle.
     *
     * @author Arnaud Charleroy
     * @param query
     * @return
     * @throws SQLException
     */
    public Integer insertOrUpdateOrDeleteQueryFromDAO(String query) throws SQLException {
        Connection con = dataSource.getConnection();
        Statement stat = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
        Integer executeUpdate = stat.executeUpdate(query);
        stat.close();
        con.close();
        return executeUpdate;
    }

    /**
     * Prepare une requete sql en utilisant une chaine de variables sous forme
     * variable1,variable2
     *
     * @author Samuel Cherimon
     * @param variables
     * @param request
     * @update AC 05/16 Rendre la méthode générique
     * @param column column pour laquelle les variables seront ajoutées
     * @return La chaine correspondant a la requete complète
     *
     */
    public static String formatMultipleValueQuery(String variables, String column, String request) {
        StringTokenizer st = new StringTokenizer(variables, ",");
        String result = "(";
        result = result + column + " = '" + st.nextToken() + "'";
//        result = result + "mm.\"codeVariable\" = '" + st.nextToken() + "'";
        while (st.hasMoreTokens()) {
            result = result + " OR " + column + " = '" + st.nextToken() + "'";
//            result = result + " OR mm.\"codeVariable\" = '" + st.nextToken() + "'";
        }
        result = result + ")";
        return request + " AND " + result;
    }

    /**
     * Construit et exécute la requête qui permet de savoir si un objet de type
     * T est présent dans la base dans une base de données relationnelle à
     * partir des informations d'un objet T
     *
     * @param obj l'objet à chercher
     * @return
     * @throws Exception
     */
    public boolean existInDB(T obj) throws Exception {
        String query = new StringBuilder("SELECT * ")
                .append("FROM ")
                .append("\"").append(table).append("\"")
                .append(" WHERE ")
                .append(makeFindSQLConditionQuery(obj, false)).toString();
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
     * Construit et exécute la requête qui permet de trouver un objet de type T
     * dans une base de données relationnelle à partir des informations d'un
     * objet T, Cette fonction retourne un objet de type T
     *
     * @param obj l'objet à chercher
     * @return
     * @throws Exception
     */
    @Override
    public T find(T obj) throws Exception {
        StringBuilder strSQLBuilder = new StringBuilder();
        // Requete SELECT préparée
        strSQLBuilder.append("SELECT * ")
                .append("FROM ")
                .append("\"").append(table).append("\"")
                .append(" WHERE ")
                .append(makeFindSQLConditionQuery(obj, false));
        LOGGER.debug(strSQLBuilder.toString());
        Statement Statement = null;
        ResultSet rs = null;
        Connection con = null;
        Map<String, String> objectFields = relationFieldsJavaSQLObject();
        final Field[] attributes = obj.getClass().getDeclaredFields();
        LOGGER.debug(JsonConverter.ConvertToJson(obj));
        try {
            con = dataSource.getConnection();
            Statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            rs = Statement.executeQuery(strSQLBuilder.toString());
            if (rs != null && rs.first()) {
                for (Field field : attributes) {
                    field.setAccessible(true);
                    if (objectFields.containsKey(field.getName())
                            && rs.getObject(objectFields.get(field.getName())) != null) {
                        if (rs.getObject(objectFields.get(field.getName())) instanceof Date) {
                            if (field.getType() == String.class) {
                                LocalDate fromDateFields = LocalDate.fromDateFields((Date) rs.getObject(objectFields.get(field.getName())));
                                field.set(obj, fromDateFields.toString("yyyy-MM-dd"));
                            } else if (field.getType() == DateTime.class) {
                                Timestamp ts = new Timestamp(((Date) rs.getObject(objectFields.get(field.getName()))).getTime());
                                field.set(obj, new DateTime(ts));
                            }
                        } else if (rs.getObject(objectFields.get(field.getName())) instanceof Geometry) {
                            if (field.getType() == String.class) {
                                field.set(obj, field.toString());
                            }
                        } else {
                            field.set(obj, rs.getObject(objectFields.get(field.getName()).replaceAll("\"", "")).toString());
                        }
                    }
                }
            }
            return obj;
        } catch (SQLException e) {
            LOGGER.error("SQL error Exist Request ", e);
            LOGGER.error(strSQLBuilder.toString());
//            e.printStackTrace();
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
     * Construit et exécute la requête qui permet d'inserér un objet de type T
     * dans une base de données relationnelle à partir des informations d'un
     * objet T
     *
     * @param obj l'objet à inserér
     * @return
     * @throws Exception
     */
    public Boolean create(T obj) throws Exception {
        String query = new StringBuilder("INSERT INTO ")
                .append("\"").append(table).append("\" ").toString();
        Connection con = null;
        PreparedStatement preparedStatement = null;
        try {
            con = dataSource.getConnection();
            preparedStatement = makeCreatePreparedSQLConditionQuery(con, query, obj);
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
     * HashMap qui décrit les attributs qui correspondent à la clé primaire de
     * l'objet et leur label dans la BD
     *
     * @return Map<String,String>
     */
    public abstract Map<String, String> pkeySQLFieldLink();

    /**
     * HashMap qui décrit les attributs qui correspondent à l'objet et leur
     * label dans la BD
     *
     * @return Map<String,String>
     */
    public abstract Map<String, String> relationFieldsJavaSQLObject();

    /**
     * Crée automatiquement une requête pour récupérer un élément dans une base
     * de données à partir de sa clé primaire
     *
     * @param obj
     * @param like
     * @return String requête SQL
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
     * Crée automatiquement une requête pour insérer un élément dans une base de
     * données à partir de sa clé primaire
     *
     * @param obj l'objet à inserér
     * @return
     */
    public String makeCreateSQLConditionQuery(T obj) {
        final Map<String, String> createSQLField = this.relationFieldsJavaSQLObject();
        StringBuilder attributesBuilder = new StringBuilder();
        StringBuilder valuesBuilder = new StringBuilder();
        final Field[] attributes = obj.getClass().getDeclaredFields();
        try {
            for (Field field : attributes) {
                field.setAccessible(true);
                Object fieldObject = field.get(obj);
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
     * Crée automatiquement une requête préparée pour insérer un élément dans
     * une base de données à partir de sa clé primaire
     *
     * @param con
     * @param query L'endroit de l'insertion
     * @param obj l'objet à inserér
     * @return
     */
    public PreparedStatement makeCreatePreparedSQLConditionQuery(Connection con, String query, T obj) {
        PreparedStatement preparedStatement = null;
        final Map<String, String> createSQLField = this.relationFieldsJavaSQLObject();
        StringBuilder attributesBuilder = new StringBuilder();
        StringBuilder valuesBuilder = new StringBuilder();
        final Field[] attributes = obj.getClass().getDeclaredFields();

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
//            System.err.println(createSQLValuesQuery);

            con = dataSource.getConnection();
            preparedStatement = con.prepareStatement(createSQLValuesQuery);
            int fieldCount = 1;
            for (Field field : attributes) {
                field.setAccessible(true);
                Object fieldObject = field.get(obj);
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
            java.util.logging.Logger.getLogger(SQLDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return preparedStatement;
    }

    public abstract T findByFields(Map<String, Object> Attr, String table);

    public abstract T single(int id);

    public abstract ArrayList<T> all();

    /**
     * Transforme un objet ResulSet en objet défini T
     *
     * @param result Données retournées par la BD
     * @return un objet de type défini T
     * @throws SQLException
     */
    public abstract T get(ResultSet result) throws SQLException;

    /**
     * Retourne les élements retournés par la requête en prenant en compte la
     * pagination de l'utilisateur
     *
     * @return
     */
    public abstract ArrayList<T> allPaginate();

    /**
     * Compte le nombre d'élement retournés par la requête
     *
     * @return Integer
     */
    public abstract Integer count();

    /**
     * Compare deux objet du type T
     *
     * @param fromDB Premier objet à comparer
     * @param object Deuxième objet à comparer
     * @return un objet avec les informations des deux objets
     */
    protected abstract T compareAndMergeObjects(T fromDB, T object);

    /**
     * Fonction qui permet de créer la partie commune d'une requête à la fois
     * pour lister les éléments et les récupérés
     *
     * @return SQLQueryBuilder
     */
    protected abstract SQLQueryBuilder prepareSearchQuery();

    /**
     * Switch database according to jwt payload information
     *
     * @param jwtClaimsSet set of claims in the jwt payload
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
