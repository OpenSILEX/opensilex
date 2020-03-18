/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.germplasm.api;

import java.net.URI;
import java.util.ArrayList;
import opensilex.service.germplasm.dal.GermplasmModel;
import opensilex.service.resource.dto.rdfResourceDefinition.PropertyPostDTO;

/**
 *
 * @author boizetal
 */
class GermplasmCreationDTO {
    protected URI uri;
    protected String rdfType;    
    protected String label;
    protected String fromGenus;
    protected String fromSpecies;
    protected String fromVariety;
    protected String fromAccession;
    
    public GermplasmModel newModel() {
        GermplasmModel model = new GermplasmModel();
        model.setUri(uri);
        model.setLabel(label);
        model.setRdfType(rdfType);
        model.setGenus(fromGenus);
        model.setSpecies(fromSpecies);
        model.setVariety(fromVariety);
        model.setAccession(fromAccession);
                
        return model;
    }   

}
