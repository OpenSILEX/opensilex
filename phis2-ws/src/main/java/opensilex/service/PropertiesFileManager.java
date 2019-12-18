//**********************************************************************************************
//                                       PropertiesFileManager.java 
//
// Author(s): Arnaud Charleroy, Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr,
//         morgane.vidal@inra.fr
// Last modification date:  January, 2017
// Subject: Read properties file
//***********************************************************************************************
package opensilex.service;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;
import java.util.Properties;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gère les méthodes appelant tous types de propriétés à partir de fichier de
 * configuration Les fichiers sont disponible dans /src/main/resources par
 * défaut dans un projet Maven
 *
 * @date 05/2016
 * @author Arnaud Charleroy
 */
public class PropertiesFileManager {

    final static Logger LOGGER = LoggerFactory.getLogger(PropertiesFileManager.class.getName());
    private static RDF4JConfig rdf4jConfig;
    private static MongoDBConfig mongoConfig;
    private static String storageBasePath;

    /**
     * Lit le fichier de configuration et retourne un objet Proprietes
     *
     * @param fileName nom du fichier à lire
     * @return null | Properties
     */
    public static Properties parseFile(String fileName) {
        InputStream inputStream = null;
        final Properties props = new Properties();
        //property is in /src/main/resources By default in maven project

        final String filePath = "/" + fileName + ".properties";

        try {
            inputStream = PropertiesFileManager.class.getResourceAsStream(filePath);
            props.load(inputStream);
        } catch (IOException | NullPointerException ex) {
            LOGGER.error(ex.getMessage(), ex);
            // Si les paramètres ne sont pas récupérés le web service propage une exception INTERNAL_SERVER_ERROR
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error : Cannot find " + fileName + " configuration file for the wanted database\n" + ex.getMessage()).build());
         } finally { 
            if (inputStream != null) { 
                try { 
                    inputStream.close(); 
                } catch (IOException ex) { 
                    LOGGER.error(ex.getMessage(), ex); 
                } 
            } 
        } 
        return props;
    }

    public static RSAPublicKey parseBinaryPublicKey(String fileName) {
        //property is in /src/main/resources By default in maven project
        RSAPublicKey generatedRSAPublicKey = null;
        DataInputStream dataInputStream = null;
        try {
            URL resource = PropertiesFileManager.class.getResource("/" + fileName + ".der");
            File publicKeyFile = new File(resource.getPath());
            dataInputStream = new DataInputStream(new FileInputStream(publicKeyFile));
            byte[] keyBytes = new byte[(int) publicKeyFile.length()];
            dataInputStream.readFully(keyBytes);
            dataInputStream.close();

            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            generatedRSAPublicKey = (RSAPublicKey) kf.generatePublic(spec);

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
            LOGGER.error(ex.getMessage(), ex);
            // Si les paramètres ne sont pas récupérés le web service propage une exception INTERNAL_SERVER_ERROR
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Can't process JWT Token" + ex.getMessage()).build());
        } finally { 
            if (dataInputStream != null) { 
                try { 
                    dataInputStream.close(); 
                } catch (IOException ex) { 
                    LOGGER.error(ex.getMessage(), ex); 
                } 
            } 
        } 
        return generatedRSAPublicKey;
    }

    /**
     * Phis postGreSQL configuration
     */
    private static PhisPostgreSQLConfig pgConfig;
    
    /**
     * Phis service configuration
     */
    private static PhisWsConfig phisConfig;
    
    /**
     * Setter for all configuration needed by phis
     * 
     * @param pgConfig PostGreSQL config
     * @param phisConfig Phis service configuration
     * @param coreConfig Application core config
     * @param mongoConfig MongoDB configuration
     * @param rdf4jConfig RDF4J configuration
     */
    public static void setOpensilexConfigs(
        PhisWsConfig phisConfig,
        RDF4JConfig rdf4jConfig,
        MongoDBConfig mongoConfig,
        String storageBasePath
    ) {
        PropertiesFileManager.phisConfig = phisConfig;
        PropertiesFileManager.rdf4jConfig = rdf4jConfig;
        PropertiesFileManager.mongoConfig = mongoConfig;
        PropertiesFileManager.pgConfig = phisConfig.postgreSQL();
        PropertiesFileManager.storageBasePath = storageBasePath;
    }
    
    /**
     * This method used to read configuration from file
     * It as been updated to use new YAML config system in modularity
     * So now this method map old properties to new ones
     *
     * @param fileName CoreConfig section
     * @param prop property name
     * @return null | property value
     */
    public static String getConfigFileProperty(String fileName, String prop) {
        String value = null;
        
        switch (fileName) {
            case "service":
                value = getServiceProperty(prop);
                break;
                
            case "phis_sql_config":
                value = getPgSQLProperty(prop);
                break;
                
            case "sesame_rdf_config":
                value = getRDF4JProperty(prop);
                break;
                
            case "mongodb_nosql_config":
                value = getMongoProperty(prop);
                break;                
                
            default:
                break;
        }
        
        return value;
    }

