//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.variable;

import java.net.URI;
import java.util.List;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.variable.api.trait.TraitDTO;
import org.opensilex.core.variable.dal.entity.EntityModel;
import org.opensilex.core.variable.dal.method.MethodModel;
import org.opensilex.core.variable.dal.quality.QualityModel;
import org.opensilex.core.variable.dal.trait.TraitModel;
import org.opensilex.core.variable.dal.unit.UnitModel;
import org.opensilex.core.variable.dal.variable.VariableModel;
import org.opensilex.server.rest.validation.Required;

public class VariableUpdateDTO {

    @NotNull
    protected String label;

    protected String longName;

    protected String comment;

    protected URI entity;

    protected URI quality;

    protected TraitDTO trait;

    protected URI method;

    protected URI unit;

    private Double lowerBound;

    private Double upperBound;

    private String synonym;

    private String dimension;

    protected List<OntologyReference> relations;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
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

    public TraitDTO getTrait() {
        return trait;
    }

    public Double getLowerBound() {
        return lowerBound;
    }

    public Double getUpperBound() {
        return upperBound;
    }

    public VariableModel newModel() {
        return defineModel(new VariableModel());
    }

    public String getSynonym() {
        return synonym;
    }

    public String getDimension() {
        return dimension;
    }

    public VariableModel defineModel(VariableModel model) {

        model.setLabel(getLabel());
        model.setLongName(getLongName());
        model.setComment(getComment());

        if(entity != null){
            ClassModel entityModel = new ClassModel();
            entityModel.setUri(entity);
            model.setEntity(entityModel);
        }
        if(quality != null){
            model.setQuality(new QualityModel(getQuality()));
        }
        if(trait != null && !StringUtils.isEmpty(trait.getTraitLabel())){
            model.setTrait(trait.newModel());
        }
        if(method != null){
            model.setMethod(new MethodModel(method));
        }
        if(unit != null){
            model.setUnit(new UnitModel(unit));
        }

        model.setLowerBound(getLowerBound());
        model.setUpperBound(getUpperBound());
        model.setSynonym(getSynonym());
        model.setDimension(getDimension());
        return model;
    }
}
