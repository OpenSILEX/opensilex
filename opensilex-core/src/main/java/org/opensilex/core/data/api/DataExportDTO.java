//******************************************************************************
//                          DataExportDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import java.net.URI;
import java.util.Set;

import org.opensilex.core.data.dal.DataModel;

/**
 * This class is used for the data csv export
 * @author Alice Boizet
 */
public class DataExportDTO extends DataGetDTO {
    private URI experiment;  

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }
    
    public static DataExportDTO fromModel(DataModel model, URI experiment, Set<URI> dateVariables) {
        DataExportDTO dto = new DataExportDTO();   
        dto.fromModel(model, dateVariables);
        dto.setExperiment(experiment);
        return dto;
    }
    
}
