/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api.variable;

import java.net.URI;

import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.variable.api.entity.EntityGetDTO;
import org.opensilex.core.variable.api.method.MethodGetDTO;
import org.opensilex.core.variable.api.quality.QualityGetDTO;
import org.opensilex.core.variable.api.trait.TraitDTO;
import org.opensilex.core.variable.api.unit.UnitGetDTO;
import org.opensilex.core.variable.dal.entity.EntityModel;
import org.opensilex.core.variable.dal.method.MethodModel;
import org.opensilex.core.variable.dal.quality.QualityModel;
import org.opensilex.core.variable.dal.trait.TraitModel;
import org.opensilex.core.variable.dal.unit.UnitModel;
import org.opensilex.core.variable.dal.variable.VariableModel;
import org.opensilex.sparql.response.ResourceTreeDTO;


/**
 *
 * @author vidalmor
 */
public class VariableGetDTO {

    private URI uri;

    private String label;

    private String longName;

    private String comment;

    private ResourceTreeDTO entity;

    private QualityGetDTO quality;

    private MethodGetDTO method;

    private TraitDTO trait;

    private UnitGetDTO unit;
    
    private Double lowerBound;
    
    private Double upperBound;

    private String synonym;

    private String dimension;

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

    public String getLongName() { return longName; }

    public void setLongName(String longName) { this.longName = longName; }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public ResourceTreeDTO getEntity() {
        return entity;
    }

    public void setEntity(ResourceTreeDTO entity) {
        this.entity = entity;
    }

    public QualityGetDTO getQuality() {
        return quality;
    }

    public void setQuality(QualityGetDTO quality) { this.quality = quality; }

    public TraitDTO getTrait() { return trait; }

    public void setTrait(TraitDTO trait) { this.trait = trait; }

    public MethodGetDTO getMethod() {
        return method;
    }

    public void setMethod(MethodGetDTO method) {
        this.method = method;
    }

    public UnitGetDTO getUnit() {
        return unit;
    }

    public void setUnit(UnitGetDTO unit) {
        this.unit = unit;
    }

    public Double getLowerBound() { return lowerBound; }

    public void setLowerBound(Double lowerBound) { this.lowerBound = lowerBound; }

    public Double getUpperBound() { return upperBound; }

    public void setUpperBound(Double upperBound) { this.upperBound = upperBound; }

    public String getSynonym() {
        return synonym;
    }

    public void setSynonym(String synonym) {
        this.synonym = synonym;
    }

    public String getDimension() { return dimension; }

    public void setDimension(String dimension) { this.dimension = dimension; }

    public static VariableGetDTO fromModel(VariableModel model) {
        VariableGetDTO dto = new VariableGetDTO();

        dto.setUri(model.getUri());
        dto.setLabel(model.getLabel());
        dto.setComment(model.getComment());
        dto.setLongName(model.getLongName());
        dto.setLowerBound(model.getLowerBound());
        dto.setUpperBound(model.getUpperBound());
        dto.setSynonym(model.getSynonym());
        dto.setDimension(model.getDimension());

        TraitModel traitModel = model.getTrait();
        if(traitModel != null){
            dto.setTrait(TraitDTO.fromModel(traitModel));
        }

        ClassModel entityModel = model.getEntity();
        if(entityModel != null){
            ResourceTreeDTO entityDto = new ResourceTreeDTO();
            entityDto.setName(entityModel.getName());
            entityDto.setUri(entityModel.getUri());
            dto.setEntity(entityDto);
        }

        QualityModel qualityModel = model.getQuality();
        if(qualityModel != null){
            QualityGetDTO qualityGetDTO = new QualityGetDTO();
            qualityGetDTO.setUri(qualityModel.getUri());
            qualityGetDTO.setLabel(qualityModel.getName());
            dto.setQuality(qualityGetDTO);
        }

        MethodModel method = model.getMethod();
        if(method != null){
            MethodGetDTO methodGetDTO = new MethodGetDTO();
            methodGetDTO.setUri(method.getUri());
            methodGetDTO.setLabel(method.getName());
            dto.setMethod(methodGetDTO);
        }

        UnitModel unit = model.getUnit();
        if(unit != null){
            UnitGetDTO unitGetDTO = new UnitGetDTO();
            unitGetDTO.setUri(unit.getUri());
            unitGetDTO.setLabel(unit.getName());
            dto.setUnit(unitGetDTO);
        }

        return dto;
    }
}

