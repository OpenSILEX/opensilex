//******************************************************************************
//                                       UriGenerator.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 9 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  9 mars 2018
// Subject: class to generate differents kinds of uris (vector, sensor, ...)
//******************************************************************************
package phis2ws.service.utils;

import java.util.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.sesame.AgronomicalObjectDaoSesame;
import phis2ws.service.dao.sesame.SensorDAOSesame;
import phis2ws.service.dao.sesame.VectorDAOSesame;

/**
 * generate differents kinds of uris (vector, sensor, ...)
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class UriGenerator {
    
    final static Logger LOGGER = LoggerFactory.getLogger(UriGenerator.class);
    
    private static final String PROPERTIES_SERVICE_FILE_NAME = "service";
    private static final String PROPERTIES_SERVICE_BASE_URI = "baseURI";
    
    private static final String URI_CODE_SENSOR = "s";
    private static final String URI_CODE_VECTOR = "v";
    private static final String URI_CODE_AGRONOMICAL_OBJECT = "o";
    
    private static final String PLATFORM_URI = PropertiesFileManager.getConfigFileProperty(PROPERTIES_SERVICE_FILE_NAME, PROPERTIES_SERVICE_BASE_URI);
    
    /**
     * generates a new vector uri. 
     * a vector uri has the following form : 
     * <prefix>:<year>/<unic_code>
     * <unic_code> = 1 letter type + 2 numbers year + auto incremented number
     * with 3 digits (per year)
     * e.g. http://www.phenome-fppn.fr/diaphen/2017/v1702
     * @param year the insertion year of the vector.
     * @return the new vector uri
     */
    private String generateVectorUri(String year) {        
        //1. get the actual number of vectors in the triplestor for the year
        VectorDAOSesame vectorDAO = new VectorDAOSesame();
        int numberExistingVectors = vectorDAO.getNumberOfVectors(year);
        
        //2. generates vectors uri
        String numberOfVectors = Integer.toString(numberExistingVectors + 1);
        String newVectorNumber;
        
        if (numberOfVectors.length() == 1) {
            newVectorNumber = "0" + numberOfVectors;
        } else {
            newVectorNumber = numberOfVectors;
        }
        
        return PLATFORM_URI + year + "/" + URI_CODE_VECTOR + year.substring(2,4) + newVectorNumber;
    }
    
    /**
     * generates a new sensor uri. 
     * a sensor uri has the following form : 
     * <prefix>:<year>/<unic_code>
     * <unic_code> = 1 letter type + 2 numbers year + auto incremented number
     * with 2 digits (per year)
     * the year corresponds to the year of insertion in the triplestore
     * e.g. http://www.phenome-fppn.fr/diaphen/2017/s17002
     * @param year the insertion year of the sensor.
     * @return the new sensor uri
     */
    private String generateSensorUri(String year) {
        //1. get the actual number of sensors in the triplestor for the year
        SensorDAOSesame sensorDAO = new SensorDAOSesame();
        int numberExistingSensors = sensorDAO.getNumberOfSensors(year);
        
        //2. generates sensor uri
        int sensorNumber = numberExistingSensors + 1;
        String numberOfSensors = Integer.toString(sensorNumber);
        String newSensorNumber;
        
        switch (numberOfSensors.length()) {
            case 1:
                newSensorNumber = "00" + numberOfSensors;
                break;
            case 2:
                newSensorNumber = "0" + numberOfSensors;
                break;
            default:
                newSensorNumber = numberOfSensors;
                break;
        }
        
        return PLATFORM_URI + year + "/" + URI_CODE_SENSOR + year.substring(2,4) + newSensorNumber;
    }
    
    /**
     * generates a new agronomical object uri. 
     * a sensor uri has the following form : 
     * <prefix>:<year>/<unic_code>
     * <unic_code> = 1 letter type + 2 numbers year + auto incremented number
     * with 6 digits (per year)
     * the year corresponds to the year of insertion in the triplestore
     * e.g. http://www.phenome-fppn.fr/diaphen/2017/o17000001
     * @param year the insertion year of the agronomical object.
     * @return the new agronomical object uri
     */
    private String generateAgronomicalObjectUri(String year) {
        //1. get the existing uri with the higher number for the year 
        //(i.e. the last inserted agronomical object for the year)
        AgronomicalObjectDaoSesame agronomicalObjectDAO = new AgronomicalObjectDaoSesame();
        int lastAgronomicalObjectIdFromYear = agronomicalObjectDAO.getLastAgronomicalObjectIdFromYear(year);
        
        //2. generates agronomical object uri
        int agronomicalObjectNumber = lastAgronomicalObjectIdFromYear + 1;
        String agronomicalObjectId = Integer.toString(agronomicalObjectNumber);
        
        
        while (agronomicalObjectId.length() < 6) {
            agronomicalObjectId = "0" + agronomicalObjectId;
        }
        
        return PLATFORM_URI + year + "/" + URI_CODE_AGRONOMICAL_OBJECT + year.substring(2,4) + agronomicalObjectId;
    }
    
    /**
     * generates the uri of a new instance of instanceType
     * @param instanceType the rdf type of the instance. (a concept uri)
     * @param year year of the creation of the element. If it is null, it will 
     *             be the current year
     * @return the generated uri
     */
    public String generateNewInstanceUri(String instanceType, String year) {
        if (year == null) {
            year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
        }
        
        URINamespaces uriNamespaces = new URINamespaces();
        
        if (uriNamespaces.getObjectsProperty("cVector").equals(instanceType)) {
            return generateVectorUri(year);
        } else if (uriNamespaces.getObjectsProperty("cSensor").equals(instanceType)) {
            return generateSensorUri(year);
        } else {
            AgronomicalObjectDaoSesame agronomicalObjectDAO = new AgronomicalObjectDaoSesame();
            if (agronomicalObjectDAO.isObjectAgronomicalObject(instanceType)) {
                return generateAgronomicalObjectUri(year);
            }
        }
        
        return null;
    }
}
