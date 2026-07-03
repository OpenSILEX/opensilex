/*
 * *****************************************************************************
 *                         SpiderMutagenLogicExtendedRules.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 02/07/2026 16:42
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.yvan.spidermutagen.bll;

import org.jvnet.hk2.annotations.Service;
import org.opensilex.core.scientificObject.bll.ScientificObjectLogicExtendedRules;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.server.exceptions.ConflictException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import javax.ws.rs.WebApplicationException;
import java.net.URI;

@Service
public class SpiderMutagenLogicExtendedRules implements ScientificObjectLogicExtendedRules {
    @Override
    public boolean applyRulesToThisType(URI typeURI) {
        return SPARQLDeserializers.compareURIs(typeURI, "http://www.yvan.extension.org#SpiderMutagen");
    }

    @Override
    public void createRule(ScientificObjectModel model) throws WebApplicationException {
        throw new ConflictException("this is me");
    }

    @Override
    public void updateRule(ScientificObjectModel model) throws WebApplicationException {

    }

    @Override
    public void deleteRule(ScientificObjectModel model) throws WebApplicationException {

    }
}
