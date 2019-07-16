//******************************************************************************
//                                ObjectDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 21 mai 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.rdfResourceDefinition;

import opensilex.service.model.RdfResourceDefinition;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;

/**
 * Object DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class RdfResourceDTO extends AbstractVerifiedClass {
    //uri of the rdf resource
    //@example http://www.opensilex.org/demo/id/provenance/1553100256084
    protected String uri;
    //Label of the rdf resource
    //@example PROV2019-3
    protected String label;

    public RdfResourceDTO() {
        
    }

    public RdfResourceDTO(String uri, String label) {
        this.uri = uri;
        this.label = label;
    }
    
    public RdfResourceDTO(RdfResourceDefinition rdfResourceDefinition) {
        uri = rdfResourceDefinition.getUri();
        label = rdfResourceDefinition.getLabel();
    }

    @Override
    public Object createObjectFromDTO() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
