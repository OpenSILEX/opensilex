//**********************************************************************************************
//                                       Phenotype.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  September, 14 2017
// Subject: represents the phenotype view
//***********************************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;

public class Phenotype {
    private String variableURI;
    private String experiment;
    private Provenance provenance;
    private ArrayList<Data> data = new ArrayList<>();
    
    public Phenotype() {
        
    }

    public String getVariableURI() {
        return variableURI;
    }

    public void setVariableURI(String variableURI) {
        this.variableURI = variableURI;
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
    
    public void addData(Data d) {
        data.add(d);
    }

    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }
}
