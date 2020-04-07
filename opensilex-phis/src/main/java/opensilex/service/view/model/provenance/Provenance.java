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
     * {
     *   namespaces" : {
     *      "prov" : "http://www.w3.org/ns/prov#",
     *      "oeso" : "http://www.opensilex.org/vocabulary/oeso#",
     *      "rdf" : "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
     *   ,
     *   creationDate" : "2019-10-30T11:16:23+0100",
     *   prov:Agent" : {
     *      "oeso:SensingDevice" : [ 
     *          "http://www.opensilex.org/sunagri/2018/s18002", 
     *          "http://www.opensilex.org/sunagri/2018/s18003"
     *      ],
     *      "oeso:Operator" : [ 
     *          "perrine.fortin@sunr.fr", 
     *          "perrine.juillion@itk.fr"
     *      ]
     *  }
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
