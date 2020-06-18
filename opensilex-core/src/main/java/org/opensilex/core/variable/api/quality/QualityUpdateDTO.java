//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.quality;

import java.net.URI;
import java.util.List;
import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.quality.QualityModel;
import org.opensilex.server.rest.validation.Required;

public class QualityUpdateDTO extends SKOSReferencesDTO {

    @Required
    protected String label;

    protected String comment;

    protected URI type;

    protected List<OntologyReference> relations;

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

    public QualityModel newModel() {
        return defineModel(new QualityModel());
    }

    public QualityModel defineModel(QualityModel model) {
        model.setName(getLabel());
        model.setComment(getComment());
        setSkosReferencesToModel(model);
        return model;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }
}
