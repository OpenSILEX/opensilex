package org.opensilex.core.variable.api;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.opensilex.core.germplasm.api.GermplasmAPI;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.sharedResource.SharedResourceInstanceDTO;
import org.opensilex.core.species.api.SpeciesDTO;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.core.variable.api.entity.EntityGetDTO;
import org.opensilex.core.variable.api.entityOfInterest.InterestEntityGetDTO;
import org.opensilex.core.variable.api.method.MethodGetDTO;
import org.opensilex.core.variable.api.characteristic.CharacteristicGetDTO;
import org.opensilex.core.variable.api.unit.UnitDetailsDTO;
import org.opensilex.core.variable.dal.*;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;


/**
 *
 * @author Renaud COLIN
 */

@JsonPropertyOrder({
        "uri", "name", "alternative_name", "description", "publisher", "publication_date", "last_updated_date",
        "entity", "entity_of_interest","characteristic", "trait", "trait_name", "method", "unit",
        "species","time_interval", "sampling_interval", "datatype",
        SKOSReferencesDTO.EXACT_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.CLOSE_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.BROAD_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.NARROW_MATCH_JSON_PROPERTY
})

public class VariableDetailsDTO extends BaseVariableDetailsDTO<VariableModel> {

    @JsonProperty("alternative_name")
    private String alternativeName;

    @JsonProperty("publisher")
    private UserGetDTO publisher;

    @JsonProperty("publication_date")
    private OffsetDateTime publicationDate;

    @JsonProperty("last_updated_date")
    private OffsetDateTime lastUpdatedDate;

    @JsonProperty("entity")
    private EntityGetDTO entity;
    
    @JsonProperty("entity_of_interest")
    private InterestEntityGetDTO entityOfInterest;
    
    @JsonProperty("characteristic")
    private CharacteristicGetDTO characteristic;

    @JsonProperty("method")
    private MethodGetDTO method;

    @JsonProperty("unit")
    private UnitDetailsDTO unit;

    @JsonProperty("trait")
    private URI trait;

    @JsonProperty("trait_name")
    private String traitName;

    @JsonProperty("species")
    private List<SpeciesDTO> species;

    @JsonProperty("time_interval")
    private String timeInterval;

    @JsonProperty("sampling_interval")
    private String samplingInterval;

    @JsonProperty("datatype")
    private URI dataType;

    public VariableDetailsDTO(VariableModel model) {
        this(model, null);
    }
    
    public VariableDetailsDTO(VariableModel model, SharedResourceInstanceDTO sharedResourceInstance) {
        super(model, sharedResourceInstance);

        EntityModel entity = model.getEntity();
        this.entity = new EntityGetDTO(entity);
        
        InterestEntityModel entityOfInterest = model.getEntityOfInterest();
        if(entityOfInterest != null){
            this.entityOfInterest = new InterestEntityGetDTO(entityOfInterest);
        }
        
        CharacteristicModel characteristic = model.getCharacteristic();
        this.characteristic = new CharacteristicGetDTO(characteristic);

        MethodModel method = model.getMethod();
        this.method = new MethodGetDTO(method);

        UnitModel unit = model.getUnit();
        this.unit = new UnitDetailsDTO(unit);

        this.alternativeName = model.getAlternativeName();

        if (model.getPublicationDate() != null) {
            this.publicationDate = model.getPublicationDate();
        }

        if (model.getLastUpdateDate() != null) {
            this.lastUpdatedDate = model.getLastUpdateDate();
        }

        if(model.getSpecies() != null){
            List<SpeciesDTO> dtos = new ArrayList<>();
            for (SpeciesModel species : model.getSpecies()){
                dtos.add(SpeciesDTO.fromModel(species));
            }
            this.species = dtos;
        }

        this.timeInterval = model.getTimeInterval();
        this.samplingInterval = model.getSamplingInterval();

        URI dataType = model.getDataType();
        try {
            this.dataType = new URI(SPARQLDeserializers.getExpandedURI(dataType));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        trait = model.getTraitUri();
        traitName = model.getTraitName();
    }

    public VariableDetailsDTO() {
    }

    @Override
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/Plant_Height")
    public URI getUri() {
        return uri;
    }

    @Override
    @ApiModelProperty(example = "Plant_Height")
    public String getName() {
        return name;
    }

    @ApiModelProperty(example = "Plant_Height_Estimation_Cm")
    public String getAlternativeName() {
        return alternativeName;
    }

    public void setAlternativeName(String alternativeName) {
        this.alternativeName = alternativeName;
    }

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
        this.publisher = publisher;
    }

