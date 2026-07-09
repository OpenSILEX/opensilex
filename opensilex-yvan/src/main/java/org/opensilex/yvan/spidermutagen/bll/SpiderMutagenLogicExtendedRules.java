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
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.ConflictException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.yvan.ontology.YvanOntology;

import javax.ws.rs.WebApplicationException;
import java.net.URI;
import java.util.List;

@Service
public class SpiderMutagenLogicExtendedRules implements ScientificObjectLogicExtendedRules {
    @Override
    public boolean applyRulesToThisType(URI typeURI) {
        return SPARQLDeserializers.compareURIs(typeURI, YvanOntology.SpiderMutagen.getURI());
    }

    @Override
    public void createRule(ScientificObjectModel model) throws WebApplicationException {
        validateSpiderMutagen(model);
    }

    @Override
    public void updateRule(ScientificObjectModel model) throws WebApplicationException {
        validateSpiderMutagen(model);
    }

    @Override
    public void deleteRule(ScientificObjectModel model) throws WebApplicationException {

    }

    private void validateSpiderMutagen(ScientificObjectModel model) throws WebApplicationException {
        if (model.getRelations() == null || model.getRelations().isEmpty()) return;

        if (model.getRelation(YvanOntology.legsNumber) != null) {
            int legNumbers = Integer.parseInt(model.getRelation(YvanOntology.legsNumber).getValue());
            if (legNumbers < 2) {
                throw new BadRequestException("a spider with less than 2 legs ? Too crazy to be allowed sorry.");
            }
        }

        //it is useless to check if devices exist as it will be done by the core ScientificObject services.
        if (model.getRelations(YvanOntology.linkedDevice) != null) {
            List<String> linkedDevices = model.getRelations(YvanOntology.linkedDevice).map(SPARQLModelRelation::getValue).toList();
            if (linkedDevices.size() > 3) {
                throw new BadRequestException("This spider seems to have a lot of attention... max 3 devices allowed per spider.");
            }
        }
    }
}
