//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opensilex.sparql.annotations.SPARQLResourceURI;
import org.opensilex.sparql.annotations.SPARQLTypeRDF;
import org.opensilex.sparql.annotations.SPARQLTypeRDFLabel;

/**
 *
 * @author vidalmor
 */
public abstract class SPARQLResourceModel implements SPARQLModel {
    
    @SPARQLResourceURI()
    protected URI uri;
    public static final String URI_FIELD = "uri";
    
    @SPARQLTypeRDF()
    protected URI type;
    public static final String TYPE_FIELD = "type";
    
    @SPARQLTypeRDFLabel()
    protected SPARQLLabel typeLabel;
    
    protected List<SPARQLModelRelation> relations = new ArrayList<>();
    
    public URI getUri() {
        return uri;
    }
    
    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    public URI getType() {
        return type;
    }
    
    public void setType(URI type) {
        this.type = type;
    }
    
    public SPARQLLabel getTypeLabel() {
        return typeLabel;
    }
    
    public void setTypeLabel(SPARQLLabel typeLabel) {
        this.typeLabel = typeLabel;
    }
    
    public List<SPARQLModelRelation> getRelations() {
        return relations;
    }
    
    public void setRelations(List<SPARQLModelRelation> relations) {
        this.relations = relations;
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + Objects.hashCode(this.uri);
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SPARQLResourceModel other = (SPARQLResourceModel) obj;
        return Objects.equals(this.uri, other.uri);
    }
    
    public void copyToModel(SPARQLResourceModel model) {
        model.setUri(getUri());
        model.setType(getType());
        model.setTypeLabel(getTypeLabel());
        model.setRelations(getRelations());
    }
    
}
