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
import java.util.logging.Level;
import java.util.logging.Logger;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.mongo.ImageMetadataDaoMongo;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.dao.sesame.AgronomicalObjectDAOSesame;
import phis2ws.service.dao.sesame.MethodDaoSesame;
import phis2ws.service.dao.sesame.SensorDAOSesame;
import phis2ws.service.dao.sesame.UriDaoSesame;
import phis2ws.service.dao.sesame.TraitDaoSesame;
import phis2ws.service.dao.sesame.UnitDaoSesame;
import phis2ws.service.dao.sesame.VariableDaoSesame;
import phis2ws.service.dao.sesame.VectorDAOSesame;
import phis2ws.service.model.User;

/**
 * generate differents kinds of uris (vector, sensor, ...)
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class UriGenerator {
    private static final String PROPERTIES_SERVICE_FILE_NAME = "service";
    private static final String PROPERTIES_SERVICE_BASE_URI = "baseURI";
    
    private static final String URI_CODE_AGRONOMICAL_OBJECT = "o";
    private static final String URI_CODE_METHOD = "m";
    private static final String URI_CODE_SENSOR = "s";
    private static final String URI_CODE_TRAIT = "t";
    private static final String URI_CODE_UNIT = "u";
    private static final String URI_CODE_VARIABLE = "v";
    private static final String URI_CODE_VECTOR = "v";
    private static final String URI_CODE_IMAGE = "i";
    
    private static final String PLATFORM_URI = PropertiesFileManager.getConfigFileProperty(PROPERTIES_SERVICE_FILE_NAME, PROPERTIES_SERVICE_BASE_URI);
    private static final String PLATFORM_URI_ID = PLATFORM_URI + "id/";
    private static final String PLATFORM_URI_ID_METHOD = PLATFORM_URI_ID + "methods/";
    private static final String PLATFORM_URI_ID_TRAITS = PLATFORM_URI_ID + "traits/";
    private static final String PLATFORM_URI_ID_UNITS = PLATFORM_URI_ID + "units/";
    private static final String PLATFORM_URI_ID_VARIABLES = PLATFORM_URI_ID + "variables/";
    private static final String PLATFORM_URI_ID_VARIETY = PLATFORM_URI + "v/";
    private static final String PLATFORM_URI_ID_AGENT = PLATFORM_URI_ID + "agent/";
    
    
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
        int lastSensorIdFromYear = sensorDAO.getLastIdFromYear(year);
        
        //2. generates sensor uri
        int sensorNumber = lastSensorIdFromYear + 1;
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
        //1. get the higher number for the year 
        //(i.e. the last inserted agronomical object for the year)
        AgronomicalObjectDAOSesame agronomicalObjectDAO = new AgronomicalObjectDAOSesame();
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
     * generates a new variable uri.
     * a variable uri follows the pattern :
     * <prefix>:id/variables/<unic_code>
     * <unic_code> = 1 letter type + auto incremented number with 3 digits
     * e.g. http://www.phenome-fppn.fr/diaphen/id/variables/v001
     * @return the new variable uri
     */
    private String generateVariableUri() {
        //1. get the higher variable id (i.e. the last 
        //inserted variable)
        VariableDaoSesame variableDAO = new VariableDaoSesame();
        int lastVariableId = variableDAO.getLastId();
        
        //2. generates variable uri
        int newVariableId = lastVariableId + 1;
        String variableId = Integer.toString(newVariableId);
        
        while (variableId.length() < 3) {
            variableId = "0" + variableId;
        }
        
        return PLATFORM_URI_ID_VARIABLES + URI_CODE_VARIABLE + variableId; 
    }
    
    /**
     * generates a new trait uri.
     * a trait uri follows the pattern :
     * <prefix>:id/traits/<unic_code>
     * <unic_code> = 1 letter type + auto incremented number with 3 digits
     * e.g. http://www.phenome-fppn.fr/diaphen/id/traits/t001
     * @return the new trait uri
     */
    private String generateTraitUri() {
        //1. get the higher trait id (i.e. the last 
        //inserted trait)
        TraitDaoSesame traitDAO = new TraitDaoSesame();
        int lastTraitId = traitDAO.getLastId();
        
        //2. generates trait uri
        int newTraitId = lastTraitId + 1;
        String traitId = Integer.toString(newTraitId);
        
        while (traitId.length() < 3) {
            traitId = "0" + traitId;
        }
        
        return PLATFORM_URI_ID_TRAITS + URI_CODE_TRAIT + traitId; 
    }
    
    /**
     * generates a new method uri.
     * a method uri follows the pattern :
     * <prefix>:id/methods/<unic_code>
     * <unic_code> = 1 letter type + auto incremented number with 3 digits
     * e.g. http://www.phenome-fppn.fr/diaphen/id/methods/m001
     * @return the new method uri
     */
    private String generateMethodUri() {
        //1. get the higher method id (i.e. the last 
        //inserted method)
        MethodDaoSesame methodDAO = new MethodDaoSesame();
        int lastMethodId = methodDAO.getLastId();
        
        //2. generates method uri
        int newMethodId = lastMethodId + 1;
        String methodId = Integer.toString(newMethodId);
        
        while (methodId.length() < 3) {
            methodId = "0" + methodId;
        }
        
        return PLATFORM_URI_ID_METHOD + URI_CODE_METHOD + methodId; 
    }
    
    /**
     * generates a new unit uri.
     * a unit uri follows the pattern :
     * <prefix>:id/units/<unic_code>
     * <unic_code> = 1 letter type + auto incremented number with 3 digits
     * e.g. http://www.phenome-fppn.fr/diaphen/id/units/m001
     * @return the new unit uri
     */
    private String generateUnitUri() {
         //1. get the higher unit id (i.e. the last 
        //inserted unit)
        UnitDaoSesame unitDAO = new UnitDaoSesame();
        int lastUnitId = unitDAO.getLastId();
        
        //2. generates unit uri
        int newUnitId = lastUnitId + 1;
        String unitId = Integer.toString(newUnitId);
        
        while (unitId.length() < 3) {
            unitId = "0" + unitId;
        }
        
        return PLATFORM_URI_ID_UNITS + URI_CODE_UNIT + unitId; 
    }
    
    /**
     * generates a new variety uri.
     * a variety uri follows the pattern : 
     * <prefix>:v/<varietynameinlowercase>
     * e.g. http://www.phenome-fppn.fr/diaphen/v/dkc4814
     * @param variety the variety name
     * @return the new variety uri
     */
    private String generateVarietyUri(String variety) {
        return PLATFORM_URI_ID_VARIETY + variety.toLowerCase();
    }
    
    /**
     * generates a new agent uri.
     * a agent uri follows the pattern : 
     * <prefix>:id/agent/<unic_code>
     * <unic_code> = firstname first letter concat with lastname in lowercase
     * e.g. http://www.phenome-fppn.fr/diaphen/id/agent/acharleroy
     * @param agentEmail the agent email
     * @return the new agent uri
     */
    private String generateAgentUri(String agentEmail) {
        // retreive user information
        UserDaoPhisBrapi uspb = new UserDaoPhisBrapi();
        User user = new User(agentEmail); 
        try {
            uspb.find(user);
        } catch (Exception ex) {
            Logger.getLogger(UriGenerator.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        // format firstname and lastname
        String userFirstName = user.getFirstName().trim();
        String userFamilyName = user.getFamilyName().trim();
        userFamilyName = userFamilyName.replace(" ", "-");
      
        // create URI
        return PLATFORM_URI_ID_AGENT + userFirstName.toLowerCase().charAt(0) + userFamilyName.toLowerCase();
    }
    
    
    /**
     * generates a new image uri. 
     * an image uri follows the pattern : 
     * <prefix>:yyyy/<unic_code>
     * <unic_code> = 1 letter type (i) + 2 digits year + auto incremet with 10 digit
     * e.g. http://www.phenome-fppn.fr/diaphen/2018/i180000000001
     * @param year the year of insertion of the image
     * @param lastGeneratedUri if a few uri has been generated but not inserted, 
     *                         corresponds to the last generated uri 
     * @return the new uri
     */
    private String generateImageUri(String year, String lastGeneratedUri) {
        if (lastGeneratedUri == null) {
            ImageMetadataDaoMongo imageDaoMongo = new ImageMetadataDaoMongo();
            long imagesNumber = imageDaoMongo.getNbImagesYear();
            imagesNumber++;
            
            //calculate the number of 0 to add before the number of the image
            String nbImagesByYear = Long.toString(imagesNumber);
            while (nbImagesByYear.length() < 10) {
                nbImagesByYear = "0" + nbImagesByYear;
            }

            String uniqueId = URI_CODE_IMAGE + year.substring(2, 4) + nbImagesByYear;
            return PLATFORM_URI + year + "/" + uniqueId;
        } else {
            int uniqueId = Integer.parseInt(lastGeneratedUri.split("/" + URI_CODE_IMAGE + year.substring(2, 4))[1]);
            uniqueId++;
            
            String nbImagesByYear = Long.toString(uniqueId);
            while (nbImagesByYear.length() < 10) {
                nbImagesByYear = "0" + nbImagesByYear;
            }
            
            return PLATFORM_URI + year + nbImagesByYear;
        }
    }
    
    /**
     * generates the uri of a new instance of instanceType
     * @param instanceType the rdf type of the instance. (a concept uri)
     * @param year year of the creation of the element. If it is null, it will 
     *             be the current year
     * @param additionalInformation some additional informations used for some 
     *                              uri generators. (e.g. the variety name, 
     *                              or the last generated uri for the images)
     * @return the generated uri
     */
    public String generateNewInstanceUri(String instanceType, String year, String additionalInformation) {
        if (year == null) {
            year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
        }
        
        URINamespaces uriNamespaces = new URINamespaces();
        
        UriDaoSesame uriDaoSesame = new UriDaoSesame();
        
        if (uriDaoSesame.isSubClassOf(instanceType, uriNamespaces.getObjectsProperty("cVector"))) {
            return generateVectorUri(year);
        } else if (uriDaoSesame.isSubClassOf(instanceType, uriNamespaces.getObjectsProperty("cSensingDevice"))) {
            return generateSensorUri(year);
        } else if (uriNamespaces.getObjectsProperty("cVariable").equals(instanceType)) {
            return generateVariableUri();
        } else if (uriNamespaces.getObjectsProperty("cTrait").equals(instanceType)) {
            return generateTraitUri();
        } else if (uriNamespaces.getObjectsProperty("cMethod").equals(instanceType)) {
            return generateMethodUri();
        } else if (uriNamespaces.getObjectsProperty("cUnit").equals(instanceType)) {
            return generateUnitUri();
        } else if (uriDaoSesame.isSubClassOf(instanceType, uriNamespaces.getObjectsProperty("cAgronomicalObject"))){
            return generateAgronomicalObjectUri(year);
        } else if (uriNamespaces.getObjectsProperty("cVariety").equals(instanceType)) {
            return generateVarietyUri(additionalInformation);
        } else if (uriDaoSesame.isSubClassOf(instanceType, uriNamespaces.getObjectsProperty("cImage"))) {
            return generateImageUri(year, additionalInformation);
        } else if (instanceType.equals(uriNamespaces.getObjectsProperty("cAgent")) || uriDaoSesame.isSubClassOf(instanceType, uriNamespaces.getObjectsProperty("cAgent"))) {
            return generateAgentUri(additionalInformation);
        }
        
        return null;
    }
}
