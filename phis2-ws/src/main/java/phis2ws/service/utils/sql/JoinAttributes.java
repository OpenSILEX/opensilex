//**********************************************************************************************
//                                       JoinAttributes.java 
//
// Author(s): Arnaud CHARLEROY 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: october 2016
// Contact:arnaud.charleroy@supagro.inra.fr, anne.tireau@supagro.inra.fr, pascal.neveu@supagro.inra.fr
// Last modification date:  October, 2016
// Subject: A class which list usables JOIN in SQL
//***********************************************************************************************
package phis2ws.service.utils.sql;

import javax.inject.Singleton;

/**
 * Liste des jointures
 * @author A. CHARLEROY
 */
@Singleton
public final class JoinAttributes {
    public static String LEFTJOIN = "LEFT JOIN"; 
    public static String RIGHTJOIN = "RIGHT JOIN"; 
    public static String INNERJOIN  = "INNER JOIN "; 
    public static String FULLJOIN = "FULL JOIN"; 
    public static String NATURALJOIN = "NATURAL JOIN"; 

    public JoinAttributes() {
    }
    
}
