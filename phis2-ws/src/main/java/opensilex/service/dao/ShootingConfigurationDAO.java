//******************************************************************************
//                       ShootingConfigurationDAO.java
// SILEX-INRA
// Copyright © INRA 2018
// Creation date: 5 Feb. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import com.mongodb.BasicDBObject;
import java.text.SimpleDateFormat;
import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.configuration.DateFormats;
import opensilex.service.dao.manager.MongoDAO;
import opensilex.service.model.ShootingConfiguration;

/**
 * Image metadata shooting configuration DAO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ShootingConfigurationDAO extends MongoDAO<ShootingConfiguration> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ShootingConfigurationDAO.class);
    
    //Represents the mongodb documents label for the date 
    final static String DB_FIELDS_DATE = "date";
    //Represents the mongodb documents labels for the sensor positions
    final static String DB_FIELDS_SENSOR_POSITION = "sensorPosition";
    //Represents the mongodb documents labels for the timestamp
    final static String DB_FIELDS_TIMESTAMP = "timestamp";
    //Represents the mongodb documents label for the sensor uri
    final static String DB_FIELDS_SENSOR = "sensor";
    
    @Override
    protected BasicDBObject prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Gets a Document corresponding to an image shooting configuration and 
     * transforms it in a ShootingConfiguration object.
     * @param shootingConfigurationDocument
     * @return the Shooting configuration corresponding to the document
     */
    public static ShootingConfiguration mongoDocumentToShootingConfiguration(Document shootingConfigurationDocument) {
         ShootingConfiguration shootingConfiguration = new ShootingConfiguration();
        shootingConfiguration.setDate(new SimpleDateFormat(DateFormats.YMDHMSZ_FORMAT).format(shootingConfigurationDocument.getDate(ShootingConfigurationDAO.DB_FIELDS_DATE)));
        shootingConfiguration.setPosition(shootingConfigurationDocument.getString(ShootingConfigurationDAO.DB_FIELDS_SENSOR_POSITION));
        shootingConfiguration.setTimestamp(Long.toString(shootingConfigurationDocument.getLong(ShootingConfigurationDAO.DB_FIELDS_TIMESTAMP)));
        shootingConfiguration.setSensor(shootingConfigurationDocument.getString(ShootingConfigurationDAO.DB_FIELDS_SENSOR));
        
        return shootingConfiguration;
    }

    @Override
    public List<ShootingConfiguration> create(List<ShootingConfiguration> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<ShootingConfiguration> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<ShootingConfiguration> update(List<ShootingConfiguration> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ShootingConfiguration find(ShootingConfiguration object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ShootingConfiguration findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<ShootingConfiguration> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
