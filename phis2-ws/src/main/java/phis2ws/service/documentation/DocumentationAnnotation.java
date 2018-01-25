//**********************************************************************************************
//                                       DocumentationAnnotation.java 
//
// Author(s): Arnaud CHARLEROY, Morgane VIDAL, Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact: eloan.lagier@inra.fr, arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Janvier, 25 2018
// Subject: A class which group documentation informations ( try message bundle for the next version)
//***********************************************************************************************
package phis2ws.service.documentation;

import javax.inject.Singleton;
import phis2ws.service.PropertiesFileManager;

@Singleton
public final class DocumentationAnnotation {

    /**
     * Par defaut la page est 0
     */
    public static final String PAGE = "Current page number";
    /**
     * A mettre a jour si changement de la variable dans service properties
     *
     * @see service.properties
     */
    public static final String PAGE_SIZE = "Number of elements per page (limited to 150000)";
    
     public static final String PAGE_SIZE_MONGO = "Number of elements per page (limited to 1000000)";

    // user messages
    public static final String ERROR_SEND_DATA = "Server error. Cannot send data.";
    public static final String ERROR_FETCH_DATA = "Server error. Cannot fetch data.";
    public static final String BAD_USER_INFORMATION = "Bad informations send by user";
    public static final String ACCES_TOKEN = "Access token given";
    public static final String SQL_ERROR_FETCH_DATA = "SQL Error can't fetch results";
    public static final String NO_RESULTS = "No data found";
    public static final String USER_NOT_AUTHORIZED = "You aren't authorized to fetch the result of this ressource call";

    public static final String SWAGGER_DOCUMENTATION_HEADER  =
            "This page describes the methods allowed by this web service. <p style=\"color: red;\"> You must read the paragraph below before use it !</p>"
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

    // defintions des variables    
    public static final String EXPERIMENT_URI_DEFINITION = "An experiment URI (Unique Resource Identifier)";
    public static final String EXPERIMENT_POST_DATA_DEFINITION = "JSON format of experiment data";
    
    public static final String PROJECT_URI_DEFINITION = "A project URI (Unique Resource Identifier)";
    public static final String PROJECT_POST_DATA_DEFINITION = "JSON format of project data";
    
    public static final String AGRONOMICAL_OBJECT_URI_DEFINITION = "Agronomical object URI (Unique Resource Identifier)";
    public static final String AGRONOMICAL_OBJECT_POST_DATA_DEFINITION = "JSON format of agronomical object data";
    
    public static final String GROUP_URI_DEFINITION = "A group uri";
    public static final String GROUP_POST_DATA_DEFINITION = "JSON format of group data";
    
    public static final String USER_EMAIL_DEFINITION = "A user email";
    public static final String USER_POST_DATA_DEFINITION = "JSON format of user data";
    
    public static final String VARIABLE_POST_DATA_DEFINITION = "JSON format of variable data";
    public static final String VARIABLE_URI_DEFINITION = "A variable URI (Unique Resource Identifier)";
    
    public static final String TRAIT_POST_DATA_DEFINITION = "JSON format of trait";
    public static final String TRAIT_URI_DEFINITION = "A trait URI (Unique Resource Identifier)";
    
    public static final String METHOD_POST_DATA_DEFINITION = "JSON format of method";
    public static final String METHOD_URI_DEFINITION = "A method URI (Unique Resource Identifier)";
    
    public static final String UNIT_POST_DATA_DEFINITION = "JSON format of unit";
    public static final String UNIT_URI_DEFINITION = "A unit URI (Unique Resource Identifier)";
    
    public static final String VARIABLES_DEFINITION = "A variable or comma-separated variables list";
    
    public static final String ADMIN_ONLY_NOTES = "This can only be done by a PHIS-SILEX admin.";
    public static final String USER_ONLY_NOTES = "This can only be done by a PHIS-SILEX user.";
    
    public static final String DOCUMENT_URI_DEFINITION = "A document URI (Unique Resource Identifier)";
    
    public static final String LAYER_POST_DATA_DEFINITION = "JSON format of requested layer";
    
    public static final String RAW_DATA_POST_DATA_DEFINITION = "JSON format of raw data";
    
    public static final String CONCEPT_URI_DEFINITION ="A concept URI (Unique Resource Identifier)";
    public static final String DEEP ="true or false deppending if you want instances of concept progenity";
    
    // Exemples de variables
    public static final String EXAMPLE_EXPERIMENT_URI = "http://phenome-fppn.fr/diaphen/DIA2012-1";
    public static final String EXAMPLE_EXPERIMENT_START_DATE = "2015-07-07 00:00:00+02";
    public static final String EXAMPLE_EXPERIMENT_END_DATE = "2015-08-07 00:00:00+02";
    public static final String EXAMPLE_EXPERIMENT_FIELD = "field";
    public static final String EXAMPLE_EXPERIMENT_PLACE = "place";
    public static final String EXAMPLE_EXPERIMENT_ALIAS = "alias";
    public static final String EXAMPLE_EXPERIMENT_KEYWORDS = "keywords";  
    public static final String EXAMPLE_EXPERIMENT_CAMPAIGN = "2012";
    
    public static final String EXAMPLE_AGRONOMICAL_OBJECT_URI = "http://phenome-fppn.fr/phis_field/ao1";
    
