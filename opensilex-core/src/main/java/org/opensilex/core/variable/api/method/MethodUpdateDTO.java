//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.method;

import java.net.URI;
import java.util.List;
import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.method.MethodModel;
import org.opensilex.server.rest.validation.Required;

public class MethodUpdateDTO extends SKOSReferencesDTO {

    @Required
    private String label;

    private String comment;

    private List<OntologyReference> relations;

    private URI type;

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

    public void setRelations(List<OntologyReference> reference) {
        this.relations = reference;
    }

    public URI getType() { return type; }

    public void setType(URI type) { this.type = type; }

    public MethodModel newModel() {
        return defineModel(new MethodModel());
    }

    public MethodModel defineModel(MethodModel model) {
        model.setName(label);
        model.setComment(comment);
        if(type != null){
            model.setType(type);
        }
        setSkosReferencesToModel(model);
        return model;
    }
}
