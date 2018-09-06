//**********************************************************************************************
//                                       StatusCodeMsg.java 
//
// Author(s): Arnaud Charleroy 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  February, 05 2018 - add constants
// Subject: A class which group possible status message return
// @see phenomeapi.service.view.brapi.Status
//***********************************************************************************************
package phis2ws.service.documentation;

import javax.inject.Singleton;

/**
 * Defines the web service return messages
 * @author Arnaud Charleroy
 * @author Morgane Vidal
 */
@Singleton
public final class StatusCodeMsg {
    
    public static final String ACCESS_DENIED = "Access denied";
    public static final String ACCESS_ERROR = "Access error";
    public static final String ADMINISTRATOR_ONLY = "Request only for administrators";
    public static final String ALREADY_EXISTING_DATA = "Already existing data";
    public static final String BAD_CARDINALITY = "Bad cardinality";
    public static final String BAD_DATA_FORMAT = "Bad data format";
    public static final String COMMIT_TRIPLESTORE_ERROR = "Error during commit or rolleback Triplestore statements.";
    public static final String DATA_ERROR = "Data error";
    public static final String DATA_INSERTED = "Data inserted";
    public static final String ERR = "Error";
    public static final String EXPECTED_DATE_FORMAT_YMD = "Expected date format : YYYY-MM-DD";
    public static final String FILE_ERROR = "File error";
    public static final String INFO = "Info";
    public static final String MALFORMED_CREATE_QUERY = "Malformed create query";
    public static final String MALFORMED_UPDATE_QUERY = "Malformed update query";
    public static final String MALFORMED_URI = "Malformed uri";
    public static final String MD5_ERROR = "md5 error";
    public static final String MISSING_FIELDS_LIST = "Missing Field(s) : ";
    public static final String MISSING_FIELDS = "Fields are missing in JSON Data";
    public static final String NO_RESULTS = "No results";
    public static final String POSTGRESQL_ERROR = "PostgresSQL Error";
    public static final String QUERY_ERROR = "Query error";
    public static final String REQUEST_ERROR = "Request error";
    public static final String RESOURCES_CREATED = "Resources created";
    public static final String RESOURCES_UPDATED = "Resources updated";
    public static final String SFTP_EXCEPTION = "sftp exception";
    public static final String TIMEOUT = "Timeout";
    public static final String TRIPLESTOR_ACCESS_ERROR = "Triplestore access error";
    public static final String UNKNOWN_URI = "Unknown uri";
    public static final String WRONG_VALUE = "Wrong value";
    public static final String INVALID_INPUT_PARAMETERS = "Wrong format parameter(s)";
}
