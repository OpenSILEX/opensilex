/*******************************************************************************
 *                         UpdateExperimentSpecies.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 22/03/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.migration.experiment;

import org.opensilex.OpenSilex;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;


/**
 * @author Valentin RIGOLLE
 */
public class UpdateExperimentSpecies implements OpenSilexModuleUpdate {
    public static final Logger LOGGER = LoggerFactory.getLogger(UpdateExperimentSpecies.class);

    private OpenSilex opensilex;

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override
    public String getDescription() {
        return "Update the species of an experiment based on the germplasm of their Scientific Objects";
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {
        Objects.requireNonNull(opensilex);

        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();

        MongoDBService mongodb = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);

        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, mongodb);

        try {
            List<URI> experimentUriList = sparql.searchURIs(ExperimentModel.class, "");

            sparql.startTransaction();

            LOGGER.debug("Updating {} experiments", experimentUriList.size());

            for (URI experimentUri : experimentUriList) {
                experimentDAO.updateExperimentSpeciesFromScientificObjects(experimentUri);
                LOGGER.debug("Experiment <{}> successfully updated", experimentUri);
            }

            sparql.commitTransaction();
            LOGGER.info("All experiments have been successfully updated");

        } catch (Exception e) {
            try {
                sparql.rollbackTransaction();
                throw new OpensilexModuleUpdateException(this, e);
            } catch ( Exception e2) {
                throw new OpensilexModuleUpdateException(this, e2);
            }
        }
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }
}
