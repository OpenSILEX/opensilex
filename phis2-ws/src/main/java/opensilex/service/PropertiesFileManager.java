//******************************************************************************
//                              PropertiesFileManager.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: August 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr,
//          morgane.vidal@inra.fr
//******************************************************************************
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Property file manager.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class PropertiesFileManager {

    final static Logger LOGGER = LoggerFactory.getLogger(PropertiesFileManager.class.getName());

    /**
     * Read config file and return a Property object.
     * @param fileName
     * @return null | Properties
     */
    public static Properties parseFile(String fileName) {
        InputStream inputStream = null;
        final Properties props = new Properties();

        final String filePath = "/" + fileName + ".properties";

        try {
            inputStream = PropertiesFileManager.class.getResourceAsStream(filePath);
            props.load(inputStream);
        } catch (IOException | NullPointerException ex) {
            LOGGER.error(ex.getMessage(), ex);
            
            /* If tha parameters are ne recovered, an INTERNAL_SERVER_ERROR 
            exception is thrown */
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error : Cannot find " + fileName + " configuration file for the wanted database\n" + ex.getMessage())
                    .build());
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

    /**
     * Parses a binary public key.
     * @param configurationFileName
     * @return the key parsed
     */
    public static RSAPublicKey parseBinaryPublicKey(String configurationFileName) {
        RSAPublicKey generatedRSAPublicKey = null;
        DataInputStream dataInputStream = null;
        try {
            URL resource = PropertiesFileManager.class.getResource("/" + configurationFileName + ".der");
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
            // INTERNAL_SERVER_ERROR thrown if parameters are not recovered
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
     * Reads a configuration file and returns a specific attribute.
     * @param configurationFileName
     * @param attribute
     * @return null | Properties
     */
    public static String getConfigFileProperty(String configurationFileName, String attribute) {
        try {
            final Properties sqlProps = parseFile(configurationFileName);
            final StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(sqlProps.getProperty(attribute));
            return strBuilder.toString();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            ex.printStackTrace();
            // INTERNAL_SERVER_ERROR thrown if parameters are not recovered
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error : Cannot find properties in the configuration file " + configurationFileName + " for the wanted database\n" + ex.getMessage())
                    .build());
        }
    }

    /**
     * Reads a configuration file and returns the SQL connection URL with the 
     * database name, the user and the server name.
     * @param configurationFileName
     * @return null | Properties
     */
    public static String getSQLConnectionUrl(String configurationFileName) {
        try {
            final Properties sqlProps = parseFile(configurationFileName);
            final StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(sqlProps.getProperty("url"))
                    .append("?")
                    .append("user=").append(sqlProps.getProperty("username"))
                    .append("&")
                    .append("password=").append(sqlProps.getProperty("password"));
            return strBuilder.toString();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            // Si les paramètres ne sont pas récupérés le web service propage une exception INTERNAL_SERVER_ERROR
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error : Cannot find " + configurationFileName + " configuration file for the wanted database\n" + ex.getMessage()).build());
        }
    }

    /**
     * Reads a configuration file and get a PoolProperties object that permits 
     * to configure a relational database connection set.
     * @see https://tomcat.apache.org/tomcat-8.0-doc/jdbc-pool.html
     * @param configurationFileName
     * @return null | Properties
     */
    public static PoolProperties getSQLPoolDataSourceProperties(String configurationFileName) {
        try {
            final PoolProperties p = new PoolProperties();
            // minimal configuration
            p.setUrl(getConfigFileProperty(configurationFileName, "url"));
            p.setDriverClassName(getConfigFileProperty(configurationFileName, "driver"));
            p.setUsername(getConfigFileProperty(configurationFileName, "username"));
            p.setPassword(getConfigFileProperty(configurationFileName, "password"));
            // Optional 
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "jmxEnabled"), "null")) {
                p.setJmxEnabled(Boolean.valueOf(getConfigFileProperty(configurationFileName, "jmxEnabled")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "defaultAutoCommit"), "null")) {
                p.setDefaultAutoCommit(Boolean.valueOf(getConfigFileProperty(configurationFileName, "defaultAutoCommit")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "testWhileIdle"), "null")) {
                p.setTestWhileIdle(Boolean.valueOf(getConfigFileProperty(configurationFileName, "testWhileIdle")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "testOnBorrow"), "null")) {
                p.setTestOnBorrow(Boolean.valueOf(getConfigFileProperty(configurationFileName, "testOnBorrow")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "validationQuery"), "null")) {
                p.setValidationQuery(getConfigFileProperty(configurationFileName, "validationQuery"));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "testOnReturn"), "null")) {
                p.setTestOnReturn(Boolean.valueOf(getConfigFileProperty(configurationFileName, "testOnReturn")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "validationInterval"), "null")) {
                p.setValidationInterval(Long.valueOf(getConfigFileProperty(configurationFileName, "validationInterval")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "timeBetweenEvictionRunsMillis"), "null")) {
                p.setTimeBetweenEvictionRunsMillis(Integer.valueOf(getConfigFileProperty(configurationFileName, "timeBetweenEvictionRunsMillis")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "maxActive"), "null")) {
                p.setMaxActive(Integer.valueOf(getConfigFileProperty(configurationFileName, "maxActive")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "minIdle"), "null")) {
                p.setMinIdle(Integer.valueOf(getConfigFileProperty(configurationFileName, "minIdle")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "maxIdle"), "null")) {
                p.setMaxIdle(Integer.valueOf(getConfigFileProperty(configurationFileName, "maxIdle")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "initialSize"), "null")) {
                p.setInitialSize(Integer.valueOf(getConfigFileProperty(configurationFileName, "initialSize")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "maxWait"), "null")) {
                p.setMaxWait(Integer.valueOf(getConfigFileProperty(configurationFileName, "maxWait")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "removeAbandonedTimeout"), "null")) {
                p.setRemoveAbandonedTimeout(Integer.valueOf(getConfigFileProperty(configurationFileName, "removeAbandonedTimeout")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "maxAge"), "null")) {
                p.setMaxAge(Long.valueOf(getConfigFileProperty(configurationFileName, "maxAge")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "logAbandoned"), "null")) {
                p.setLogAbandoned(Boolean.valueOf(getConfigFileProperty(configurationFileName, "logAbandoned")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "removeAbandoned"), "null")) {
                p.setRemoveAbandoned(Boolean.valueOf(getConfigFileProperty(configurationFileName, "removeAbandoned")));
            }
            if (!Objects.equals(getConfigFileProperty(configurationFileName, "jdbcInterceptors"), "null")) {
                p.setJdbcInterceptors(getConfigFileProperty(configurationFileName, "jdbcInterceptors"));
            }

            return p;

        } catch (NumberFormatException ex) {
            LOGGER.error("Error : Cannot find " + configurationFileName + " configuration file \n", ex);
            return null;
        }
    }
}