    public static final String EXAMPLE_PROJECT_URI = "http://phenome-fppn.fr/phis_field/projectTest";
    public static final String EXAMPLE_PROJECT_NAME = "projectTest";
    public static final String EXAMPLE_PROJECT_ACRONYME = "P T";
    public static final String EXAMPLE_PROJECT_SUBPROJECT_TYPE = "subproject type";
    public static final String EXAMPLE_PROJECT_FINANCIAL_SUPPORT = "financial support";
    public static final String EXAMPLE_PROJECT_FINANCIAL_NAME = "financial name";
    public static final String EXAMPLE_PROJECT_DATE_START = "2015-07-07";
    public static final String EXAMPLE_PROJECT_DATE_END = "2016-07-07";
    public static final String EXAMPLE_PROJECT_KEYWORDS = "keywords";
    public static final String EXAMPLE_PROJECT_PARENT_PROJECT = "parent project";
    public static final String EXAMPLE_PROJECT_SCIENTIFIC_CONTACT = "Morgane Vidal";
    public static final String EXAMPLE_PROJECT_ADMINISTRATIVE_CONTACT = "Morgane Vidal";
    public static final String EXAMPLE_PROJECT_PROJECT_COORDINATOR = "Morgane Vidal";
    public static final String EXAMPLE_PROJECT_WEBSITE = "http://example.com";
    public static final String EXAMPLE_PROJECT_TYPE = "project type";
    
    public static final String EXAMPLE_GROUP_URI = "http://phenome-fppn.fr/mauguio/INRA-MISTEA-GAMMA";
    public static final String EXAMPLE_GROUP_NAME = "INRA-MISTEA-GAMMA";
    public static final String EXAMPLE_GROUP_LEVEL = "Owner";
    public static final String EXAMPLE_GROUP_AVAILABLE = "true";
    
    public static final String EXAMPLE_USER_EMAIL = "morgane.vidal@inra.fr";
    public static final String EXAMPLE_USER_PASSWORD = "b30aebada8cb09d2cb686a1fdaec38fd";
    public static final String EXAMPLE_USER_FIRST_NAME = "Morgane";
    public static final String EXAMPLE_USER_FAMILY_NAME = "Vidal";
    public static final String EXAMPLE_USER_ADDRESS = "2 place Pierre Viala, Bat 29, Montpellier";
    public static final String EXAMPLE_USER_PHONE = "0400000000";
    public static final String EXAMPLE_USER_AFFILIATION = "affiliation";
    public static final String EXAMPLE_USER_ORCID = "orcid";
    public static final String EXAMPLE_USER_ADMIN = "true";
    public static final String EXAMPLE_USER_AVAILABLE = "true";
    
    public static final String EXAMPLE_DOCUMENT_URI = "http://www.phenome-fppn.fr/phis_field/documents/documente597f57ba71d421a86277d830f4b9885";
    public static final String EXAMPLE_DOCUMENT_TYPE = "http://www.phenome-fppn.fr/vocabulary/2017#ScientificDocument";
    public static final String EXAMPLE_DOCUMENT_CREATOR = "John Doe";
    public static final String EXAMPLE_DOCUMENT_LANGUAGE = "fr";
    public static final String EXAMPLE_DOCUMENT_CREATION_DATE = "2017-07-07";
    public static final String EXAMPLE_DOCUMENT_EXTENSION = "png";
    public static final String EXAMPLE_DOCUMENT_TITLE = "title";
    public static final String EXAMPLE_DOCUMENT_CONCERNED_TYPE_URI = "http://www.phenome-fppn.fr/vocabulary/2017#Experiment";
    public static final String EXAMPLE_DOCUMENT_STATUS = "linked";
    
    public static final String EXAMPLE_TRAIT_URI = "http://www.phenome-fppn.fr/phis_field/id/traits/t001";
    public static final String EXAMPLE_TRAIT_LABEL = "Height";
    
    public static final String EXAMPLE_METHOD_URI = "http://www.phenome-fppn.fr/phis_field/id/methods/m001";
    public static final String EXAMPLE_METHOD_LABEL = "comptage";
    
    public static final String EXAMPLE_UNIT_URI = "http://www.phenome-fppn.fr/phis_field/id/units/u001";
    public static final String EXAMPLE_UNIT_LABEL = "cm";
    
    public static final String EXAMPLE_VARIABLE_URI = "http://www.phenome-fppn.fr/diaphen/id/variable/v0000001";
    public static final String EXAMPLE_VARIABLE_LABEL = "LAI";
    
    public static final String EXAMPLE_CONCEPT_URI = "http://www.phenome-fppn.fr/vocabulary/2017#Document";
    public static final String EXAMPLE_DEEP ="true";
    public static final String EXAMPLE_SIBLING_URI = "http://www.phenome-fppn.fr/vocabulary/2017#ScientificDocument";

    
    public static final String EXAMPLE_RDFTYPE_URI = "http://www.phenome-fppn.fr/vocabulary/2017#ScientificDocument";
    public static final String EXAMPLE_INSTANCE_URI = "http://www.phenome-fppn.fr/phenovia/documents/document90fb96ace2894cdb9f4575173d8ed4c9";
}