    /**
     * Map old properties which where con
     * @param prop
     * @return 
     */
    private static String getServiceProperty(String prop) {
        String value = null;
        
        switch (prop) {
            case "sessionTime":
                value = phisConfig.sessionTime();
                break;
            case "waitingFileTime":
                value = phisConfig.waitingFileTime();
                break;                
            case "uploadFileServerIP":
                value = phisConfig.uploadFileServerIP();
                break;
            case "uploadFileServerUsername":
                value = phisConfig.uploadFileServerUsername();
                break;
            case "uploadFileServerPassword":
                value = phisConfig.uploadFileServerPassword();
                break;
            case "uploadFileServerDirectory":
                value = storageBasePath;
                break;
            case "layerFileServerDirectory":
                value = phisConfig.layerFileServerDirectory();
                break;
            case "layerFileServerAddress":
                value = phisConfig.layerFileServerAddress();
                break;
            case "defaultLanguage":
                value = "en";
                break;
            case "uploadImageServerDirectory":
                value = phisConfig.uploadImageServerDirectory();
                break;
            case "imageFileServerDirectory":
                value = phisConfig.imageFileServerDirectory();
                break;     
            case "gnpisPublicKeyFileName":
                value = phisConfig.gnpisPublicKeyFileName();
                break;   
            case "phisPublicKeyFileName":
                value = phisConfig.phisPublicKeyFileName();
                break;   
            case "pageSizeMax":
                value = phisConfig.pageSizeMax();
                break;  
            default:
                break;
        }
        
        return value;
    }

    private static String getPgSQLProperty(String prop) {
         String value = null;
        
        switch (prop) {
            case "driver":
                value = pgConfig.driver();
                break;                 
            case "url":
                value = "jdbc:postgresql://" + pgConfig.host() 
                        + ":" + pgConfig.port() 
                        + "/" + pgConfig.database();
                break;
            case "username":
                value = pgConfig.username();
                break;                
            case "password":
                value = pgConfig.password();
                break;
            case "testWhileIdle":
                value = "" + pgConfig.testWhileIdle();
                break;
            case "testOnBorrow":
                value = "" + pgConfig.testOnBorrow();
                break;
            case "testOnReturn":
                value = "" + pgConfig.testOnReturn();
                break;                
            case "validationQuery":
                value = pgConfig.validationQuery();
                break;
            case "validationInterval":
                value = "" + pgConfig.validationInterval();
                break;
            case "timeBetweenEvictionRunsMillis":
                value = "" + pgConfig.timeBetweenEvictionRunsMillis();
                break;
            case "maxActive":
                value = "" + pgConfig.maxActive();
                break;
            case "minIdle":
                value = "" + pgConfig.minIdle();
                break;
            case "maxIdle":
                value = "" + pgConfig.maxIdle();
                break;
            case "maxWait":
                value = "" + pgConfig.maxWait();
                break;
            case "initialSize":
                value = "" + pgConfig.initialSize();
                break;
            case "removeAbandoned":
                value = "" + pgConfig.removeAbandoned();
                break;
            case "removeAbandonedTimeout":
                value = "" + pgConfig.removeAbandonedTimeout();
                break;
            case "logAbandoned":
                value = "" + pgConfig.logAbandoned();
                break;
            case "jmxEnabled":
                value = "" + pgConfig.jmxEnabled();
                break;
            case "maxAge":
                value = "" + pgConfig.maxAge();
                break;
            case "jdbcInterceptors":
                value = pgConfig.jdbcInterceptors();
                break;
            default:
                break;
        }
        
        return value;
    }

    private static String getRDF4JProperty(String prop) {
        String value = null;
        
        switch (prop) {
            case "sesameServer":
                value = rdf4jConfig.serverURI();
                break;
            case "repositoryID":
                value = rdf4jConfig.repository();
                break;                
            case "infrastructure":
                value = phisConfig.infrastructure();
                break;
            case "baseURI":
                value = phisConfig.ontologyBaseURI();
                break;
            case "vocabularyContext":
                value = phisConfig.vocabulary();
                break;
            default:
                break;
        }
        
        return value;
    }

    private static String getMongoProperty(String prop) {
         String value = null;
        
        switch (prop) {
            case "host":
                value = mongoConfig.host();
                break;
            case "port":
                value = "" + mongoConfig.port();
                break;
            case "user":
                value = mongoConfig.username();
                break;
            case "password":
                value = mongoConfig.password();
                break;
            case "authdb":
                value = mongoConfig.authDB();
                break;
            case "db":
                value = mongoConfig.database();
                break;                     
            case "documents":
                value = phisConfig.documentsCollection();
                break;
            case "provenance":
                value = phisConfig.provenanceCollection();
                break;
            case "data":
                value = phisConfig.dataCollection();
                break;
            case "images":
                value = phisConfig.imagesCollection();
                break;
            default:
                break;
        }
        
        return value;
    }
    
