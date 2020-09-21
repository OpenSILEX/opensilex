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
import javax.jdo.annotations.Convert;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Provenance model
 * @author Alice Boizet
 */
@PersistenceCapable(table = "provenance")
public class ProvenanceModel implements NoSQLModel{
    @NotPersistent
    private final String baseURI = "id/provenance";
    @NotPersistent
    private String[] URICompose;
    
    @PrimaryKey
    URI uri;    
    String label;
    String comment;
    
    @Element(embeddedMapping={
    @Embedded(members={
        @Persistent(name="uri", column="uri")})
    })
    @Join(column="uri")
    @Persistent(defaultFetchGroup="true")
    List<ExperimentProvModel> experiments;    
    
    @Element(embeddedMapping={
    @Embedded(members={
        @Persistent(name="type", column="rdf:type"),
        @Persistent(name="startedAtTime", column="startedAtTime"),
        @Persistent(name="endedAtTime", column="endedAtTime"),
        @Persistent(name="settings", column="settings")})
    })
    @Join(column="uri")
    @Persistent(defaultFetchGroup="true")
    @Column(name="prov:Activity")
    List<ActivityModel> activity;
    
    @Element(embeddedMapping={
    @Embedded(members={
        @Persistent(name="uri", column="uri"),
        @Persistent(name="type", column="rdf:type"),
        @Persistent(name="settings", column="settings")})
    })
    @Join(column="uri")
    @Persistent(defaultFetchGroup="true")
    @Column(name="prov:Agent")
    List<AgentModel> agents;

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public void setUri(URI uri) {
        this.uri = uri;
    }

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

    public List<ExperimentProvModel> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<ExperimentProvModel> experiments) {
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

    public void setURICompose(String[] elt){
        this.URICompose = elt;
    }
    
    @Override
    public String getGraphPrefix() {
        return baseURI;
    }

    @Override
    public <T extends NoSQLModel> T update(T instance) {
        ProvenanceModel newInstance =  new ProvenanceModel();
        ProvenanceModel updateInstance = (ProvenanceModel) instance;

        newInstance.setUri(instance.getUri());
        if(updateInstance.getLabel() !=  null)
            newInstance.setLabel(updateInstance.getLabel());
        else
            newInstance.setLabel(label);

        if(updateInstance.getComment() != null)
            newInstance.setComment(updateInstance.getComment());
        else
            newInstance.setComment(comment);

        if(updateInstance.getExperiments()!=  null)
            newInstance.setExperiments(updateInstance.getExperiments());
        else
            newInstance.setExperiments(experiments);

        if(updateInstance.getActivity() !=  null)
            newInstance.setActivity(updateInstance.getActivity());
        else
            newInstance.setActivity(activity);

        if(updateInstance.getAgents() !=  null)
            newInstance.setAgents(updateInstance.getAgents());
        else
            newInstance.setAgents(agents);

        return (T) newInstance;
    }

    @Override
    public BooleanExpression getURIExpr(URI uri) {
        QProvenanceModel candidate = QProvenanceModel.candidate();
        return candidate.uri.eq(uri);
    }

    @Override
    public String[] getUriSegments(NoSQLModel instance) {
        String[] lab = {this.label};
        return lab;
    }

}
