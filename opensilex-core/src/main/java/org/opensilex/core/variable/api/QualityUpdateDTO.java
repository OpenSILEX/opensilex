//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api;

import java.util.*;
import org.opensilex.core.ontology.*;
import org.opensilex.core.variable.dal.*;
import org.opensilex.server.validation.*;

public class QualityUpdateDTO {

    @Required
    protected String label;

    protected String comment;

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
        return updateModel(new QualityModel());
    }

    public QualityModel updateModel(QualityModel model) {
        if (getLabel() != null) {
            model.setName(getLabel());
        }
        if (getComment() != null) {
            model.setComment(getComment());
        }

        return model;
    }
}
