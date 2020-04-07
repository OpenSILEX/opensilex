//******************************************************************************
//                                AccessionPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 24 juin 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.germplasm;

import java.util.ArrayList;
import opensilex.service.model.Germplasm;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.dto.rdfResourceDefinition.PropertyPostDTO;

/**
 * GermplasmPostDTO
 * used to create a "germplasm" which can be a variety, an accession or a plantMaterialLot
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class GermplasmPostDTO extends AbstractVerifiedClass {
    /*voir https://www.genesys-pgr.org/fr/doc/0/basics*/
    
    private String rdfType;
    private String URI;
    private String label;
    private ArrayList<PropertyPostDTO> properties;
    
    @Override
    public Germplasm createObjectFromDTO() throws Exception {
        boolean annotationInsert = true;
        Germplasm germplasm = new Germplasm();
        germplasm.setRdfType(rdfType);
        germplasm.setUri(URI);
        germplasm.setLabel(label);
        
        if (properties != null) {
            properties.forEach((property) -> {
                germplasm.addProperty(property.createObjectFromDTO());
            });
        }
        
        return germplasm;
    }

    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public String getURI() {
        return URI;
    }

    public void setURI(String URI) {
        this.URI = URI;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ArrayList<PropertyPostDTO> getProperties() {
        return properties;
    }

    public void setProperties(ArrayList<PropertyPostDTO> properties) {
        this.properties = properties;
    }
    
}
