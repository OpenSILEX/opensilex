

package org.opensilex.core.provenance.dal;

import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;

import java.net.URI;
import java.util.Collection;
import java.util.List;

public class ProvenanceSearchFilter extends MongoSearchFilter {

    Collection<URI> uris;
    String name;
    String description;
    URI activityType;
    URI activityUri;
    URI agentType;
    List<URI> agents;

    public Collection<URI> getUris() {
        return uris;
    }

    public ProvenanceSearchFilter setUris(Collection<URI> uris) {
        this.uris = uris;
        return this;
    }

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

    public List<URI> getAgents() {
        return agents;
    }

    public ProvenanceSearchFilter setAgents(List<URI> agents) {
        this.agents = agents;
        return this;
    }
}
