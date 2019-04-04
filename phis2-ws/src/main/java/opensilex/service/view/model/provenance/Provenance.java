//******************************************************************************
//                              Provenance.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 Mar. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.view.model.provenance;

import java.util.HashMap;

/**
 * Provenance model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Provenance {
    
    /**
     * Provenance URI.
     * @example http://www.opensilex.org/demo/2019/pvx355chdo34pger464565
     */
    protected String uri;
    
    /**
     * Label.
     * @example PROV2018-EAR
     */
    protected String label;
    
    /**
     * Comment.
     */
    protected String comment;
    
    /**
     * Additional information for the provenance. 
     * Its content depends on the type of the provenance. 
     * @example 
     * [ 
     *   "SensingDevice" => "http://www.opensilex.org/demo/s001",
     *   "Vector" => "http://www.opensilex.org/demo/v001"
     * ]
     */
    protected Object metadata = new HashMap<>();

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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
}
