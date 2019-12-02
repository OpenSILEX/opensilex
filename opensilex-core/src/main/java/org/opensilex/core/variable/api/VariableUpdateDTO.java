//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api;

import java.net.URI;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.QualityModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.server.validation.Required;


public class VariableUpdateDTO {

    @Required
    protected String label;

    protected String comment;

    @NotNull
    protected URI entity;

    @NotNull
    protected URI quality;

    @NotNull
    protected URI method;

    @NotNull
    protected URI unit;

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

    public URI getEntity() {
        return entity;
    }

    public void setEntity(URI entity) {
        this.entity = entity;
    }

    public URI getQuality() {
        return quality;
    }

    public void setQuality(URI quality) {
        this.quality = quality;
    }

    public URI getMethod() {
        return method;
    }

    public void setMethod(URI method) {
        this.method = method;
    }

    public URI getUnit() {
        return unit;
    }

    public void setUnit(URI unit) {
        this.unit = unit;
    }

    public List<OntologyReference> getRelations() {
        return relations;
    }

    public void setRelations(List<OntologyReference> reference) {
        this.relations = reference;
    }

    public VariableModel newModel() {
        return updateModel(new VariableModel());
    }

    public VariableModel updateModel(VariableModel model) {
        if (getLabel() != null) {
            model.setLabel(getLabel());
        }
        if (getComment() != null) {
            model.setComment(getComment());
        }
        if (getEntity() != null) {
            model.setEntity(new EntityModel(getEntity()));
        }
        if (getQuality() != null) {
            model.setQuality(new QualityModel(getQuality()));
        }
        if (getMethod() != null) {
            model.setMethod(new MethodModel(getMethod()));
        }
        if (getUnit() != null) {
            model.setUnit(new UnitModel(getUnit()));
        }

        return model;
    }
}
