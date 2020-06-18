//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.entity;

import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.entity.EntityModel;
import org.opensilex.server.rest.validation.Required;

import java.net.URI;
import java.util.List;

public class EntityUpdateDTO extends SKOSReferencesDTO {

    @Required
    protected String label;

    protected String comment;

    protected List<OntologyReference> relations;

    protected URI type;

    public EntityModel newModel() {
        return defineModel(new EntityModel());
    }

    public EntityModel defineModel(EntityModel model) {
        model.setName(label);
        model.setComment(comment);
        if(type != null){
            model.setType(type);
        }
        setSkosReferencesToModel(model);
        return model;
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

    public List<OntologyReference> getRelations() {
        return relations;
    }

    public void setRelations(List<OntologyReference> relations) {
        this.relations = relations;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }
}
