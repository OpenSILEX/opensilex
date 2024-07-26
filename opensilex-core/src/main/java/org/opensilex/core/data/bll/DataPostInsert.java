/*
 *  *************************************************************************************
 *  DataPostInsert.java
 *  OpenSILEX - Licence AGPL V3.0 -  https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright @ INRAE 2024
 * Contact :  user@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ************************************************************************************
 */

package org.opensilex.core.data.bll;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * An object which store information which are useful for running post data-insert actions
 * @author rcolin
 */
public class DataPostInsert {

    Map<URI, Set<URI>> variableToDevices;

}
