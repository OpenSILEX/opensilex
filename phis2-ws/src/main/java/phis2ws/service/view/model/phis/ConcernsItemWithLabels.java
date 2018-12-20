//******************************************************************************
//                                       ConcernsItemWithLabels.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 20 déc. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;

/**
 *
 * @author andreas
 */
public class ConcernsItemWithLabels extends ConcernItem {
    
    private ArrayList<String> labels;

    public ConcernsItemWithLabels(ArrayList<String> labels, String uri, 
            String rdfType) {
        super(uri, rdfType);
        this.labels = labels;
    }

    public ArrayList<String> getLabels() {
        return labels;
    }

    public void setLabels(ArrayList<String> labels) {
        this.labels = labels;
    }    
}
