//******************************************************************************
//                                FactorDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 12 août 2019
// Contact: Expression userEmail is undefined on line 6, column 15 in file:///home/charlero/GIT/GITLAB/phis-ws/phis2-ws/licenseheader.txt., anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.treatment;

import opensilex.service.model.Factor;

/**
 * Factor DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class FactorDTO {
    
    //uri of the rdf resource
    protected String uri;
    
    //label of the rdf resource
    protected String label;
    
    public FactorDTO(Factor species) {
        uri = species.getUri();
        label = species.getLabel();
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}