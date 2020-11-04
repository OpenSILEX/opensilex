//******************************************************************************
//                          ProvenanceModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import java.net.URI;
import javax.jdo.query.BooleanExpression;
import org.opensilex.nosql.model.NoSQLModel;
import javax.jdo.annotations.NotPersistent;
import java.util.List;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 * Provenance model
 * @author Alice Boizet
 */
public class ProvenanceModel extends MongoModel {
    
    //URI uri;    
    String label;
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
            this.getLabel()
        };
    }
    
    
}
