/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.provenance.api;

import java.net.URI;
import java.util.Map;

/**
 *
 * @author boizetal
 */

public class ProvenanceGetDTO {
    
    protected URI uri;
    
    /**
     * label
     */
    protected String label;
    
    /**
     * comment
     */
    protected String comment;
    
    /**
     * experiment
     */
    protected URI experiment;
    
    /**
     * metadata
     */
    protected Map metadata;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
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

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }   

    public Map getMetadata() {
        return metadata;
    }

    public void setMetadata(Map metadata) {
        this.metadata = metadata;
    }
    
}
