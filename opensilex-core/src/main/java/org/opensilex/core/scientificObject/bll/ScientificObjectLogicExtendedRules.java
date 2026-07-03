/*
 * *****************************************************************************
 *                         ScientificObjectLogicExtendedRules.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 02/07/2026 15:59
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.core.scientificObject.bll;

import org.jvnet.hk2.annotations.Contract;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;

import javax.ws.rs.WebApplicationException;
import java.net.URI;

@Contract
public interface ScientificObjectLogicExtendedRules {
    public boolean applyRulesToThisType(URI typeURI);
    public void createRule(ScientificObjectModel model) throws WebApplicationException;
    public void updateRule(ScientificObjectModel model) throws WebApplicationException;
    public void deleteRule(ScientificObjectModel model) throws WebApplicationException;
}
