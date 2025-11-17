//******************************************************************************
//                          ExperimentModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.siduri.experimentFdF.dal;

import org.opensilex.core.ontology.Ofs;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.sparql.annotations.SPARQLProperty;


/**
 * @author efernandez
 */

public class ExperimentFdFModel extends ExperimentModel {

    @SPARQLProperty(
            ontology = Ofs.class,
            property = "isFdF"
    )
    protected Boolean isFdF;
    public static final String IS_FDF_FIELD = "isFdF";

    public Boolean getIsFdF() {
        return isFdF;
    }

    public void setIsFdF(Boolean fdF) {
        this.isFdF = fdF;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
}