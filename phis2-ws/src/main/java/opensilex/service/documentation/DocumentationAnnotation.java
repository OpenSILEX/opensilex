//******************************************************************************
//                           DocumentationAnnotation.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: August 2016
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, eloan.lagier@inra.fr, 
//          anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.documentation;

import javax.inject.Singleton;
import opensilex.service.PropertiesFileManager;

/**
 * Documentation annotations.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Singleton
public final class DocumentationAnnotation {

    // Default page number is 0
    public static final String PAGE = "Current page number";
    
    /**
     * To be updated if the variable in service.properties is updated
     * @see service.properties
     */
    public static final String PAGE_SIZE = "Number of elements per page (limited to 150000)";

    public static final String PAGE_SIZE_MONGO = "Number of elements per page (limited to 1000000)";

    // User messages
    public static final String ERROR_SEND_DATA = "Server error. Cannot send data.";
    public static final String ERROR_FETCH_DATA = "Server error. Cannot fetch data.";
    public static final String BAD_USER_INFORMATION = "Bad informations send by user";
    public static final String ACCES_TOKEN = "Access token given";
    public static final String SQL_ERROR_FETCH_DATA = "SQL Error can't fetch results";
    public static final String NO_RESULTS = "No data found";
    public static final String USER_NOT_AUTHORIZED = "You aren't authorized to fetch the result of this ressource call";
    public static final String FILE_NOT_FOUND = "File not found";
    
    public static final String SWAGGER_DOCUMENTATION_HEADER
        = "This page describes the methods allowed by this web service. <p style=\"color: red;\"> You must read the paragraph below before use it !</p>"
        + "<br>"
        + "<ol>"
        + "<li>"
        + "<b> 1. You must first retrieve an acces token using the \"token\" call (fill with your PHIS-SILEX username and password)</b> and after you will be able to use other service calls.</li>"
        + "<li>"
        + "<b> 2. You must fill the sessionId parameter</b> with the created access token on each call. <p style=\"color: red;\">This token is available during " + Integer.valueOf(PropertiesFileManager.getConfigFileProperty("service", "sessionTime")) + " seconds.</p> This time will be reload at each use in order to keep the token valid without retrieve a new one.</li>"
        + "</ol>"
        + "<br>"
        + "<i>The response call <b>example values shown</b> in this api documentation represent the <b>data array</b> which is located <b>in the response result object</b> <p style=\"color: red;\">except for the token call.</p><i/><br>"
        + "<b>The token also include the response object header. </b>"
        + "For more information, the <b>Response object definition</b> is available at <b><a href=\"http://docs.brapi.apiary.io/#introduction/structure-of-the-response-object:\">Brapi response object</a></b>.";

    public static final String ACTUATOR_POST_DEFINITION = "JSON format to insert actuators";
    public static final String ACTUATOR_URI_DEFINITION = "An actuator URI (Unique Resource Identifier)";
    
    public static final String ENVIRONMENT_POST_DEFINITION = "JSON format to insert environment";
    
    public static final String DATA_POST_DEFINITION = "JSON format to insert data";
    
    public static final String EXPERIMENT_URI_DEFINITION = "An experiment URI (Unique Resource Identifier)";
    public static final String EXPERIMENT_POST_DATA_DEFINITION = "JSON format of experiment data";

    public static final String PROJECT_URI_DEFINITION = "A project URI (Unique Resource Identifier)";
    public static final String PROJECT_POST_DATA_DEFINITION = "JSON format of project data";

    public static final String SCIENTIFIC_OBJECT_URI_DEFINITION = "Scientific object URI (Unique Resource Identifier)";
    public static final String SCIENTIFIC_OBJECT_POST_DATA_DEFINITION = "JSON format of scientific object data";
    public static final String EXAMPLE_SCIENTIFIC_OBJECT_ALIAS = "Plot01";

    public static final String GROUP_URI_DEFINITION = "A group uri";
    public static final String GROUP_POST_DATA_DEFINITION = "JSON format of group data";

    public static final String USER_EMAIL_DEFINITION = "A user email";
    public static final String USER_POST_DATA_DEFINITION = "JSON format of user data";

    public static final String VARIABLE_POST_DATA_DEFINITION = "JSON format of variable data";
    public static final String VARIABLE_URI_DEFINITION = "A variable URI (Unique Resource Identifier)";
    public static final String VARIABLE_CALL_MESSAGE = "Retrieve the list of all variables available in the system";
    public static final String VARIABLE_DETAILS_CALL_MESSAGE = "Retrieve variable details by id";    
    
    public static final String TRAIT_POST_DATA_DEFINITION = "JSON format of trait";
    public static final String TRAIT_URI_DEFINITION = "A trait URI (Unique Resource Identifier)";
    public static final String TRAIT_CALL_MESSAGE = "Retrieve the list of all traits available in the system";
    public static final String TRAIT_DETAILS_CALL_MESSAGE = "Retrieve trait details by id";
   
    public static final String TRIPLET_POST_DATA_DEFINITION = "JSON format of a triplet";

    public static final String ANNOTATION_POST_DATA_DEFINITION = "JSON format of an annotation";

    public static final String METHOD_POST_DATA_DEFINITION = "JSON format of method";
    public static final String METHOD_URI_DEFINITION = "A method URI (Unique Resource Identifier)";

    public static final String UNIT_POST_DATA_DEFINITION = "JSON format of unit";
    public static final String UNIT_URI_DEFINITION = "A unit URI (Unique Resource Identifier)";

    public static final String SENSOR_URI_DEFINITION = "a sensor URI (Unique Resource Identifier)";
    public static final String SENSOR_POST_DEFINITION = "JSON format of sensor data";
    public static final String SENSOR_PROFILE_POST_DEFINITION = "JSON format of sensor profile data";
    public static final String LINK_VARIABLES_DEFINITION = "List of variables uris";
    public static final String LINK_SENSORS_DEFINITION = "List of sensors uris.";

    public static final String VARIABLES_DEFINITION = "A variable or comma-separated variables list";

    public static final String VECTOR_POST_DEFINITION = "JSON format of vector data";
    public static final String VECTOR_RDF_TYPE_DEFINITION = "A vector rdf type URI";
    
    public static final String RADIOMETRIC_TARGET_POST_DEFINITION = "JSON format of radiometric target data";
    
    public static final String PROVENACE_POST_DEFINITION = "JSON format of provenance";
    
    public static final String EVENT_POST_DEFINITION = "JSON format of a list of events without URIs";
    public static final String EVENT_PUT_DEFINITION = "JSON format of a list of events with URIs";

    public static final String ADMIN_ONLY_NOTES = "This can only be done by a PHIS-SILEX admin.";
    public static final String USER_ONLY_NOTES = "This can only be done by a PHIS-SILEX user.";

    public static final String DOCUMENT_URI_DEFINITION = "A document URI (Unique Resource Identifier)";

    public static final String LAYER_POST_DATA_DEFINITION = "JSON format of requested layer";

    public static final String RAW_DATA_POST_DATA_DEFINITION = "JSON format of raw data";

    public static final String INFRASTRUCTURE_URI_DEFINITION = "An infrastructure URI (Unique Resource Identifier)";
    
    public static final String CONCEPT_URI_DEFINITION = "A concept URI (Unique Resource Identifier)";
    public static final String DEEP ="true or false deppending if you want instances of concept progenity";
    
    public static final String CALL_DATATYPE_DEFINITION = "The data format supported by the call";

    // Global examples
    public static final String EXAMPLE_DATETIME = "2017-06-15 10:51:00+0200";
    public static final String EXAMPLE_XSDDATETIME = "2017-06-15T10:51:00+0200";
    public static final String EXAMPLE_DATE = "2017-06-15";

    // Specific examples
    public static final String EXAMPLE_ACTUATOR_RDF_TYPE = "http://www.opensilex.org/vocabulary/oeso#Actuator";
    public static final String EXAMPLE_ACTUATOR_URI = "http://www.opensilex.org/opensilex/2019/a19001";
    public static final String EXAMPLE_DATA_FILE_WEB_PATH = "http://www.opensilex.org/images/example.jpg";
    public static final String EXAMPLE_EXPERIMENT_URI = "http://www.opensilex.org/demo/DMO2012-1";
    public static final String EXAMPLE_EXPERIMENT_START_DATE = EXAMPLE_DATETIME;
    public static final String EXAMPLE_EXPERIMENT_END_DATE = EXAMPLE_DATETIME;
    public static final String EXAMPLE_EXPERIMENT_FIELD = "field";
    public static final String EXAMPLE_EXPERIMENT_PLACE = "place";
    public static final String EXAMPLE_EXPERIMENT_ALIAS = "alias";
    public static final String EXAMPLE_EXPERIMENT_KEYWORDS = "keywords";
    public static final String EXAMPLE_EXPERIMENT_CAMPAIGN = "2012";
    public static final String EXAMPLE_EXPERIMENT_CROP_SPECIES = "maize";
    
    public static final String EXAMPLE_ENVIRONMENT_VALUE = "1.3";

    public static final String EXAMPLE_FILE_INFORMATION_CHECKSUM = "106fa487baa1728083747de1c6df73e9";
    public static final String EXAMPLE_FILE_INFORMATION_EXTENSION = "jpg";

    public static final String EXAMPLE_SCIENTIFIC_OBJECT_LABEL = "POZ_1";
    public static final String EXAMPLE_SCIENTIFIC_OBJECT_POLYGON = "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))";
    public static final String EXAMPLE_SCIENTIFIC_OBJECT_URI = "http://www.opensilex.org/demo/2018/o18000076";
    public static final String EXAMPLE_SCIENTIFIC_OBJECT_TYPE = "http://www.opensilex.org/vocabulary/oeso#Plot";

    public static final String EXAMPLE_IMAGE_TYPE = "http://www.opensilex.org/vocabulary/oeso#HemisphericalImage";
    public static final String EXAMPLE_IMAGE_URI = "http://www.opensilex.org/demo/2017/i170000000000";
    public static final String EXAMPLE_IMAGE_DATE = EXAMPLE_DATETIME;
    public static final String EXAMPLE_IMAGE_CONCERNED_ITEMS = EXAMPLE_SCIENTIFIC_OBJECT_URI + ";" + EXAMPLE_SCIENTIFIC_OBJECT_URI;

    public static final String EXAMPLE_PROJECT_URI = "http://www.opensilex.org/demo/projectTest";
    public static final String EXAMPLE_PROJECT_NAME = "projectTest";
    public static final String EXAMPLE_PROJECT_ACRONYME = "P T";
    public static final String EXAMPLE_PROJECT_SUBPROJECT_TYPE = "subproject type";
    public static final String EXAMPLE_PROJECT_FINANCIAL_SUPPORT = "financial support";
    public static final String EXAMPLE_PROJECT_FINANCIAL_NAME = "financial name";
    public static final String EXAMPLE_PROJECT_DATE_START = "2015-07-07";
    public static final String EXAMPLE_PROJECT_DATE_END = "2016-07-07";
    public static final String EXAMPLE_PROJECT_KEYWORDS = "keywords";
    public static final String EXAMPLE_PROJECT_PARENT_PROJECT = "parent project";
    public static final String EXAMPLE_PROJECT_WEBSITE = "http://example.com";
    public static final String EXAMPLE_PROJECT_TYPE = "project type";
    
    public static final String EXAMPLE_PROPERTY_RDF_TYPE = "http://xmlns.com/foaf/0.1/Agent";
    public static final String EXAMPLE_PROPERTY_RELATION = "http://www.opensilex.org/vocabulary/2018#hasContact";
    public static final String EXAMPLE_PROPERTY_VALUE = "http://www.opensilex.org/demo/id/agent/marie_dupond";

    public static final String EXAMPLE_PROVENANCE_URI = "http://www.opensilex.org/demo/2018/pv181515071552";
    public static final String EXAMPLE_PROVENANCE_DATE = EXAMPLE_DATE;
    public static final String EXAMPLE_PROVENANCE_LABEL = "PROV2019-LEAF";
    public static final String EXAMPLE_PROVENANCE_COMMENT = "In this provenance we have count the number of leaf per plant";
    public static final String EXAMPLE_PROVENANCE_METADATA = "{ \"SensingDevice\" : \"http://www.opensilex.org/demo/s001\",\n" +
                                                               "\"Vector\" : \"http://www.opensilex.org/demo/v001\"}";
    public static final String EXAMPLE_PROVENANCE_METADATA_FILTER = "{ \"metadata.SensingDevice\" : \"http://www.opensilex.org/demo/s001\",\n" +
                                                               "\"metadata.Vector\" : \"http://www.opensilex.org/demo/v001\"}";

    public static final String EXAMPLE_GROUP_URI = "http://phenome-fppn.fr/demo/INRA-MISTEA-GAMMA";
    public static final String EXAMPLE_GROUP_NAME = "INRA-MISTEA-GAMMA";
    public static final String EXAMPLE_GROUP_LEVEL = "Owner";
    public static final String EXAMPLE_GROUP_AVAILABLE = "true";

    public static final String EXAMPLE_USER_EMAIL = "admin@opensilex.org";
    public static final String EXAMPLE_USER_PASSWORD = "21232f297a57a5a743894a0e4a801fc3";
    public static final String EXAMPLE_USER_FIRST_NAME = "Marie";
    public static final String EXAMPLE_USER_FAMILY_NAME = "Dupont";
    public static final String EXAMPLE_USER_ADDRESS = "2 place Pierre Viala, Montpellier";
    public static final String EXAMPLE_USER_PHONE = "0400000000";
    public static final String EXAMPLE_USER_AFFILIATION = "affiliation";
    public static final String EXAMPLE_USER_ORCID = "orcid";
    public static final String EXAMPLE_USER_ADMIN = "true";
    public static final String EXAMPLE_USER_AVAILABLE = "true";
    public static final String EXAMPLE_USER_URI = "http://www.opensilex.org/demo/id/agent/marie_dupond";

    public static final String EXAMPLE_DATA_INCERTITUDE = "0.4";
    public static final String EXAMPLE_DATA_VALUE = "3.0000000";
    public static final String EXAMPLE_DATA_URI = "http://www.opensilex.org/1e9eb2fbacc7222d3868ae96149a8a16b32b2a1870c67d753376381ebcbb5937/e78da502-ee3f-42d3-828e-aa8cab237f93";
    
    public static final String EXAMPLE_DOCUMENT_URI = "http://www.opensilex.org/demo/documents/documente597f57ba71d421a86277d830f4b9885";
    public static final String EXAMPLE_DOCUMENT_TYPE = "http://www.opensilex.org/vocabulary/oeso#ScientificDocument";
    public static final String EXAMPLE_DOCUMENT_CREATOR = "John Doe";
    public static final String EXAMPLE_DOCUMENT_LANGUAGE = "fr";
    public static final String EXAMPLE_DOCUMENT_CREATION_DATE = "2017-07-07";
    public static final String EXAMPLE_DOCUMENT_EXTENSION = "png";
    public static final String EXAMPLE_DOCUMENT_TITLE = "title";
    public static final String EXAMPLE_DOCUMENT_CONCERNED_ITEM_TYPE_URI = "http://www.opensilex.org/vocabulary/oeso#Experiment";
    public static final String EXAMPLE_DOCUMENT_STATUS = "linked";

    public static final String EXAMPLE_SENSOR_URI = "http://www.opensilex.org/demo/2018/s18001";
    public static final String EXAMPLE_SENSOR_RDF_TYPE = "http://www.opensilex.org/vocabulary/oeso#Sensor";
    public static final String EXAMPLE_SENSOR_LABEL = "par03_p";
    public static final String EXAMPLE_SENSOR_BRAND = "Skye Instruments";
    public static final String EXAMPLE_SENSOR_MODEL = "m001";
    public static final String EXAMPLE_SENSOR_VARIABLE = "http://www.opensilex.org/demo/id/variables/v001";
    public static final String EXAMPLE_SENSOR_IN_SERVICE_DATE = EXAMPLE_DATE;
    public static final String EXAMPLE_SENSOR_DATE_OF_PURCHASE = EXAMPLE_DATE;
    public static final String EXAMPLE_SENSOR_DATE_OF_LAST_CALIBRATION = EXAMPLE_DATE;
    public static final String EXAMPLE_SENSOR_SERIAL_NUMBER = "A1E345F32";

    public static final String EXAMPLE_SHOOTING_CONFIGURATION_TIMESTAMP = "1512744238";
    public static final String EXAMPLE_SHOOTING_CONFIGURATION_POSITION = "POINT(0, 0)";
    public static final String EXAMPLE_SHOOTING_CONFIGURATION_DATE = EXAMPLE_EXPERIMENT_START_DATE;

    public static final String EXAMPLE_SPECIES_RDF_TYPE = "http://www.opensilex.org/vocabulary/oeso#Species";
    public static final String EXAMPLE_SPECIES_URI = "http://www.opensilex.org/id/species/maize";
    public static final String EXAMPLE_SPECIES_HAS_SPECIES = "http://www.opensilex.org/vocabulary/oeso#hasSpecies";
    public static final String EXAMPLE_SPECIES_LABEL = "Maize";

    public static final String EXAMPLE_TRAIT_URI = "http://www.opensilex.org/demo/id/traits/t001";
    public static final String EXAMPLE_TRAIT_LABEL = "Height";

    public static final String EXAMPLE_TRIPLET_SUBJECT = "http://www.opensilex.org/demo/DIA2018-2";
    public static final String EXAMPLE_TRIPLET_PROPERTY = "http://www.opensilex.org/vocabulary/oeso#hasDocument";
    public static final String EXAMPLE_TRIPLET_OBJECT = "http://www.opensilex.org/demo/documents/documente597f57ba71d421a86277d830f4b9885";
    public static final String EXAMPLE_TRIPLET_OBJECT_LANGUAGE = "en-US";
    public static final String EXAMPLE_TRIPLET_OBJECT_TYPE = "uri";
    public static final String EXAMPLE_TRIPLET_GRAPH = "http://www.opensilex.org/demo/DIA2018-2";

    public static final String EXAMPLE_METHOD_URI = "http://www.opensilex.org/demo/id/methods/m001";
    public static final String EXAMPLE_METHOD_LABEL = "comptage";

    public static final String EXAMPLE_UNIT_URI = "http://www.opensilex.org/demo/id/units/u001";
    public static final String EXAMPLE_UNIT_LABEL = "cm";

    public static final String EXAMPLE_VARIABLE_URI = "http://www.opensilex.org/demo/id/variable/v0000001";
    public static final String EXAMPLE_VARIABLE_LABEL = "LAI";
        
    public static final String EVENT_URI_DEFINITION = "An event URI (Unique Resource Identifier)";
    public static final String EXAMPLE_EVENT_URI = "http://www.opensilex.org/id/event/12590c87-1c34-426b-a231-beb7acb33415";
    public static final String EXAMPLE_EVENT_TYPE = "http://www.opensilex.org/vocabulary/oeev#MoveFrom";
    public static final String EXAMPLE_EVENT_DESCRIPTION = "The pest attack lasted 20 minutes";
    public static final String EXAMPLE_EVENT_CONCERNED_ITEM_URI = "http://www.opensilex.org/m3p/arch/2017/c17000242";
    public static final String EXAMPLE_EVENT_CONCERNED_ITEM_LABEL = "Plot Lavalette";
    public static final String EXAMPLE_EVENT_DATE = "2017-09-08T12:00:00+01:00";
    public static final String EXAMPLE_EVENT_SEARCH_START_DATE = "2017-09-08T12:00:00+01:00";
    public static final String EXAMPLE_EVENT_SEARCH_END_DATE = "2019-10-08T12:00:00+01:00";

    public static final String EXAMPLE_VECTOR_URI = "http://www.opensilex.org/demo/2018/v1801";
    public static final String EXAMPLE_VECTOR_RDF_TYPE = "http://www.opensilex.org/vocabulary/oeso#UAV";
    public static final String EXAMPLE_VECTOR_LABEL = "par03_p";
    public static final String EXAMPLE_VECTOR_BRAND = "Skye Instruments";
    public static final String EXAMPLE_VECTOR_SERIAL_NUMBER = "A1E345F32";
    public static final String EXAMPLE_VECTOR_IN_SERVICE_DATE = EXAMPLE_DATE;
    public static final String EXAMPLE_VECTOR_DATE_OF_PURCHASE = EXAMPLE_DATE;
    public static final String EXAMPLE_VECTOR_PERSON_IN_CHARGE = EXAMPLE_USER_EMAIL;

    public static final String EXAMPLE_CONCEPT_URI = "http://www.opensilex.org/vocabulary/oeso#Document";
    public static final String EXAMPLE_DEEP = "true";
    public static final String EXAMPLE_SIBLING_URI = "http://www.opensilex.org/vocabulary/oeso#ScientificDocument";

    public static final String EXAMPLE_CONCEPT_LABEL = "'document'@en";

    public static final String EXAMPLE_RDFTYPE_URI = "http://www.opensilex.org/vocabulary/oeso#ScientificDocument";
    public static final String EXAMPLE_INSTANCE_URI = "http://www.opensilex.org/demo/documents/document90fb96ace2894cdb9f4575173d8ed4c9";

    public static final String EXAMPLE_WAS_GENERATED_BY_DOCUMENT = EXAMPLE_DOCUMENT_URI;
    public static final String EXAMPLE_WAS_GENERATED_BY_DESCRIPTION = "Phenoscript v1.3";

    public static final String EXAMPLE_TOKEN_JWT_CLIENTID = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJQaGlzIiwic3ViIjoibW9yZ2FuZS52aWRhbEBpbnJhLmZyIiwiaWF0IjoxNTIwMjU5NjY5LCJleHAiOjE1MjM4NTYwNjl9.PjdxAb_U8fJaR6XwkNBa011XLFMM8psi0ym5VpfTV_fLkkIp6ZKFDOM-kTFAjNfPSR8tVUiU90pTkKSje9Ib9T629gA7Xw-8006smUL-n-ZIaQZ18mxzVb6jsVP6tOcJjjlucAmWZIhLIcT1e1KbOlBfToHpdRgOhDWkdkwENVc7n2TAcq-eLIJpcwQeDsCel7Ea2hssCxg4p8jCs68S43wg2mPTudTclW5_Q3HzKpf-DmPBFf6MMZmPtEAqi2aFfOoLp_8GDh92ywPOHJUdwvT9UkL87ELM7j5C8zrDE_CZAC-1IZdINE1KTTp36sScCJBsbm5DGLeIWF8g0e1vug";
    public static final String EXAMPLE_TOKEN_JWT_GRANTTYPE = "jwt";
    
    public static final String EXAMPLE_CALL_DATATYPE = "json";
  
    public static final String EXAMPLE_ANNOTATION_URI = "http://www.opensilex.org/platform/id/annotation/8247af37-769c-495b-8e7e-78b1141176c2";
    public static final String EXAMPLE_ANNOTATION_TARGET = "http://www.opensilex.org/demo/id/variable/v0000001";
    public static final String EXAMPLE_ANNOTATION_MOTIVATED_BY = "http://www.w3.org/ns/oa#commenting";
    public static final String EXAMPLE_ANNOTATION_BODY_VALUE = "Ustilago maydis infection";
    public static final String EXAMPLE_ANNOTATION_CREATOR = "http://www.opensilex.org/demo/id/agent/marie_dupond";

    // API global parameters
    public static final String EXAMPLE_SORTING_ALLOWABLE_VALUES = "asc,desc";

    public static final String EXAMPLE_INFRASTRUCTURE_URI = "http://www.opensilex.org/demo";
    public static final String EXAMPLE_INFRASTRUCTURE_RDF_TYPE = "http://www.opensilex.org/vocabulary/oeso#Infrastructure";
    public static final String EXAMPLE_INFRASTRUCTURE_LABEL = "EMPHASIS";
    
    public static final String EXAMPLE_LANGUAGE = "en";
    
    public static final String EXAMPLE_SKOS_REFERECENCE_URI = "http://purl.obolibrary.org/obo/CO_125_0000002";
}