//**********************************************************************************************
//                                       StatusCodeMsg.java 
//
// Author(s): Arnaud CHARLEROY 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@supagro.inra.fr, anne.tireau@supagro.inra.fr, pascal.neveu@supagro.inra.fr
// Last modification date:  October, 2016
// Subject: A class which group possible status message return
// @see phenomeapi.service.view.brapi.Status
//***********************************************************************************************
package phis2ws.service.documentation;

import javax.inject.Singleton;

/**
 * Définit des messages de retour régulièrement utilisés
 * @author Arnaud CHARLEROY
 */
@Singleton
public final class StatusCodeMsg {
    public static final String ERR = "Error";
    public static final String INFO = "Info";
    public static final String ERRPG = "PostgresSQL Error";
    public static final String MISSINGFIELDS = "Missing Field(s) : ";
   
}
