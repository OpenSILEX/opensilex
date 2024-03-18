/*******************************************************************************
 *                         ObservationLogic.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/03/2024
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.observation.bll;

import org.jvnet.hk2.annotations.Service;
import org.opensilex.core.observation.bll.model.ObservationBLM;
import org.opensilex.core.observation.dal.ObservationDAO;
import org.opensilex.core.observation.dal.ObservationSearchFilter;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import java.net.URISyntaxException;

/**
 * @author Valentin Rigolle
 */
@Service
public class ObservationLogic {
    private ObservationDAO dao;

    @Inject
    public ObservationLogic(ObservationDAO dao) {
        this.dao = dao;
    }

    public boolean create(ObservationBLM<?> observationBLM) {
        try {
            //todo return the created object / at least the URI
            var result = dao.create(observationBLM.toModel());
            return result.wasAcknowledged();
        } catch (URISyntaxException | NoSQLAlreadyExistingUriException e) {
            //todo handle this / rethrow or use logic-specific exception ?
            throw new RuntimeException(e);
        }
    }

    public ListWithPagination<ObservationBLM<?>> search() {
        return dao.search(new ObservationSearchFilter(), ObservationBLM::fromModel);
    }
}
