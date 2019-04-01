//**********************************************************************************************
//                                       WasGeneratedBy.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: October 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 4 2017
// Subject: Represents the data wasGeneratedBy view
//***********************************************************************************************
package opensilex.service.view.model.phis;

public class WasGeneratedBy {
    private String wasGeneratedBy; 
    private String wasGeneratedByDescription;
    
    public WasGeneratedBy() {
        
    }

    public String getWasGeneratedBy() {
        return wasGeneratedBy;
    }

    public void setWasGeneratedBy(String wasGeneratedBy) {
        this.wasGeneratedBy = wasGeneratedBy;
    }

    public String getWasGeneratedByDescription() {
        return wasGeneratedByDescription;
    }

    public void setWasGeneratedByDescription(String wasGeneratedByDescription) {
        this.wasGeneratedByDescription = wasGeneratedByDescription;
    }
}
