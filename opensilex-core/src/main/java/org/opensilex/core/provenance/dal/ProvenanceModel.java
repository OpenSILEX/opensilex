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

    public static final String NAME_FIELD = "name";

    String description;

    public static final String DESCRIPTION_FIELD = "description";


    List<ActivityModel> activity;

    public static final String ACTIVITY_FIELD = "activity";


    List<AgentModel> agents;

    public static final String AGENTS_FIELD = "agents";


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
