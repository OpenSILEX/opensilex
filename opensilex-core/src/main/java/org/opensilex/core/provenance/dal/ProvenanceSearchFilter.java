package org.opensilex.core.provenance.dal;

import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;
import java.net.URI;

public class ProvenanceSearchFilter extends MongoSearchFilter {

    String name;
    String description;
    URI activityType;
    URI activityUri;
    URI agentType;
    URI agentURI;

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

    public URI getActivityType() {
        return activityType;
    }

    public void setActivityType(URI activityType) {
        this.activityType = activityType;
    }

    public URI getAgentType() {
        return agentType;
    }

    public void setAgentType(URI agentType) {
        this.agentType = agentType;
    }

    public URI getActivityUri() {
        return activityUri;
    }

    public void setActivityUri(URI activityUri) {
        this.activityUri = activityUri;
    }

    public URI getAgentURI() {
        return agentURI;
    }

    public void setAgentURI(URI agentURI) {
        this.agentURI = agentURI;
    }
}
