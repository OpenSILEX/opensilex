//**********************************************************************************************
//                                       RawDataDTO.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  September, 13 2017
// Subject: XX
//***********************************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.Data;
import phis2ws.service.view.model.phis.Phenotype;
import phis2ws.service.view.model.phis.Provenance;

public class PhenotypeDTO extends AbstractVerifiedClass {

    private String variableUri;
    private Provenance provenance;
    private ArrayList<Data> data;
    
    @Override
    public Map rules() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put(variableUri, Boolean.TRUE);
        
        //SILEX:todo
        //Trouver une solution pour mettres les rules sur les objets complexes.
        //\SILEX:todo
        return rules;
    }

    @Override
    public Phenotype createObjectFromDTO() {
       Phenotype phenotypes = new Phenotype();
       phenotypes.setVariableURI(variableUri);       
       phenotypes.setProvenance(new Provenance(provenance));
       for (Data d : data) {
           phenotypes.addData(new Data(d));
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

    public ArrayList<Data> getData() {
        return data;
    }

    public void setData(ArrayList<Data> data) {
        this.data = data;
    }
}
