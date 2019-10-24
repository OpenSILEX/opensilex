//******************************************************************************
//                               UriGenerator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.utils;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.codec.binary.Base32;
import org.apache.jena.sparql.AlreadyExists;
import opensilex.service.PropertiesFileManager;
import opensilex.service.dao.ActuatorDAO;
import opensilex.service.dao.ExperimentSQLDAO;
import opensilex.service.dao.ImageMetadataMongoDAO;
import opensilex.service.dao.GroupDAO;
import opensilex.service.dao.ScientificObjectRdf4jDAO;
import opensilex.service.dao.AnnotationDAO;
import opensilex.service.dao.EventDAO;
import opensilex.service.dao.MethodDAO;
import opensilex.service.dao.ProjectDAO;
import opensilex.service.dao.RadiometricTargetDAO;
import opensilex.service.dao.SensorDAO;
import opensilex.service.dao.UriDAO;
import opensilex.service.dao.TraitDAO;
import opensilex.service.dao.UnitDAO;
import opensilex.service.dao.VariableDAO;
import opensilex.service.dao.VectorDAO;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Foaf;
import opensilex.service.ontology.Oeev;
import opensilex.service.ontology.Oeso;
import opensilex.service.ontology.Time;
import opensilex.service.model.Group;

/**
 * URI generator. Used for various objects (vector, sensor, ...).
 * @update [Andreas Garcia] 14 Feb. 2019: use the last inserted experiment
 * number instead of total number of experiment to calculate the number of a
 * new experiment
 * @update [Andreas Garcia] 15 Apr. 2019: make all functions static because they don't need instantiation.
 * new experiment
 * @author Morgane Vidal <morgane.vidal@inra.fr>, Arnaud Charleroy <arnaud.charleory@inra.fr>
 * SILEX:todo: 
 *       - Element: User agent URI
 *         Purpose: For now, generated user agent URIs are not unique. 
 *         Numbers must be add at the end of user agent URI
 *         if two user agents have the same family name and first name.
 *         .e.g:
 *              - First user: Jean Dupont-Marie http://www.phenome-fppn.fr/diaphen/id/agent/jean_dupont-marie
 *              - Second user: Jean Dupont-Marie http://www.phenome-fppn.fr/diaphen/id/agent/jean_dupont-marie01
 * \SILEX:todo
 * @update [Vincent Migot] 17 July 2019: Add syncronization on public methods to prevent URI duplication
 */
public class UriGenerator {    
    private static final String URI_CODE_ACTUATOR = "a";
    private static final String URI_CODE_SCIENTIFIC_OBJECT = "o";
    private static final String URI_CODE_IMAGE = "i";
    private static final String URI_CODE_METHOD = "m";
    private static final String URI_CODE_SENSOR = "s";
    private static final String URI_CODE_RADIOMETRIC_TARGET = "rt";
    private static final String URI_CODE_TRAIT = "t";
    private static final String URI_CODE_UNIT = "u";
    private static final String URI_CODE_VARIABLE = "v";
    private static final String URI_CODE_VECTOR = "v";
    private static final String URI_CODE_GERMPLASM = "g";

