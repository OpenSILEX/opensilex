//******************************************************************************
//                          ProvenanceGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.api;

import java.net.URI;
import org.opensilex.core.provenance.dal.ProvenanceModel;

/**
 * Provenance Get DTO
 * @author Alice Boizet
 */
public class ProvenanceGetDTO extends ProvenanceCreationDTO {
    
    protected URI uri;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    public static ProvenanceGetDTO fromModel(ProvenanceModel model){
        ProvenanceGetDTO dto = new ProvenanceGetDTO();        
        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setComment(model.getComment());
        dto.setExperiments(model.getExperiments());
        dto.setActivity(model.getActivity());
        dto.setAgents(model.getAgents());
        
        return dto;
    }
}
