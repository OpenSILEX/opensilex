//******************************************************************************
//                                       Provenance.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 mars 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis.provenance;

import java.util.HashMap;

/**
 * Represents the view of a provenance.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Provenance {
    //Provenance uri
    //@example http://www.opensilex.org/demo/2019/pvx355chdo34pger464565
    protected String uri;
    //Label of the provenance
    //@example PROV2018-EAR
    protected String label;
    //Comment of the provenance
    protected String comment;
    //Additional informations for the provenance. Its containt depends of the type of provenance. 
    //@example [ "SensingDevice" => "http://www.opensilex.org/demo/s001",
    //           "Vector" => "http://www.opensilex.org/demo/v001"]
    protected HashMap<String, Object> metadata = new HashMap<>();

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

    public HashMap<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(HashMap<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public void addMetadata(String key, String value) {
        this.metadata.put(key, value);
    }
}
