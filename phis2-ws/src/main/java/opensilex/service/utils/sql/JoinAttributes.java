//******************************************************************************
//                              JoinAttributes.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: Oct. 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.utils.sql;

import javax.inject.Singleton;

/**
 * SQL joins.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
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