    /**
     * Lit un fichier de configuration et retourne l'url pour une base de donnée
     * SQL contenant le nom, l'utilisateur et le serveur
     *
     * @param fileName nom du fichier à lire
     * @return null | Properties
     */
    public static String getSQLConnectionUrl(String fileName) {
        try {
            final StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(getPgSQLProperty("url"))
                    .append("?")
                    .append("user=").append(getPgSQLProperty("username"))
                    .append("&")
                    .append("password=").append(getPgSQLProperty("password"));
            return strBuilder.toString();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            // Si les paramètres ne sont pas récupérés le web service propage une exception INTERNAL_SERVER_ERROR
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error : Cannot find " + fileName + " configuration file for the wanted database\n" + ex.getMessage()).build());
        }
    }

    /**
     * Lit un fichier de configuration et un objet PoolPropreties permettant de
     * configurer un jeu de connexion de base de donnée relationelle
     *
     * @see https://tomcat.apache.org/tomcat-8.0-doc/jdbc-pool.html
     * @param fileName nom du fichier à lire
     * @return null | Properties
     */
    public static PoolProperties getSQLPoolDataSourceProperties(String fileName) {
        try {
            final PoolProperties p = new PoolProperties();
            //  minimal configuration
            p.setUrl(getConfigFileProperty(fileName, "url"));
            p.setDriverClassName(getConfigFileProperty(fileName, "driver"));
            p.setUsername(getConfigFileProperty(fileName, "username"));
            p.setPassword(getConfigFileProperty(fileName, "password"));
            // Optional 
            if (!Objects.equals(getConfigFileProperty(fileName, "jmxEnabled"), "null")) {
                p.setJmxEnabled(Boolean.valueOf(getConfigFileProperty(fileName, "jmxEnabled")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "defaultAutoCommit"), "null")) {
                p.setDefaultAutoCommit(Boolean.valueOf(getConfigFileProperty(fileName, "defaultAutoCommit")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "testWhileIdle"), "null")) {
                p.setTestWhileIdle(Boolean.valueOf(getConfigFileProperty(fileName, "testWhileIdle")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "testOnBorrow"), "null")) {
                p.setTestOnBorrow(Boolean.valueOf(getConfigFileProperty(fileName, "testOnBorrow")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "validationQuery"), "null")) {
                p.setValidationQuery(getConfigFileProperty(fileName, "validationQuery"));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "testOnReturn"), "null")) {
                p.setTestOnReturn(Boolean.valueOf(getConfigFileProperty(fileName, "testOnReturn")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "validationInterval"), "null")) {
                p.setValidationInterval(Long.valueOf(getConfigFileProperty(fileName, "validationInterval")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "timeBetweenEvictionRunsMillis"), "null")) {
                p.setTimeBetweenEvictionRunsMillis(Integer.valueOf(getConfigFileProperty(fileName, "timeBetweenEvictionRunsMillis")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "maxActive"), "null")) {
                p.setMaxActive(Integer.valueOf(getConfigFileProperty(fileName, "maxActive")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "minIdle"), "null")) {
                p.setMinIdle(Integer.valueOf(getConfigFileProperty(fileName, "minIdle")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "maxIdle"), "null")) {
                p.setMaxIdle(Integer.valueOf(getConfigFileProperty(fileName, "maxIdle")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "initialSize"), "null")) {
                p.setInitialSize(Integer.valueOf(getConfigFileProperty(fileName, "initialSize")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "maxWait"), "null")) {
                p.setMaxWait(Integer.valueOf(getConfigFileProperty(fileName, "maxWait")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "removeAbandonedTimeout"), "null")) {
                p.setRemoveAbandonedTimeout(Integer.valueOf(getConfigFileProperty(fileName, "removeAbandonedTimeout")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "maxAge"), "null")) {
                p.setMaxAge(Long.valueOf(getConfigFileProperty(fileName, "maxAge")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "logAbandoned"), "null")) {
                p.setLogAbandoned(Boolean.valueOf(getConfigFileProperty(fileName, "logAbandoned")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "removeAbandoned"), "null")) {
                p.setRemoveAbandoned(Boolean.valueOf(getConfigFileProperty(fileName, "removeAbandoned")));
            }
            if (!Objects.equals(getConfigFileProperty(fileName, "jdbcInterceptors"), "null")) {
                p.setJdbcInterceptors(getConfigFileProperty(fileName, "jdbcInterceptors"));
            }

            return p;

        } catch (Exception ex) {
            LOGGER.error("Error : Cannot find " + fileName + " configuration file \n", ex);
            return null;
        }
    }
}