    private static final String PLATFORM_CODE =  PropertiesFileManager.getConfigFileProperty("sesame_rdf_config", "infrastructureCode") ;
    private static final String PLATFORM_URI = Contexts.PLATFORM.toString();
    private static final String PLATFORM_URI_ID = PLATFORM_URI + "id/";
    private static final String PLATFORM_URI_ID_AGENT = PLATFORM_URI_ID + "agent/";
    private static final String PLATFORM_URI_ID_ANNOTATION = PLATFORM_URI_ID + "annotation/";
    private static final String PLATFORM_URI_ID_EVENT = PLATFORM_URI_ID + "event/";
    private static final String PLATFORM_URI_ID_INSTANT = PLATFORM_URI_ID + "instant/";
    public static final String PLATFORM_URI_ID_METHOD = PLATFORM_URI_ID + "methods/" + URI_CODE_METHOD;
    private static final String PLATFORM_URI_ID_RADIOMETRIC_TARGET = PLATFORM_URI_ID + "radiometricTargets/";
    public static final String PLATFORM_URI_ID_TRAITS = PLATFORM_URI_ID + "traits/" + URI_CODE_TRAIT;
    public static final String PLATFORM_URI_ID_UNITS = PLATFORM_URI_ID + "units/" + URI_CODE_UNIT;
    public static final String PLATFORM_URI_ID_VARIABLES = PLATFORM_URI_ID + "variables/" + URI_CODE_VARIABLE;
    private static final String PLATFORM_URI_ID_VARIETY = PLATFORM_URI + "v/";
    private static final String PLATFORM_URI_ID_PROVENANCE = PLATFORM_URI_ID + "provenance/";
    public static final String PLATFORM_URI_ID_GERMPLASM = PLATFORM_URI_ID + "germplasm/" + URI_CODE_GERMPLASM;
    private static final String PLATFORM_URI_ID_ACCESSION = PLATFORM_URI_ID + "accession/";
    private static final String PLATFORM_URI_ID_PLANT_MATERIAL_LOT = PLATFORM_URI_ID + "lot/";
    private static final String PLATFORM_URI_ID_SPECIES = PLATFORM_URI_ID + "species/";
    private static final String PLATFORM_URI_ID_GENUS = PLATFORM_URI_ID + "genus/";
    
    private static final String EXPERIMENT_URI_SEPARATOR = "-";

    /**
     * Prevent URI generator to be instanciated
     */
    private UriGenerator() {}
    
    /**
     * Generates a new vector URI. a vector URI has the following pattern:
     * <prefix>:<year>/<unic_code>
     * <unic_code> = 1 letter type + 2 numbers year + auto incremented number
     * with 3 digits (per year).
     * @example http://www.phenome-fppn.fr/diaphen/2017/v1702
     * @param year the insertion year of the vector.
     * @return the new vector URI
     */
    private static String generateVectorUri(String year) {
        // get last vectors ID
        int vectorNumber = getNextVectorID(year);
        String numberOfVectors = Integer.toString(vectorNumber);
        String newVectorNumber;

        if (numberOfVectors.length() == 1) {
            newVectorNumber = "0" + numberOfVectors;
        } else {
            newVectorNumber = numberOfVectors;
        }
        return getVectorUriPatternByYear(year) + newVectorNumber;
    }
    
    /**
     * Internal variable to store the last vector ID by year
     */
    private static Map<String, Integer> vectorLastIDByYear = new HashMap<>();
    
    /**
     * Return the next vector ID by incrementing vectorLastIDByYear variable and initializing it before if needed
     * @return next vector ID
     */
    private static int getNextVectorID(String year) {
        if (!vectorLastIDByYear.containsKey(year)) {
            VectorDAO vectorDAO = new VectorDAO();
            vectorLastIDByYear.put(year, vectorDAO.getLastIdFromYear(year));
        }
        
        int vectorLastID = vectorLastIDByYear.get(year);
        vectorLastID++;
        vectorLastIDByYear.put(year, vectorLastID);
        return vectorLastID;
    }

    /**
     * Return vector uri pattern <prefix>:<year>/<unic_code>
     * @param year
     * @return prefix
     */
    public static String getVectorUriPatternByYear(String year) {
        return PLATFORM_URI + year + "/" + URI_CODE_VECTOR + year.substring(2, 4);
    }
    
    /**
     * Generates a new sensor URI. A sensor URI has the following pattern:
     * <prefix>:<year>/<unic_code>
     * <unic_code> = 1 letter type + 2 numbers year + auto incremented number
     * with 2 digits (per year) the year corresponds to the year of insertion in
     * the triplestore.
     * @example http://www.phenome-fppn.fr/diaphen/2017/s17002
     * @param year the insertion year of the sensor.
     * @return the new sensor URI
     */
    private static String generateSensorUri(String year) {
        int sensorNumber = getNextSensorID(year);
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
        return getSensorUriPatternByYear(year) + newSensorNumber;
    }
    
