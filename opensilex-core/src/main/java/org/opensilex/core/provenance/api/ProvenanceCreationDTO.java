//******************************************************************************
//                          ProvenanceCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.core.provenance.dal.ActivityModel;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.server.rest.validation.Required;

/**
 * Provenance Creation DTO
 * @author Alice Boizet
 */
public class ProvenanceCreationDTO {
    
    /**
     * uri
     */
    @ValidURI
    @ApiModelProperty(value = "provenance name", example = "http://provenance/prov01")
    protected URI uri;

    /**
     * name
     */
    @Required    
    @ApiModelProperty(value = "provenance uri manually entered", example = "air_temperature_acquisition", required = true)
    protected String name;
    
    /**
     * description
     */
    @ApiModelProperty(value = "provenance description", example = "acquisition of air temperature with sensor 01")
    protected String description;
    
    /**
     * activity
     */
    @JsonProperty("prov_activity")
    protected List<ActivityCreationDTO> activity;
       
    /**
     * agents
     */
    @JsonProperty("prov_agent")
    protected List<AgentModel> agents;

    @JsonProperty("publisher")
    protected UserGetDTO publisher;

    @JsonProperty("issued")
    protected Instant publicationDate;

    @JsonProperty("modified")
    protected Instant lastUpdatedDate;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ActivityCreationDTO> getActivity() {
        return activity;
    }

    public void setActivity(List<ActivityCreationDTO> activity) {
        this.activity = activity;
    }

    public List<AgentModel> getAgents() {
        return agents;
    }

    public void setAgents(List<AgentModel> agents) {
        this.agents = agents;
    }

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
        this.publisher = publisher;
    }

    public Instant getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Instant publicationDate) {
        this.publicationDate = publicationDate;
    }

    public Instant getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Instant lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public ProvenanceModel newModel() throws ParseException, UnableToParseDateException, TimezoneAmbiguityException, TimezoneException {
        ProvenanceModel model = new ProvenanceModel();
        model.setUri(uri);
        model.setName(name);
        model.setDescription(description);
        if (Objects.nonNull(publisher) && Objects.nonNull(publisher.getUri())) {
            model.setPublisher(publisher.getUri());
        }
        if (Objects.nonNull(publicationDate)) {
            model.setPublicationDate(publicationDate);
        }
        if (activity != null) {
            List<ActivityModel> activities = new ArrayList<>();
            for (ActivityCreationDTO act:activity) {
                ActivityModel activityModel = act.newModel(); 
                activities.add(activityModel);
            }    
            model.setActivity(activities);
        }
        
        model.setAgents(agents);

        return model;
        
    }
}
