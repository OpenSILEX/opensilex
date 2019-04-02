//******************************************************************************
//                               ConcernedItem.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 11 December 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;

/**
 * Concerned item model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ConcernedItem {
    
    private String uri;
    private String rdfType;
    private ArrayList<String> labels;

    public ConcernedItem() {
    }

    public ConcernedItem(String uri, String rdfType, ArrayList<String> labels) {
        this.uri = uri;
        this.rdfType = rdfType;
        this.labels = labels;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public ArrayList<String> getLabels() {
        return labels;
    }

    public void setLabels(ArrayList<String> labels) {
        this.labels = labels;
    }
}
