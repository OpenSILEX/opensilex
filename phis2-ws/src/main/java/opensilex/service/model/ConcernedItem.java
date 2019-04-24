//******************************************************************************
//                               ConcernedItem.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 11 December 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.List;

/**
 * Concerned item model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ConcernedItem {
    
    private String uri;
    private String rdfType;
    private List<String> labels;
    private String objectLinked;

    public ConcernedItem() {
    }

    public ConcernedItem(String uri, String rdfType, List<String> labels, String objectLinked) {
        this.uri = uri;
        this.rdfType = rdfType;
        this.labels = labels;
        this.objectLinked = objectLinked;
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

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getObjectLinked() {
        return objectLinked;
    }

    public void setObjectLinked(String objectLinked) {
        this.objectLinked = objectLinked;
    }
}
