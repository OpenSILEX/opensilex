//******************************************************************************
//                          ProvenanceModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import java.net.URI;
import java.util.List;
import javax.jdo.annotations.Convert;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.query.BooleanExpression;
import org.opensilex.nosql.model.NoSQLModel;

/**
 *
 * @author boizetal
 */
@PersistenceCapable(table = "provenance")
public class ProvenanceModel implements NoSQLModel{
    
    @Convert(URIStringConverter.class)
    URI uri;    
    String label;
    String comment;
    
    @Convert(ListURIStringConverter.class)
    @Persistent(defaultFetchGroup="true")
    List<URI> experiments;
    
    @Persistent(defaultFetchGroup="true")
    @Embedded
    AgentModel agent;

    public URI getUri() {
        return uri;
    }

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

    public List<URI> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<URI> experiments) {
        this.experiments = experiments;
    }

    public AgentModel getAgent() {
        return agent;
    }

    public void setAgent(AgentModel agent) {
        this.agent = agent;
    }

    @Override
    public String getGraphPrefix() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T extends NoSQLModel> T update(T instance) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BooleanExpression getURIExpr(URI uri) {
        QProvenanceModel candidate = QProvenanceModel.candidate();
        return candidate.uri.eq(uri);
    }

    @Override
    public String[] getUriSegments(NoSQLModel instance) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


   
}
