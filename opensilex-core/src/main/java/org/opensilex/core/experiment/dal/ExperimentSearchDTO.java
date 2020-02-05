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

    public ExperimentSearchDTO setEnded(Boolean ended) {
        this.ended = ended;
        return this;
    }
}
