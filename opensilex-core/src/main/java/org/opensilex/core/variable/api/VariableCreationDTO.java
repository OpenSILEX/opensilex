/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api;

import java.net.URI;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.QualityModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableModel;

import javax.validation.constraints.NotNull;

/**
 * @author vidalmor
 */
public class VariableCreationDTO extends SKOSReferencesDTO {

    protected URI uri;

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/Plant_Height")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @NotNull
    private String name;

    private String longName;

    private String comment;

    private URI entity;

    private URI quality;

    private URI traitUri;

    private String traitName;

    private URI method;

    private URI unit;

    private String synonym;

    private String dimension;

    private List<OntologyReference> relations;

    @ApiModelProperty(example = "Plant_Height")
    @NotNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "Plant_Height_Estimation_Cm")
    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    @ApiModelProperty(example = "Describe the height of a plant.")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @NotNull
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getEntity() {
        return entity;
    }

    public void setEntity(URI entity) {
        this.entity = entity;
    }

    @NotNull
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/quality/Height")
    public URI getQuality() {
        return quality;
    }

    public void setQuality(URI quality) {
        this.quality = quality;
    }

    @NotNull
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/method/Estimation")
    public URI getMethod() {
        return method;
    }

    public void setMethod(URI method) {
        this.method = method;
    }

    @NotNull
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/unit/centimeter")
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

    @ApiModelProperty(notes = "Additional trait URI. Could be used for interoperability", example = "http://purl.obolibrary.org/obo/TO_0002644")
    public URI getTraitUri() {
        return traitUri;
    }

    public void setTraitUri(URI traitUri) {
        this.traitUri = traitUri;
    }

    @ApiModelProperty(notes = "Additional trait name. Could be used for interoperability if you describe the trait URI", example = "dry matter digestibility")
    public String getTraitName() {
        return traitName;
    }

    public void setTraitName(String traitName) {
        this.traitName = traitName;
    }

    public void setSynonym(String synonym) {
        this.synonym = synonym;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public VariableModel newModel() {
        VariableModel model = new VariableModel();
        model.setUri(uri);
        model.setName(getName());
        model.setLongName(getLongName());
        model.setComment(getComment());

        model.setEntity(new EntityModel(getEntity()));
        model.setQuality(new QualityModel(getQuality()));
        model.setMethod(new MethodModel(method));
        model.setUnit(new UnitModel(unit));

        model.setTraitUri(traitUri);
        model.setTraitName(traitName);

        model.setSynonym(getSynonym());
        model.setDimension(getDimension());
        setSkosReferencesToModel(model);
        return model;
    }

    @ApiModelProperty(example = "Plant_Length")
    public String getSynonym() {
        return synonym;
    }

    @ApiModelProperty(notes = "Describe the dimension on which variables values are integrated", example = "minutes")
    public String getDimension() {
        return dimension;
    }
}
