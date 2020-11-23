//******************************************************************************
//                          ProvenanceCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.text.ParseException;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.core.provenance.dal.ActivityModel;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.server.rest.validation.ValidURI;

/**
 * Provenance Creation DTO
 * @author Alice Boizet
 */
public class ProvenanceCreationDTO {
    
    /**
     * uri
     */
    @ValidURI
    protected URI uri;

    /**
     * label
     */
    @NotNull
    protected String name;
    
    /**
     * comment
     */
    protected String comment;
    
    /**
     * experiments list
     */
    protected List<URI> experiments;

    /**
     * activity
     */
    @JsonProperty("provActivity")
    protected List<ActivityModel> activity;
       
    /**
     * agents
     */
    @JsonProperty("provAgent")
    protected List<AgentModel> agents;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<ActivityModel> getActivity() {
        return activity;
    }

    public void setActivity(List<ActivityModel> activity) {
        this.activity = activity;
    }

    public List<AgentModel> getAgents() {
        return agents;
    }

    public void setAgents(List<AgentModel> agents) {
        this.agents = agents;
    }

    public ProvenanceModel newModel() throws ParseException {
        ProvenanceModel model = new ProvenanceModel();
        model.setUri(uri);
        model.setName(name);
        model.setComment(comment);
        model.setExperiments(experiments);
        model.setActivity(activity);
        model.setAgents(agents);

        return model;
        
    }
}
