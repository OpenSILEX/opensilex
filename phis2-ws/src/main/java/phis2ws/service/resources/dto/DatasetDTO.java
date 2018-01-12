//**********************************************************************************************
//                                       DatasetDTO.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  September, 13 2017
// Subject: Represents the JSON submitted for the datasets
//***********************************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.Dataset;
import phis2ws.service.view.model.phis.Provenance;

/**
 * corresponds to the submitted JSON for the datasets
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DatasetDTO extends AbstractVerifiedClass {

    private String variableUri;
    private Provenance provenance;
    private ArrayList<DataDTO> data;
    
    @Override
    public Map rules() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put(variableUri, Boolean.TRUE);
        
        //SILEX:todo
        //Find a way to add rules on others Objects
        //\SILEX:todo
        return rules;
    }

    @Override
    public Dataset createObjectFromDTO() {
       Dataset phenotypes = new Dataset();
       phenotypes.setVariableURI(variableUri);       
       phenotypes.setProvenance(new Provenance(provenance));
       for (DataDTO d : data) {
           phenotypes.addData(d.createObjectFromDTO());
       }
       
       return phenotypes;
    }   
    
    @ApiModelProperty(example = "http://www.phenome-fppn.fr/diaphen/id/variable/v00001")
    public String getVariableUri() {
        return variableUri;
    }

    public void setVariableUri(String variableUri) {
        this.variableUri = variableUri;
    }

    public Provenance getProvenance() {
        return provenance;
    }

    public void setProvenance(Provenance provenance) {
        this.provenance = provenance;
    }

    public ArrayList<DataDTO> getData() {
        return data;
    }

    public void setData(ArrayList<DataDTO> data) {
        this.data = data;
    }
}
