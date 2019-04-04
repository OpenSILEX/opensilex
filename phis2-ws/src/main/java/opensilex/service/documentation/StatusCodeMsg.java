//******************************************************************************
//                             StatusCodeMsg.java 
// SILEX-PHIS 
// Copyright © INRA 2016
// Creation date: August 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.documentation;

import javax.inject.Singleton;

/**
 * Web service return messages.
 * @see phenomeapi.service.view.brapi.Status
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
@Singleton
public final class StatusCodeMsg {
    
    public static final String ACCESS_DENIED = "Access denied";
    public static final String ACCESS_ERROR = "Access error";
    public static final String ADMINISTRATOR_ONLY = "Request only for administrators";
    public static final String UNEXPECTED_ERROR = "Unexpected error";
    public static final String ALREADY_EXISTING_DATA = "Already existing data";
    public static final String BAD_CARDINALITY = "Bad cardinality";
    public static final String BAD_DATA_FORMAT = "Bad data format";
    public static final String COMMIT_TRIPLESTORE_ERROR = "Error during commit or rolleback Triplestore statements.";
    public static final String DATA_ERROR = "Data error";
    public static final String DATA_INSERTED = "Data inserted";
    public static final String DATA_REJECTED = "Data rejected";
    public static final String ERR = "Error";
    public static final String ERROR_WHILE_COMMITTING_OR_ROLLING_BACK_TRIPLESTORE_STATEMENT = "Error while committing or rolling back Triplestore statements: ";
    public static final String EVENT_TO_ADD_IS_EMPTY = "The event to add is empty";
    public static final String EXPECTED_DATE_FORMAT_YMD = "Expected date format : YYYY-MM-DD";
    public static final String FILE_ERROR = "File error";
    public static final String INFO = "Info";
    public static final String MALFORMED_CREATE_QUERY = "Malformed create query";
    public static final String MALFORMED_UPDATE_QUERY = "Malformed update query";
    public static final String MALFORMED_URI = "Malformed URI";
    public static final String MD5_ERROR = "md5 error";
    public static final String MISSING_FIELDS_LIST = "Missing Field(s) : ";
    public static final String MISSING_FIELDS = "Fields are missing in JSON Data";
    public static final String NO_RESULTS = "No results";
    public static final String POSTGRESQL_ERROR = "PostgresSQL Error";
    public static final String QUERY_ERROR = "Query error";
    public static final String URI_TYPE_NOT_IN_DOMAIN_OF_RELATION = "the type of the subject %s is not in the domain of the relation %s";
    public static final String REQUEST_ERROR = "Request error";
    public static final String RESOURCES_CREATED = "Resource(s) created";
    public static final String RESOURCES_UPDATED = "Resource(s) updated";
    public static final String SFTP_EXCEPTION = "sftp exception";
    public static final String TIMEOUT = "Timeout";
    public static final String TRIPLESTOR_ACCESS_ERROR = "Triplestore access error";
    public static final String UNKNOWN_URI = "Unknown URI";
    public static final String UNKNOWN_TYPE = "Unknown type %s";
    public static final String UNKNOWN_URI_OF_TYPE = "Unknown URI %s of type %s";
    public static final String UNKNOWN_EVENT_URI = "Unknown event URI";
    public static final String UNKNOWN_CONCERNED_ITEM_URI = "Unknown concerned item's URI %s";
    public static final String VALUE_TYPE_URI_NOT_IN_RANGE_OF_RELATION = "the type %s of the value %s is not in the range of the relation %s";
    public static final String WRONG_VALUE = "Wrong value";
    public static final String INVALID_INPUT_PARAMETERS = "Wrong format parameter(s)";
}
