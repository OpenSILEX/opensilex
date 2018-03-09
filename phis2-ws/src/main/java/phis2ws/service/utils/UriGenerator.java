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
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.sesame.SensorDAOSesame;
import phis2ws.service.dao.sesame.VectorDAOSesame;

/**
 * generate differents kinds of uris (vector, sensor, ...)
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class UriGenerator {
    
    private static final String PROPERTIES_SERVICE_FILE_NAME = "service";
    private static final String PROPERTIES_SERVICE_BASE_URI = "baseURI";
    
    private static final String URI_CODE_SENSOR = "s";
    private static final String URI_CODE_VECTOR = "v";
    
    /**
     * generates a new vector uri. 
     * a vector uri has the following form : 
     * <prefix>:<year>/<unic_code>
     * <unic_code> = 1 letter type + 2 numbers year + auto incremented number
     * with 3 digits (per year)
     * e.g. http://www.phenome-fppn.fr/diaphen/2017/v1702
     * @return the new vector uri
     */
    private String generateVectorUri() {
        String actualYear = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
        
        //1. get the actual number of vectors in the triplestor for the year
        VectorDAOSesame vectorDAO = new VectorDAOSesame();
        int numberExistingVectors = vectorDAO.getNumberOfVectors(actualYear);
        
        //2. generates vectors uri
        String platformUri = PropertiesFileManager.getConfigFileProperty(PROPERTIES_SERVICE_FILE_NAME, PROPERTIES_SERVICE_BASE_URI);
        
        String numberOfVectors = Integer.toString(numberExistingVectors + 1);
        String newVectorNumber;
        
        if (numberOfVectors.length() == 1) {
            newVectorNumber = "0" + numberOfVectors;
        } else {
            newVectorNumber = numberOfVectors;
        }
        
        return platformUri + "/" + actualYear + "/" + URI_CODE_VECTOR + actualYear.substring(0,2) + newVectorNumber;
    }
    
    /**
     * generates a new sensor uri. 
     * a sensor uri has the following form : 
     * <prefix>:<year>/<unic_code>
     * <unic_code> = 1 letter type + 2 numbers year + auto incremented number
     * with 2 digits (per year)
     * the year corresponds to the year of insertion in the triplestore
     * e.g. http://www.phenome-fppn.fr/diaphen/2017/s17002
     * @return the new vector uri
     */
    private String generateSensorUri() {
        String actualYear = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
        
        //1. get the actual number of sensors in the triplestor for the year
        SensorDAOSesame sensorDAO = new SensorDAOSesame();
        int numberExistingSensors = sensorDAO.getNumberOfSensors(actualYear);
        
        //2. generates sensor uri
        String platformUri = PropertiesFileManager.getConfigFileProperty(PROPERTIES_SERVICE_FILE_NAME, PROPERTIES_SERVICE_BASE_URI);
        
        String numberOfSensors = Integer.toString(numberExistingSensors + 1);
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
        
        return platformUri + "/" + actualYear + "/" + URI_CODE_SENSOR + actualYear.substring(0,2) + newSensorNumber;
    }
    
    /**
     * generates the uri of a new instance of instanceType
     * @param instanceType the rdf type of the instance. (a concept uri)
     * @return the generated uri
     */
    public String generateNewInstanceUri(String instanceType) {
        URINamespaces uriNamespaces = new URINamespaces();
        
        if (uriNamespaces.getContexts().get("cVector").equals(instanceType)) {
            return generateVectorUri();
        } else if (uriNamespaces.getContexts().get("cSensor").equals(instanceType)) {
            return generateSensorUri();
        }
        
        return null;
    }
}
