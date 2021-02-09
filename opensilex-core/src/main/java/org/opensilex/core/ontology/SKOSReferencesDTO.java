//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, morgane.vidal@inra.fr,anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.ontology;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.server.rest.validation.ValidURI;

/**
 * Ontology reference model.
 *
 * @author Morgane Vidal
 */
public abstract class SKOSReferencesDTO {

    public final static String EXACT_MATCH_JSON_PROPERTY = "exact_match";
    public final static String CLOSE_MATCH_JSON_PROPERTY = "close_match";
    public final static String BROAD_MATCH_JSON_PROPERTY = "broad_match";
    public final static String NARROW_MATCH_JSON_PROPERTY = "narrow_match";
    
    @ValidURI
    @JsonProperty(EXACT_MATCH_JSON_PROPERTY)
    private List<URI> exactMatch = new ArrayList<>();
    
    @ValidURI
    @JsonProperty(CLOSE_MATCH_JSON_PROPERTY)
    private List<URI> closeMatch = new ArrayList<>();
    
    @ValidURI
    @JsonProperty(BROAD_MATCH_JSON_PROPERTY)
    private List<URI> broadMatch = new ArrayList<>();
    
    @ValidURI
    @JsonProperty(NARROW_MATCH_JSON_PROPERTY)
    private List<URI> narrowMatch = new ArrayList<>();

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

    public List<URI> getBroadMatch() {
        return broadMatch;
    }

    public void setBroadMatch(List<URI> broadMatch) {
        this.broadMatch = broadMatch;
    }

    public List<URI> getNarrowMatch() {
        return narrowMatch;
    }

    public void setNarrowMatch(List<URI> narrowMatch) {
        this.narrowMatch = narrowMatch;
    }

    public void setSkosReferencesFromModel(SKOSReferences model) {
        this.setNarrowMatch(model.getNarrowMatch());
        this.setBroadMatch(model.getBroadMatch());
        this.setCloseMatch(model.getCloseMatch());
        this.setExactMatch(model.getExactMatch());
    }

    public void setSkosReferencesToModel(SKOSReferences model) {
        model.setNarrowMatch(this.getNarrowMatch());
        model.setBroadMatch(this.getBroadMatch());
        model.setCloseMatch(this.getCloseMatch());
        model.setExactMatch(this.getExactMatch());
    }
}
