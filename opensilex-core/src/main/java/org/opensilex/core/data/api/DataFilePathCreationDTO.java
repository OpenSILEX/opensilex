//******************************************************************************
//                          DataFileCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import java.text.ParseException;
import javax.validation.constraints.NotNull;
import org.opensilex.core.data.dal.DataFileModel;

/**
 *
 * @author Alice Boizet
 */
public class DataFilePathCreationDTO extends DataFileCreationDTO {
    @NotNull
    private String relativePath;

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
    
    
    @Override
    public DataFileModel newModel() throws ParseException {
        DataFileModel model = new DataFileModel();
        model.setDate(getDate());
        model.setMetadata(getMetadata());
        model.setProvUsed(getProvenance().getProvUsed());
        model.setProvenanceSettings(getProvenance().getSettings());
        model.setProvenanceURI(getProvenance().getUri());
        model.setRdfType(getRdfType());
        model.setObject(getScientificObjects());
        model.setUri(getUri());
        model.setPath(relativePath);
        return model;
    }
    
        
}