    /**
     * Internal variable to store the last sensor ID by year
     */
    private static Map<String, Integer> sensorLastIDByYear = new HashMap<>();
    
    /**
     * Return the next sensor ID by incrementing sensorLastIDByYear variable and initializing it before if needed
     * @return next sensor ID
     */
    private static int getNextSensorID(String year) {
        if (!sensorLastIDByYear.containsKey(year)) {
            SensorDAO sensorDAO = new SensorDAO();
            sensorLastIDByYear.put(year, sensorDAO.getLastIdFromYear(year));
        }
        
        int sensorLastID = sensorLastIDByYear.get(year);
        sensorLastID++;
        sensorLastIDByYear.put(year, sensorLastID);
        return sensorLastID;
    }
    
    /**
     * Return sensor uri pattern <prefix>:<year>/<unic_code>
     * @param year
     * @return prefix
     */
    public static String getSensorUriPatternByYear(String year) {
        return PLATFORM_URI + year + "/" + URI_CODE_SENSOR + year.substring(2, 4);
    }
    
    /**
     * Generate a new actuator URI. A actuator URI has the following pattern:
     * <prefix>:<year>/<unic_code>
     * <unic_code> = 1 letter type + 2 numbers year + auto incremented number
     * with 2 digits (per year) the year corresponds to the year of insertion in
     * the triplestore
     * @example http://www.opensilex.org/demo/2017/a17002
     * @param year the insertion year of the actuator.
     * @return the new actuator URI
     */
    private static String generateActuatorUri(String year) {
        int actuatorNumber = getNextActuatorID(year);
        String numberOfActuators = Integer.toString(actuatorNumber);
        String newActuatorNumber;
        switch (numberOfActuators.length()) {
            case 1:
                newActuatorNumber = "00" + numberOfActuators;
                break;
            case 2:
                newActuatorNumber = "0" + numberOfActuators;
                break;
            default:
                newActuatorNumber = numberOfActuators;
                break;
        }
        return getActuatorUriPatternByYear(year) + newActuatorNumber;        
    }

    /**
     * Internal variable to store the last actuator ID by year
     */
    private static Map<String, Integer> actuatorLastIDByYear = new HashMap<>();
    
    /**
     * Return the next actuator ID by incrementing actuatorLastIDByYear variable and initializing it before if needed
     * @return next actuator ID
     */
    private static int getNextActuatorID(String year) {
        if (!actuatorLastIDByYear.containsKey(year)) {
            ActuatorDAO actuatorDAO = new ActuatorDAO();
            actuatorLastIDByYear.put(year, actuatorDAO.getLastIdFromYear(year));
        }
        
        int actuatorLastID = actuatorLastIDByYear.get(year);
        actuatorLastID++;
        actuatorLastIDByYear.put(year, actuatorLastID);
        return actuatorLastID;
    }
    
    /**
     * Return actuator uri pattern <prefix>:<year>/<unic_code>
     * @param year
     * @return prefix
     */
    public static String getActuatorUriPatternByYear(String year) {
        return PLATFORM_URI + year + "/" + URI_CODE_ACTUATOR + year.substring(2, 4);
    }

    /**
     * Generates a new scientific object URI. URI has the following form:
     * <prefix>:<year>/<unic_code>
     * <unic_code> = 1 letter type + 2 numbers year + auto incremented number
     * with 6 digits (per year) the year corresponds to the year of insertion in
     * the triplestore.
     * @example http://www.phenome-fppn.fr/diaphen/2017/o17000001
     * @param year the insertion year of the agronomical object.
     * @return the new agronomical object URI
     */
    private static String generateScientificObjectUri(String year) {
        String agronomicalObjectId = Integer.toString(getNextScientificObjectID(year));

        while (agronomicalObjectId.length() < 6) {
            agronomicalObjectId = "0" + agronomicalObjectId;
        }
        
        return getScientificObjectUriPatternByYear(year) + agronomicalObjectId;        
    }

    /**
     * Internal variable to store the last scientifc object ID by year
     */
    private static Map<String, Integer> scientificObjectLastIDByYear = new HashMap<>();
    
