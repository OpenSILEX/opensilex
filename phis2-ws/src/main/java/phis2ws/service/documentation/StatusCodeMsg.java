//**********************************************************************************************
//                                       StatusCodeMsg.java 
//
// Author(s): Arnaud CHARLEROY 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@supagro.inra.fr, anne.tireau@supagro.inra.fr, pascal.neveu@supagro.inra.fr
// Last modification date:  February, 05 2018 - add constants
// Subject: A class which group possible status message return
// @see phenomeapi.service.view.brapi.Status
//***********************************************************************************************
package phis2ws.service.documentation;

import javax.inject.Singleton;

/**
 * Defines the web service return messages
 * @author Arnaud CHARLEROY
 * @author Morgane Vidal
 */
@Singleton
public final class StatusCodeMsg {
    public static final String ERR = "Error";
    public static final String INFO = "Info";
    public static final String ERRPG = "PostgresSQL Error";
    public static final String MISSING_FIELDS_LIST = "Missing Field(s) : ";
    public static final String MISSING_FIELDS = "Fields are missing in JSON Data";
    public static final String DATA_ERROR = "Data error";
    public static final String RESOURCES_CREATED = "Resources created";
    public static final String DATA_INSERTED = "Data inserted";
}
