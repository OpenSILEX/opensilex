//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, morgane.vidal@inra.fr,anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.ontology;

import java.net.URI;
import java.util.List;

/**
 * Ontology reference model.
 *
 * @author Morgane Vidal
 */
public abstract class SKOSReferencesDTO {

    private List<URI> exactMatch;

    private List<URI> closeMatch;

    private List<URI> broader;

    private List<URI> narrower;

    public List<URI> getExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(List<URI> exactMatch) {
        this.exactMatch = exactMatch;
    }

    public List<URI> getCloseMatch() {
        return closeMatch;
    }

    public void setCloseMatch(List<URI> closeMatch) {
        this.closeMatch = closeMatch;
    }

    public List<URI> getBroader() {
        return broader;
    }

    public void setBroader(List<URI> broader) {
        this.broader = broader;
    }

    public List<URI> getNarrower() {
        return narrower;
    }

    public void setNarrower(List<URI> narrower) {
        this.narrower = narrower;
    }
    
    
    public void setSkosReferencesFromModel(SKOSReferencesModel model) {
        this.setNarrower(model.getNarrower());
        this.setBroader(model.getBroader());
        this.setCloseMatch(model.getCloseMatch());
        this.setExactMatch(model.getExactMatch()); 
    }
}