    /**
     * Return the next scientifc object ID by incrementing scientificObjectLastIDByYear variable and initializing it before if needed
     * @return next scientific object ID
     */
    private static int getNextScientificObjectID(String year) {
        if (!scientificObjectLastIDByYear.containsKey(year)) {
            ScientificObjectRdf4jDAO scientificObjectDAO = new ScientificObjectRdf4jDAO();
            scientificObjectLastIDByYear.put(year, scientificObjectDAO.getLastScientificObjectIdFromYear(year));
        }
        
        int scientificObjectLastID = scientificObjectLastIDByYear.get(year);
        scientificObjectLastID++;
        scientificObjectLastIDByYear.put(year, scientificObjectLastID);
        return scientificObjectLastID;
    }
    
    /**
     * Return scientific object uri pattern <prefix>:<year>/<unic_code>
     * @param year
     * @return prefix
     */
    public static String getScientificObjectUriPatternByYear(String year) {
        return PLATFORM_URI + year + "/" + URI_CODE_SCIENTIFIC_OBJECT + year.substring(2, 4);
    }

    /**
     * Generates a new variable URI. A variable URI follows the pattern:
     * <prefix>:id/variables/<unic_code>
     * <unic_code> = 1 letter type + auto incremented number with 3 digits.
     * @example http://www.phenome-fppn.fr/diaphen/id/variables/v001
     * @return the new variable URI
     */
    private static String generateVariableUri() {
        // Generate variable URI based on next id
        String variableId = Integer.toString(getNextVariableID());        

        while (variableId.length() < 3) {
            variableId = "0" + variableId;
        }

        return PLATFORM_URI_ID_VARIABLES + variableId;
    }
    
    /**
     * Internal variable to store the last variable ID
     */
    private static Integer variableLastID;
    
    /**
     * Return the next variable ID by incrementing variableLastID variable and initializing it before if needed
     * @return next variable ID
     */
    private static int getNextVariableID() {
        if (variableLastID == null) {
            VariableDAO variableDAO = new VariableDAO();
            variableLastID = variableDAO.getLastId();
        }
        
        variableLastID++;
        
        return variableLastID;
    }

    /**
     * Generates a new trait URI. A trait URI follows the pattern:
     * <prefix>:id/traits/<unic_code>
     * <unic_code> = 1 letter type + auto incremented number with 3 digits.
     * @example http://www.phenome-fppn.fr/diaphen/id/traits/t001
     * @return the new trait URI
     */
    private static String generateTraitUri() {
        // Generate trait URI based on next id
        String traitId = Integer.toString(getNextTraitID());

        while (traitId.length() < 3) {
            traitId = "0" + traitId;
        }

        return PLATFORM_URI_ID_TRAITS + traitId;
    }
    
    /**
     * Internal variable to store the last trait ID
     */
    private static Integer traitLastID;

    /**
     * Return the next trait ID by incrementing traitLastID variable and initializing it before if needed
     * @return next trait ID
     */
    private static int getNextTraitID() {
        if (traitLastID == null) {
            TraitDAO traitDAO = new TraitDAO();
            traitLastID = traitDAO.getLastId();
        }
        
        traitLastID++;
        
        return traitLastID;
    }

    /**
     * Generates a new method URI. A method URI follows the pattern:
     * <prefix>:id/methods/<unic_code>
     * <unic_code> = 1 letter type + auto incremented number with 3 digits.
     * @example http://www.phenome-fppn.fr/diaphen/id/methods/m001
     * @return the new method URI
     */
    private static String generateMethodUri() {
        // Generate method URI based on next id
        String methodId = Integer.toString(getNextMethodID());

        while (methodId.length() < 3) {
            methodId = "0" + methodId;
        }

        return PLATFORM_URI_ID_METHOD + methodId;
    }
    
    /**
     * Internal variable to store the last method ID
     */
    private static Integer methodLastID;

    /**
     * Return the next method ID by incrementing methodLastID variable and initializing it before if needed
     * @return next method ID
     */
    private static int getNextMethodID() {
        if (methodLastID == null) {
            MethodDAO methodDAO = new MethodDAO();
            methodLastID = methodDAO.getLastId();
        }
        
        methodLastID++;
        
        return methodLastID;
    }

