//******************************************************************************
//                          ProvenanceModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import java.util.List;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 * Provenance model
 *
 * @author Alice Boizet
 */
public class ProvenanceModel extends MongoModel {
  
    String name;
    String description;

    List<ActivityModel> activity;

    List<AgentModel> agents;

    public static final String NAME_FIELD = "name";
    public static final String DESCRIPTION_FIELD = "description";
    public static final String ACTIVITY_TYPE_FIELD = "activity.rdfType";
    public static final String ACTIVITY_URI_FIELD = "activity.uri";
    public static final String AGENTS_TYPE_FIELD = "agents.rdfType";
    public static final String AGENTS_URI_FIELD = "agents.uri";

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
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[]{
            this.getName()
        };
    }

}
