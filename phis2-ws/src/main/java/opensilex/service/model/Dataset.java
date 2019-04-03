//******************************************************************************
//                                       Dataset.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;

/**
 * Dataset model
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Dataset {
    
    private String variableURI;
    private String experiment;
    private Provenance provenance;
    private ArrayList<AgronomicalData> data = new ArrayList<>();
    
    public Dataset() {
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

    public ArrayList<AgronomicalData> getData() {
        return data;
    }

    public void setData(ArrayList<AgronomicalData> data) {
        this.data = data;
    }
    
    public void addData(AgronomicalData d) {
        data.add(d);
    }

    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }
}
