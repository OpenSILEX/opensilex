//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.collections4.CollectionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.opensilex.core.germplasm.api.GermplasmAPI;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.core.variable.dal.*;
import org.opensilex.server.rest.validation.ValidURI;

import javax.validation.constraints.NotNull;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

/**
 * @author vidalmor
 */
@JsonPropertyOrder({
    "uri", "name", "alternative_name", "description",
    "entity", "entity_of_interest","characteristic", "trait", "trait_name", "method", "unit",
    "species","datatype","time_interval", "sampling_interval",
    "exact_match","close_match","broader","narrower"
})
public class VariableCreationDTO extends SKOSReferencesDTO {

    @JsonProperty("uri")
    protected URI uri;

    @NotNull
    @JsonProperty("name")
    private String name;

    @JsonProperty("alternative_name")
    private String alternativeName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("entity")
    private URI entity;
    
    @JsonProperty("entity_of_interest")
    private URI entityOfInterest;

    @JsonProperty("characteristic")
    private URI characteristic;

    @JsonProperty("trait")
    private URI trait;

    @JsonProperty("trait_name")
    private String traitName;

    @JsonProperty("method")
    private URI method;

    @JsonProperty("unit")
    private URI unit;

    @JsonProperty("species")
    private List<URI> species;

    @JsonProperty("time_interval")
    private String timeInterval;

    @JsonProperty("sampling_interval")
    private String samplingInterval;

    @JsonProperty("datatype")
    private URI dataType;

    @ValidURI
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/Plant_Height")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }


    @ApiModelProperty(example = "Plant_Height", required = true)
    @NotNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "Plant_Height_Estimation_Cm")
    public String getAlternativeName() {
        return alternativeName;
    }

    public void setAlternativeName(String alternativeName) {
        this.alternativeName = alternativeName;
    }

    @ApiModelProperty(example = "Describe the height of a plant.")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ValidURI
    @NotNull
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity/Plant", required = true)
    public URI getEntity() {
        return entity;
    }

    public void setEntity(URI entity) {
        this.entity = entity;
    }

    @ValidURI
    @ApiModelProperty(example = "http://opensilex.dev/opensilex/id/plantMaterialLot#SL_001")
    public URI getEntityOfInterest() {
        return entityOfInterest;
    }

    public void setEntityOfInterest(URI entityOfInterest) {
        this.entityOfInterest = entityOfInterest;
    }
    
    @ValidURI
    @NotNull
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/characteristic/Height", required = true)
    public URI getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(URI characteristic) {
        this.characteristic = characteristic;
    }
    
    @ValidURI
    @NotNull
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/method/Estimation")
    public URI getMethod() {
        return method;
    }

    public void setMethod(URI method) {
        this.method = method;
    }

    @ValidURI
    @NotNull
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/unit/centimeter", required = true)
    public URI getUnit() {
        return unit;
    }

    public void setUnit(URI unit) {
        this.unit = unit;
    }

    @ValidURI
    @ApiModelProperty(notes = "Additional trait URI. Could be used for interoperability", example = "http://purl.obolibrary.org/obo/TO_0002644")
    public URI getTrait() {
        return trait;
    }

    public void setTrait(URI trait) {
        this.trait = trait;
    }

    @ApiModelProperty(notes = "Additional trait name. Could be used for interoperability if you describe the trait URI", example = "dry matter digestibility")
    public String getTraitName() {
        return traitName;
    }

    public void setTraitName(String traitName) {
        this.traitName = traitName;
    }

    @ValidURI
    @NotNull
    @ApiModelProperty(notes = "XSD type of the data associated with the variable", example = "http://www.w3.org/2001/XMLSchema#integer")
    public URI getDataType() { return dataType; }

    public void setDataType(URI dataType) { this.dataType = dataType; }

    @ApiModelProperty(notes = "Define the time between two data recording", example = "minutes")
    public String getTimeInterval() { return timeInterval; }

    public void setTimeInterval(String timeInterval) { this.timeInterval = timeInterval; }

    @ApiModelProperty(notes = "Define the distance between two data recording", example = "minutes")
    public String getSamplingInterval() { return samplingInterval; }

    public void setSamplingInterval(String samplingInterval) { this.samplingInterval = samplingInterval; }

    @ValidURI
    @ApiModelProperty(notes = "URI of the species associated with the variable", example = GermplasmAPI.GERMPLASM_EXAMPLE_SPECIES)
    public List<URI> getSpecies() {
        return species;
    }

    public void setSpecies(List<URI> species) {
        this.species = species;
    }

    public VariableModel newModel() {
        VariableModel model = new VariableModel();
        model.setUri(uri);
        model.setName(name);

        if(!StringUtils.isEmpty(alternativeName)){
            model.setAlternativeName(alternativeName);
        }
        if(!StringUtils.isEmpty(description)){
            model.setDescription(description);
        }
        model.setDataType(dataType);

        model.setEntity(new EntityModel(entity));
        
        if(entityOfInterest != null){
            InterestEntityModel entityOfInterest2 = new InterestEntityModel();
            entityOfInterest2.setUri(entityOfInterest);
            model.setEntityOfInterest(entityOfInterest2);            
        }
        
        model.setCharacteristic(new CharacteristicModel(characteristic));       
        model.setMethod(new MethodModel(method));
        model.setUnit(new UnitModel(unit));

        if (!CollectionUtils.isEmpty(species)){
            List<SpeciesModel> speciesModelList = new ArrayList<>();
            for(URI uri : species){
                SpeciesModel speciesModel = new SpeciesModel();
                speciesModel.setUri(uri);
                speciesModelList.add(speciesModel);
            };
            model.setSpecies(speciesModelList);

        }

        if(trait != null){
            model.setTraitUri(trait);
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
