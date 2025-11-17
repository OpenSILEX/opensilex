package org.opensilex.siduri.experimentFdF.dal;

import org.opensilex.core.experiment.dal.ExperimentSearchFilter;


/**
 * @author efernandez
 */
public class ExperimentFdFSearchFilter extends ExperimentSearchFilter {

    private Boolean isFdF;


    public ExperimentFdFSearchFilter setFdF(Boolean fdF) {
        this.isFdF = fdF;
        return this;
    }

    public Boolean isFdF(){ return isFdF; }

}
