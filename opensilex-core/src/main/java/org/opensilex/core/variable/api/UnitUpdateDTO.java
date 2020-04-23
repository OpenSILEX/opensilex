//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api;

import java.util.List;
import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.server.rest.validation.Required;

public class UnitUpdateDTO {

    @Required
    protected String label;

    protected String comment;

    protected String dimension;

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

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public List<OntologyReference> getRelations() {
        return relations;
    }

    public void setRelations(List<OntologyReference> reference) {
        this.relations = reference;
    }

    public UnitModel newModel() {
        return defineModel(new UnitModel());
    }

    public UnitModel defineModel(UnitModel model) {
        model.setLabel(getLabel());
        model.setComment(getComment());
        model.setDimension(getDimension());
        return model;
    }
}