    /**
     * Generates a new unit URI. A unit URI follows the pattern:
     * <prefix>:id/units/<unic_code>
     * <unic_code> = 1 letter type + auto incremented number with 3 digits.
     * @example http://www.phenome-fppn.fr/diaphen/id/units/m001
     * @return the new unit URI
     */
    private static String generateUnitUri() {
        // Generate unit URI based on next id
        String unitId = Integer.toString(getNextUnitID());

        while (unitId.length() < 3) {
            unitId = "0" + unitId;
        }

        return PLATFORM_URI_ID_UNITS + unitId;
    }

    /**
     * Internal variable to store the last unit ID
     */
    private static Integer unitLastID;
    
    /**
     * Return the next unit ID by incrementing unitLastID variable and initializing it before if needed
     * @return next unit ID
     */
    private static int getNextUnitID() {
        if (unitLastID == null) {
            UnitDAO unitDAO = new UnitDAO();
            unitLastID = unitDAO.getLastId();
        }
        
        unitLastID++;
        
        return unitLastID;
    }
    
    /**
     * Generates a new radiometric target URI. A radiometric target URI follows the pattern: 
     * <prefix>:id/radiometricTargets/<unic_code>
     * <unic_code> = 2 letters type (rt) + auto incremented number with 3 digits.
     * @example http://www.phenome-fppn.fr/diaphen/id/radiometricTargets/rt001
     * @return The new radiometric target URI
     */
    private static String generateRadiometricTargetUri() {
        //1. Get the highest radiometric target id (i.e. the last inserted
        //radiometric target)
        RadiometricTargetDAO radiometricTargetDAO = new RadiometricTargetDAO();
        int lastID = radiometricTargetDAO.getLastId();
        
        //2. Generate radiometric target URI
        int newRadiometricTargetID = lastID + 1;
        String radiometricTargetID = Integer.toString(newRadiometricTargetID);
        
        while (radiometricTargetID.length() < 3) {
            radiometricTargetID = "0" + radiometricTargetID;
        }
        
        return PLATFORM_URI_ID_RADIOMETRIC_TARGET + URI_CODE_RADIOMETRIC_TARGET + radiometricTargetID;
    }

    /**
     * Generates a new variety URI. A variety URI follows the pattern:
     * <prefix>:v/<varietynameinlowercase>
     * @example http://www.phenome-fppn.fr/diaphen/v/dkc4814
     * @param variety the variety name
     * @return the new variety uri
     */
    private static String generateVarietyUri(String variety) {
        return PLATFORM_URI_ID_VARIETY + variety;
    }
    
    private static String generateAccessionUri(String accessionNumber) {
        return PLATFORM_URI_ID_ACCESSION + accessionNumber;
    }
    
    private static String generateLotUri(String seedlot) {
        return PLATFORM_URI_ID_PLANT_MATERIAL_LOT + seedlot;
    }
    
    private static String generateSpeciesUri(String species) {
        return PLATFORM_URI_ID_SPECIES + species;
    }
    
    private static String generateGenusUri(String genus) {
        return PLATFORM_URI_ID_GENUS + genus;
    }

    /**
     * Generates a new agent URI. A agent URI follows the pattern:
     * <prefix>:id/agent/<unic_code>
     * <unic_code> = firstnames concat with lastnames in lowercase
     * @example http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy
     * @param agentSuffixe the agent suffix e.g. arnaud_charleroy
     * @return the new agent URI
     */
    private static String generateAgentUri(String agentSuffixe) {
        // create URI
        return PLATFORM_URI_ID_AGENT + agentSuffixe;
    }

