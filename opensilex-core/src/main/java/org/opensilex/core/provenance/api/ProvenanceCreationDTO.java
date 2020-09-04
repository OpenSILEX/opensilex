//******************************************************************************
//                          provenanceCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jdo.annotations.Embedded;
import org.bson.Document;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.provenance.dal.ProvMetadata;

/**
 *
 * @author boizetal
 */
public class ProvenanceCreationDTO {
    
    protected String jsonSchema;
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
    protected List<URI> experiments;
    
    /**
     * metadata
     */
    protected AgentModel agent;

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

    public List<URI> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<URI> experiments) {
        this.experiments = experiments;
    }   

    public AgentModel getAgent() {
        return agent;
    }

    public void setAgent(AgentModel agent) {
        this.agent = agent;
    }

    public String getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(String jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

}
