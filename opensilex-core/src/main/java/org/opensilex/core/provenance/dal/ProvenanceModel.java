//******************************************************************************
//                          ProvenanceModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import java.net.URI;
import java.util.List;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 * Provenance model
 *
 * @author Alice Boizet
 */
public class ProvenanceModel extends MongoModel {

    //URI uri;    
    String name;
    String comment;
    List<URI> experiments;

    List<ActivityModel> activity;

    List<AgentModel> agents;

//    public URI getUri() {
//        return uri;
//    }
//
//    public void setUri(URI uri) {
//        this.uri = uri;
//    }
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

    @Override
    public String[] getUriSegments(MongoModel instance) {
        return new String[]{
            this.getName()
        };
    }

}
