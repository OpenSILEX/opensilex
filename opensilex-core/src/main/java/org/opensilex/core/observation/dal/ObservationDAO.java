/*******************************************************************************
 *                         ObservationDAO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/03/2024
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.observation.dal;

import org.jvnet.hk2.annotations.Service;
import org.opensilex.core.observation.dal.model.ObservationDalModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

import javax.inject.Inject;

/**
 * @author Valentin Rigolle
 */
@Service
public class ObservationDAO extends MongoReadWriteDao<ObservationDalModel, ObservationDalSearchFilter> {
    public static final String COLLECTION_NAME = "observation";
    public static final String CREATE_URI_PATH = "observation";

    @Inject
    public ObservationDAO(MongoDBServiceV2 mongodb) {
        super(mongodb, ObservationDalModel.class, COLLECTION_NAME, CREATE_URI_PATH);
    }
}
