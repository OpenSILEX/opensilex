//******************************************************************************
//                          ExperimentSearchDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.experiment.dal;

import org.opensilex.core.experiment.api.ExperimentGetDTO;


/**
 * @author Renaud COLIN
 */
public class ExperimentSearchDTO extends ExperimentGetDTO {

    private Boolean ended;

    public Boolean isEnded() {
        return ended;
    }

    private boolean admin = false;

    public Boolean isAdmin() {
        return admin;
    }

    public ExperimentSearchDTO setAdmin(Boolean admin) {
        this.admin = admin;
        return this;
    }

    public ExperimentSearchDTO setEnded(Boolean ended) {
        this.ended = ended;
        return this;
    }   
}
