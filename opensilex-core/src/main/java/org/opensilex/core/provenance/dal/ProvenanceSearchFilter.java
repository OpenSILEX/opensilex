package org.opensilex.core.provenance.dal;

import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;
import java.net.URI;

public class ProvenanceSearchFilter extends MongoSearchFilter {

    private String name;
    private String description;
    private URI activityType;
    private URI activityUri;
    private URI agentType;
    private URI agentURI;

    public String getName() {
        return name;
    }

    public ProvenanceSearchFilter setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ProvenanceSearchFilter setDescription(String description) {
        this.description = description;
        return this;
    }

    public URI getActivityType() {
        return activityType;
    }

    public ProvenanceSearchFilter setActivityType(URI activityType) {
        this.activityType = activityType;
        return this;
    }

    public URI getActivityUri() {
        return activityUri;
    }

    public ProvenanceSearchFilter setActivityUri(URI activityUri) {
        this.activityUri = activityUri;
        return this;
    }

    public URI getAgentType() {
        return agentType;
    }

    public ProvenanceSearchFilter setAgentType(URI agentType) {
        this.agentType = agentType;
        return this;
    }

    public URI getAgentURI() {
        return agentURI;
    }

    public ProvenanceSearchFilter setAgentURI(URI agentURI) {
        this.agentURI = agentURI;
        return this;
    }
}
