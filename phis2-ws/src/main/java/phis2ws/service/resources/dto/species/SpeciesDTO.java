//******************************************************************************
//                                       SpeciesDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 déc. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.species;

import phis2ws.service.view.model.phis.Species;

/**
 * Represents a species
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class SpeciesDTO {
    //uri of the rdf resource
    protected String uri;
    //label of the rdf resource
    protected String label;
    
    public SpeciesDTO(Species species) {
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