    /**
     * Generates a new annotation URI. A unit annotation follows the pattern:
     * <prefix>:id/annotation/<unic_code>
     * <unic_code> = 1 letter type + java.util.UUID.randomUUID(); 
     * @example http://www.phenome-fppn.fr/diaphen/id/annotation/e073961b-e766-4493-b98f-74a8b2846893
     * @return the new annotation URI
     */
    private static String generateAnnotationUri() {
        //1. check if URI already exists
        AnnotationDAO annotationDao = new AnnotationDAO();
        String newAnnotationUri = PLATFORM_URI_ID_ANNOTATION + UUID.randomUUID();
        while (annotationDao.existUri(newAnnotationUri)) {
            newAnnotationUri = PLATFORM_URI_ID_ANNOTATION + UUID.randomUUID();
        }

        return newAnnotationUri;
    }

    /**
     * Generates a new event URI. an event URI follows the pattern:
     * <prefix>:id/event/<unic_code>
     * <unic_code> = java.util.UUID.randomUUID();
     * @example http://www.phenome-fppn.fr/diaphen/id/event/e073961b-e766-4493-b98f-74a8b2846893
     * @return the new event URI
     */
    private static String generateEventUri() {
        // To check if URI already exists
        EventDAO eventDao = new EventDAO(null);
        String newEventUri = PLATFORM_URI_ID_EVENT + UUID.randomUUID();
        while (eventDao.existUri(newEventUri)) {
            newEventUri = PLATFORM_URI_ID_EVENT + UUID.randomUUID();
        }

        return newEventUri;
    }

    /**
     * Generates a new Instant URI. The URI follows the pattern:
     * <prefix>:id/instant/<unic_code>
     * <unic_code> = java.util.UUID.randomUUID();
     * @example http://www.phenome-fppn.fr/diaphen/id/instant/e073961b-e766-4493-b98f-74a8b2846893
     * @return the new URI
     */
    private static String generateInstantUri() {
        // To check if the URI already exists
        EventDAO timeDao = new EventDAO(null);
        String newInstantUri = PLATFORM_URI_ID_INSTANT + UUID.randomUUID();
        while (timeDao.existUri(newInstantUri)) {
            newInstantUri = PLATFORM_URI_ID_INSTANT + UUID.randomUUID();
        }

        return newInstantUri;
    }

