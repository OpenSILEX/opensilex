//******************************************************************************
//                       ShootingConfigurationDAOMongo.java
// SILEX-INRA
// Copyright © INRA 2018
// Creation date: 5 févr. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.mongo;

import com.mongodb.BasicDBObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.DateFormats;
import phis2ws.service.dao.manager.DAOMongo;
import phis2ws.service.view.model.phis.ShootingConfiguration;

/**
 * Represents the mongodb Data Access Object for the images metadata shooting 
 * configurations
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ShootingConfigurationDAOMongo extends DAOMongo<ShootingConfiguration> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ShootingConfigurationDAOMongo.class);
    
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
     * get a Document corresponding to an image shooting configuration and 
     * transform it in a ShootingConfiguration object
     * @param shootingConfigurationDocument
     * @return the Shooting configuration corresponding to the document
     */
    public static ShootingConfiguration mongoDocumentToShootingConfiguration(Document shootingConfigurationDocument) {
         ShootingConfiguration shootingConfiguration = new ShootingConfiguration();
        shootingConfiguration.setDate(new SimpleDateFormat(DateFormats.YMDHMSZ_FORMAT).format(shootingConfigurationDocument.getDate(ShootingConfigurationDAOMongo.DB_FIELDS_DATE)));
        shootingConfiguration.setPosition(shootingConfigurationDocument.getString(ShootingConfigurationDAOMongo.DB_FIELDS_SENSOR_POSITION));
        shootingConfiguration.setTimestamp(Long.toString(shootingConfigurationDocument.getLong(ShootingConfigurationDAOMongo.DB_FIELDS_TIMESTAMP)));
        shootingConfiguration.setSensor(shootingConfigurationDocument.getString(ShootingConfigurationDAOMongo.DB_FIELDS_SENSOR));
        
        return shootingConfiguration;
    }

    @Override
    public List<ShootingConfiguration> create(List<ShootingConfiguration> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<ShootingConfiguration> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<ShootingConfiguration> update(List<ShootingConfiguration> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ShootingConfiguration find(ShootingConfiguration object) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ShootingConfiguration findById(String id) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
