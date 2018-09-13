//******************************************************************************
//                                       RadiometricTarget.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 4 sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

/**
 * Represents a radiometric target view
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class RadiometricTarget {
    //The radiometric target uri.
    //e.g. http://www.phenome-fppn.fr/diaphen/rt001
    private String uri;
    //The radiometric target label
    //e.g. RDT_089
    private String label;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