    /**
     * Generates a new image URI. an image URI follows the pattern :
     * <prefix>:yyyy/<unic_code>
     * <unic_code> = 1 letter type (i) + 2 digits year + auto increment with 10
     * digit
     * @example http://www.phenome-fppn.fr/diaphen/2018/i180000000001
     * @param year the year of insertion of the image
     * @param lastGeneratedUri if a few URI has been generated but not inserted,
     * corresponds to the last generated URI
     * @return the new URI
     */
    private static String generateImageUri(String year, String lastGeneratedUri) {
        if (lastGeneratedUri == null) {
            ImageMetadataMongoDAO imageMongoDao = new ImageMetadataMongoDAO();
            long imagesNumber = imageMongoDao.getImagesCountOfCurrentYear();
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
     * Generates a new project URI. A project URI follows the pattern:
     * <prefix>:<projectAcronyme>
     * @example http://www.opensilex.org/demo/PA
     * @param projectAcronyme the project acronym
     * @return the new URI
     */
    private static String generateProjectUri(String projectAcronyme) throws Exception {
        //1. generates URI
        String projectUri = PLATFORM_URI + projectAcronyme;
        //2. check if URI exists
        ProjectDAO projectDAO = new ProjectDAO();
        if (projectDAO.existUri(projectUri)) {
            throw new AlreadyExists("The project uri " + projectUri + " already exist in the triplestore.");
        }
        
        return projectUri;
    }
    
    /**
     * Generates a new experiment URI. An experiment URI follows this pattern:
     * <prefix>:<unic_code>
     * <unic_code> = infrastructure code + 4 digits year + auto increment digit (per year)
     * @example http://www.opensilex.org/demo/DMO2019-1
     * @param campaign the year of the campaign of the experiment
     * @return the new URI
     */
    private static String generateExperimentUri(String campaign) {
        //1. Get the campaign last experiment URI
        Integer campaignLastExperimentUri = (new ExperimentSQLDAO()).getCampaignLastExperimentUri(campaign);
        //2. Generate the URI of the experiment
        
        Integer newExperimentNumber = campaignLastExperimentUri + 1;
        
        return PLATFORM_URI + PLATFORM_CODE + campaign + EXPERIMENT_URI_SEPARATOR + newExperimentNumber;
    }
    
    /**
     * Generate a new group URI. A group URI follows the pattern:
     * <prefix>:<groupName>
     * @example http://www.opensilex.org/demo/INRA-MISTEA.GAMMA
     * @param name the group name
     * @return the new generated URI
     * @throws Exception 
     */
    private static String generateGroupUri(String name) throws Exception {
        //1. Generate URI
        String groupUri = PLATFORM_URI + name;
        //2. Check if the generated URI already exists
        GroupDAO groupDao = new GroupDAO();
        Group group = new Group(groupUri);
        
        if (groupDao.existInDB(group)) {
            throw new AlreadyExists("The group uri " + groupUri + " already exist in the triplestore.");
        }
        
        return groupUri;
    }
    
    /**
     * Generates a new provenance URI. A provenance URI follows the pattern :
     * <prefix>:id/provenance/<timestamp>
     * @example http://www.opensilex.org/demo/id/provenance/019275849
     * @return the new generated uri
     * @throws Exception 
     */
    private static String generateProvenanceUri() {
        //Generates uri
        Instant instant = Instant.now();
        long timeStampMillis = instant.toEpochMilli();
        return PLATFORM_URI_ID_PROVENANCE + Long.toString(timeStampMillis);
    }


    /**
     * Generates a new data URI.
     * @example http://www.opensilex.org/id/data/1e9eb2fbacc7222d3868ae96149a8a16b32b2a1870c67d753376381ebcbb5937e78da502ee3f42d3828eaa8cab237f93
     * @param additionalInformation the key of the data
     * @return the new generated uri
     * @throws Exception 
     */
    private static String generateDataUri(String additionalInformation) throws Exception {
        // Define data URI with key hash  and random id to prevent collision
        String uri = Contexts.PLATFORM.toString() + "id/data/" + getUniqueHash(additionalInformation);
        
        return uri;
    }
    
    /**
     * Generates a new data file URI.
     * @example http://www.opensilex.org/id/dataFile/1e9eb2fbacc7222d3868ae96149a8a16b32b2a1870c67d753376381ebcbb5937e78da502ee3f42d3828eaa8cab237f93
     * @param additionalInformation the key of the data file
     * @return the new generated uri
     * @throws Exception 
     */
    private static String generateDataFileUri(String collection, String key) throws Exception {
        // Define data URI with key hash  and random id to prevent collision
        String uri = Contexts.PLATFORM.toString() + "id/dataFile/" + collection + "/" + getUniqueHash(key);
        
        return uri;
    }
    
    private static String getUniqueHash(String key) throws NoSuchAlgorithmException {
        // Generate SHA-256 hash
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
        
        // Convert hash to base32 string in lower case string and remove = padding sign
        Base32 base32 = new Base32();
        String encodedString = base32.encodeAsString(encodedhash).replaceAll("=", "").toLowerCase();
        
        // Generate UUID without '-' sign
        String randomId = UUID.randomUUID().toString().replaceAll("-", "");
        
        return encodedString + randomId;
    }
    
    /**
     * Generates scientific objects uris for a year. The number depends on the given numberOfUrisToGenerate.
     * @param year
     * @param numberOfUrisToGenerate
     * @return the list of uri generated
     */
    public synchronized static List<String> generateScientificObjectUris(String year, Integer numberOfUrisToGenerate) {
        if (year == null) {
            year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
        }

        List<String> scientificObjectUris = new ArrayList<>();
        
        for (int i = 0; i < numberOfUrisToGenerate; i++) {
            scientificObjectUris.add(generateScientificObjectUri(year));
        }
        
        return scientificObjectUris;
    }

    /**
     * Generates the URI of a new instance of instanceType.
     * This method is syncronized to prevent URI duplication in case of multiple thread request for new URIs
     * @param instanceType the RDF type of the instance (a concept URI)
     * @param year year of the creation of the element. If it is null, it will
     * be the current year
     * @param additionalInformation some additional information used for some
     * URI generators. (e.g. the variety name, or the last generated URI for the
     * images)
     * @return the generated URI
     * @throws java.lang.Exception
     */
    public synchronized static String generateNewInstanceUri(String instanceType, String year, String additionalInformation) 
            throws Exception {
        if (year == null) {
            year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
        }

        UriDAO uriDao = new UriDAO();

        if (uriDao.isSubClassOf(instanceType, Oeso.CONCEPT_VECTOR.toString())) {
            return generateVectorUri(year);
        } else if (uriDao.isSubClassOf(instanceType, Oeso.CONCEPT_SENSING_DEVICE.toString())) {
            return generateSensorUri(year);
        } else if (Oeso.CONCEPT_VARIABLE.toString().equals(instanceType)) {
            return generateVariableUri();
        } else if (Oeso.CONCEPT_TRAIT.toString().equals(instanceType)) {
            return generateTraitUri();
        } else if (Oeso.CONCEPT_METHOD.toString().equals(instanceType)) {
            return generateMethodUri();
        } else if (Oeso.CONCEPT_UNIT.toString().equals(instanceType)) {
            return generateUnitUri();
        } else if (uriDao.isSubClassOf(instanceType, Oeso.CONCEPT_SCIENTIFIC_OBJECT.toString())) {
            return generateScientificObjectUri(year);
        } else if (Oeso.CONCEPT_GENUS.toString().equals(instanceType)) {
            return generateGenusUri(additionalInformation);
        } else if (Oeso.CONCEPT_SPECIES.toString().equals(instanceType)) {
            return generateSpeciesUri(additionalInformation);
        } else if (Oeso.CONCEPT_VARIETY.toString().equals(instanceType)) {
            return generateVarietyUri(additionalInformation);
        } else if (Oeso.CONCEPT_ACCESSION.toString().equals(instanceType)) {
            return generateAccessionUri(additionalInformation);
        } else if (Oeso.CONCEPT_PLANT_MATERIAL_LOT.toString().equals(instanceType)) {
            return generateLotUri(additionalInformation);            
        } else if (uriDao.isSubClassOf(instanceType, Oeso.CONCEPT_IMAGE.toString())) {
            return generateImageUri(year, additionalInformation);
        } else if (instanceType.equals(Foaf.CONCEPT_AGENT.toString()) 
                || uriDao.isSubClassOf(instanceType, Foaf.CONCEPT_AGENT.toString())) {
            return generateAgentUri(additionalInformation);
        } else if (instanceType.equals(Oeso.CONCEPT_ANNOTATION.toString())) {
            return generateAnnotationUri();
        } else if (instanceType.equals(Oeso.CONCEPT_RADIOMETRIC_TARGET.toString())) {
            return generateRadiometricTargetUri();
        } else if (instanceType.equals(Oeso.CONCEPT_PROJECT.toString())) {
            return generateProjectUri(additionalInformation);
        } else if (instanceType.equals(Oeso.CONCEPT_EXPERIMENT.toString())) {
            return generateExperimentUri(year);
        } else if (instanceType.equals(Foaf.CONCEPT_GROUP.toString())) {
            return generateGroupUri(additionalInformation);
        } else if (instanceType.equals(Oeso.CONCEPT_PROVENANCE.toString())) {
            return generateProvenanceUri();
        } else if (instanceType.equals(Oeso.CONCEPT_DATA.toString())) {
            return generateDataUri(additionalInformation);
        } else if (uriDao.isSubClassOf(instanceType, Oeev.Event.getURI())) {
            return generateEventUri();
        } else if (instanceType.equals(Time.Instant.toString())) {
            return generateInstantUri();
        } else if (instanceType.equals(Oeso.CONCEPT_DATA_FILE.toString())) {
            return generateDataFileUri(year, additionalInformation);
        } else if (instanceType.equals(Oeso.CONCEPT_ACTUATOR.toString())) {
            return generateActuatorUri(year);
        }
        return null;
    }
    
}
