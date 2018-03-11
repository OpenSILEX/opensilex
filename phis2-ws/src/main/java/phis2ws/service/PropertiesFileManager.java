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
package phis2ws.service;

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
 * Gère les méthodes appelant tous types de propriétés à partir de fichier de
 * configuration Les fichiers sont disponible dans /src/main/resources par
 * défaut dans un projet Maven
 *
 * @date 05/2016
 * @author Arnaud Charleroy
 */
public class PropertiesFileManager {

    final static Logger logger = LoggerFactory.getLogger(PropertiesFileManager.class.getName());

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
//        System.err.println(filePath);

        try {
            inputStream = PropertiesFileManager.class.getResourceAsStream(filePath);
            props.load(inputStream);
        } catch (IOException | NullPointerException ex) {
            logger.error(ex.getMessage(), ex);
            // Si les paramètres ne sont pas récupérés le web service propage une exception INTERNAL_SERVER_ERROR
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error : Cannot find " + fileName + " configuration file for the wanted database\n" + ex.getMessage()).build());
         } finally { 
            if (inputStream != null) { 
                try { 
                    inputStream.close(); 
                } catch (IOException ex) { 
                    logger.error(ex.getMessage(), ex); 
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
            logger.error(ex.getMessage(), ex);
            // Si les paramètres ne sont pas récupérés le web service propage une exception INTERNAL_SERVER_ERROR
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Can't process JWT Token" + ex.getMessage()).build());
        } finally { 
            if (dataInputStream != null) { 
                try { 
                    dataInputStream.close(); 
                } catch (IOException ex) { 
                    logger.error(ex.getMessage(), ex); 
                } 
            } 
        } 
        return generatedRSAPublicKey;
    }

    /**
     * Lit un fichier de configuration et retourne d'un attribut spécifique
     *
     * @param fileName nom du fichier à lire
     * @param prop nom de lattribut
     * @return null | Properties
     */
    public static String getConfigFileProperty(String fileName, String prop) {
        try {
            final Properties sqlProps = parseFile(fileName);
            final StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(sqlProps.getProperty(prop));
            return strBuilder.toString();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            ex.printStackTrace();
            // Si les paramètres ne sont pas récupérés le web service propage une exception INTERNAL_SERVER_ERROR
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error : Cannot find properties in the configuration file " + fileName + " for the wanted database\n" + ex.getMessage()).build());
        }
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
            final Properties sqlProps = parseFile(fileName);
            final StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(sqlProps.getProperty("url"))
                    .append("?")
                    .append("user=").append(sqlProps.getProperty("username"))
                    .append("&")
                    .append("password=").append(sqlProps.getProperty("password"));
            return strBuilder.toString();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
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
            logger.error("Error : Cannot find " + fileName + " configuration file \n", ex);
            return null;
        }
    }

}