    public OffsetDateTime getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(OffsetDateTime publicationDate) {
        this.publicationDate = publicationDate;
    }

    public OffsetDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(OffsetDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    @Override
    @ApiModelProperty(example = "Describe the height of a plant.")
    public String getDescription() {
        return description;
    }

    public EntityGetDTO getEntity() { return entity; }

    public void setEntity(EntityGetDTO entity) {
        this.entity = entity;
    }
    
    public InterestEntityGetDTO getEntityOfInterest() { return entityOfInterest; }
    
    public void setEntityOfInterest(InterestEntityGetDTO  entityOfInterest){
        this.entityOfInterest = entityOfInterest;
    }
    
    public CharacteristicGetDTO getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(CharacteristicGetDTO characteristic) {
        this.characteristic = characteristic;
    }

    public MethodGetDTO getMethod() {
        return method;
    }

    public void setMethod(MethodGetDTO method) {
        this.method = method;
    }

    public UnitDetailsDTO getUnit() {
        return unit;
    }

    public void setUnit(UnitDetailsDTO unit) {
        this.unit = unit;
    }

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


    @ApiModelProperty(notes = "Define the time between two data recording", example = "minutes")
    public String getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(String timeInterval) {
        this.timeInterval = timeInterval;
    }

    @ApiModelProperty(notes = "Define the distance between two data recording", example = "minutes")
    public String getSamplingInterval() {
        return samplingInterval;
    }

    public void setSamplingInterval(String samplingInterval) {
        this.samplingInterval = samplingInterval;
    }

    public URI getDataType() {
        return dataType;
    }

    @ApiModelProperty(notes = "XSD type of the data associated with the variable", example = "http://www.w3.org/2001/XMLSchema#integer")
    public void setDataType(URI dataType) {
        this.dataType = dataType;
    }

    @ValidURI
    @ApiModelProperty(notes = "Species associated with the variable", example = GermplasmAPI.GERMPLASM_EXAMPLE_SPECIES)
    public List<SpeciesDTO> getSpecies() {
        return species;
    }

    public void setSpecies(List<SpeciesDTO> species) {
        this.species = species;
    }

    @Override
    public VariableModel toModel() {
        VariableModel model = new VariableModel();
        setBasePropertiesToModel(model);
        model.setEntity(new EntityModel(this.getEntity().getUri()));

        if (this.getEntityOfInterest() != null) {
            model.setEntityOfInterest(new InterestEntityModel(this.getEntityOfInterest().getUri()));
        }

        model.setCharacteristic(new CharacteristicModel(this.getCharacteristic().getUri()));
        model.setMethod(new MethodModel(this.getMethod().getUri()));
        model.setUnit(new UnitModel(this.getUnit().getUri()));

        if (this.getSpecies() != null) {
            model.setSpecies(this.getSpecies().stream()
                    .map(species -> new SpeciesModel(species.getUri()))
                    .collect(Collectors.toList()));
        }

        model.setAlternativeName(this.getAlternativeName());
        model.setDataType(this.getDataType());
        model.setSamplingInterval(this.getSamplingInterval());
        model.setTimeInterval(this.getTimeInterval());
        model.setTraitName(this.getTraitName());
        model.setTraitUri(this.getTrait());
        return model;
    }

}

