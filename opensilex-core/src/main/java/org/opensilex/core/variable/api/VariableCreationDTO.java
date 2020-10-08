//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api;

import java.net.URI;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import org.codehaus.plexus.util.StringUtils;
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

    private String timeInterval;

    private String samplingInterval;


    private URI dataType;

    private List<OntologyReference> relations;

    @ApiModelProperty(example = "Plant_Height", required = true)
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
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity/Plant", required = true)
    public URI getEntity() {
        return entity;
    }

    public void setEntity(URI entity) {
        this.entity = entity;
    }

    @NotNull
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/quality/Height", required = true)
    public URI getQuality() {
        return quality;
    }

    public void setQuality(URI quality) {
        this.quality = quality;
    }

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/method/Estimation")
    public URI getMethod() {
        return method;
    }

    public void setMethod(URI method) {
        this.method = method;
    }

    @NotNull
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/unit/centimeter", required = true)
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

    @ApiModelProperty(example = "Plant_Length")
    public String getSynonym() {
        return synonym;
    }

    public void setSynonym(String synonym) {
        this.synonym = synonym;
    }

    @ApiModelProperty(notes = "Define the time between two data recording", example = "minutes")
    public String getTimeInterval() { return timeInterval; }

    public void setTimeInterval(String timeInterval) { this.timeInterval = timeInterval; }

    @ApiModelProperty(notes = "Define the distance between two data recording", example = "minutes")
    public String getSamplingInterval() { return samplingInterval; }

    public void setSamplingInterval(String samplingInterval) { this.samplingInterval = samplingInterval; }

    public URI getDataType() { return dataType; }

    @ApiModelProperty(notes = "XSD type of the data associated with the variable", example = "http://www.w3.org/2001/XMLSchema#integer")
    public void setDataType(URI dataType) { this.dataType = dataType; }

    public VariableModel newModel() {
        VariableModel model = new VariableModel();
        model.setUri(uri);
        model.setName(name);

        if(!StringUtils.isEmpty(longName)){
            model.setLongName(longName);
        }
        if(!StringUtils.isEmpty(comment)){
            model.setComment(comment);
        }
        model.setDataType(dataType);

        model.setEntity(new EntityModel(entity));
        model.setQuality(new QualityModel(quality));
        if(method != null){
            model.setMethod(new MethodModel(method));
        }
        model.setUnit(new UnitModel(unit));

        if(traitUri != null){
            model.setTraitUri(traitUri);
            model.setTraitName(traitName);
        }
        if(!StringUtils.isEmpty(timeInterval)){
            model.setTimeInterval(timeInterval);
        }
        if(!StringUtils.isEmpty(samplingInterval)){
            model.setSamplingInterval(samplingInterval);
        }
        setSkosReferencesToModel(model);
        return model;
    }

}
