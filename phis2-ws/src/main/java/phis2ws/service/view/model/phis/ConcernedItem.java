//**********************************************************************************************
//                                       ConcernItem.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 11 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  December, 11 2017
// Subject: Represents the concern item view
//***********************************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;

/**
 * represents the concern item view
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
