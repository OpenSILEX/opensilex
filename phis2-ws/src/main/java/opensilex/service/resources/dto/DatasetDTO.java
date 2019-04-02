//******************************************************************************
//                                       DatasetDTO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.Valid;
import opensilex.service.resources.validation.interfaces.Required;
import opensilex.service.resources.dto.manager.AbstractVerifiedClass;
import opensilex.service.resources.validation.interfaces.URL;
import opensilex.service.model.Dataset;

/**
 * Dataset DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DatasetDTO extends AbstractVerifiedClass {

    private String variableUri;
    //Provenance is the dataset provenance. It is composed of the creation date 
    //of the dataset, the script which has generate the dataset and a description
    //of the generation (if possible)
    private ProvenanceDTO provenance;
    private ArrayList<AgronomicalDataDTO> data;
    
    

    @Override
    public Dataset createObjectFromDTO() {
       Dataset phenotypes = new Dataset();
       phenotypes.setVariableURI(variableUri);       
       phenotypes.setProvenance(provenance.createObjectFromDTO());
       for (AgronomicalDataDTO d : data) {
           phenotypes.addData(d.createObjectFromDTO());
       }
       
       return phenotypes;
    }   
    
    @URL
    @Required
    @ApiModelProperty(example = "http://www.phenome-fppn.fr/diaphen/id/variable/v00001")
    public String getVariableUri() {
        return variableUri;
    }

    public void setVariableUri(String variableUri) {
        this.variableUri = variableUri;
    }

    @Valid
    public ProvenanceDTO getProvenance() {
        return provenance;
    }

    public void setProvenance(ProvenanceDTO provenance) {
        this.provenance = provenance;
    }
    
    @Valid
    public ArrayList<AgronomicalDataDTO> getData() {
        return data;
    }

    public void setData(ArrayList<AgronomicalDataDTO> data) {
        this.data = data;
    }
}
