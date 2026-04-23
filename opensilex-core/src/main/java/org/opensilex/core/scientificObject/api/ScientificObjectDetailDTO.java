/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.geojson.GeoJsonObject;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.response.NamedResourceDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.server.rest.serialization.CustomParamConverterProvider;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

/**
 *
 * @author vmigot
 */
@JsonPropertyOrder({"uri", "publisher", "publication_date", "last_updated_date", "rdf_type", "rdf_type_name", "name", "parent", "parent_name", "factor_level", "relations", "location"})
public class ScientificObjectDetailDTO extends NamedResourceDTO<ScientificObjectModel> {

    @ApiModelProperty(value = "Scientific object parent URI")
    protected URI parent;

    @JsonProperty("publisher")
    protected UserGetDTO publisher;

    @JsonProperty("parent_name")
    @ApiModelProperty(value = "Scientific object parent name")
    protected String parentLabel;

    @JsonProperty("rdf_type")
    @ApiModelProperty(value = "Scientific object type", example = ScientificObjectAPI.SCIENTIFIC_OBJECT_EXAMPLE_TYPE)
    protected URI type;

    @JsonProperty("rdf_type_name")
    @ApiModelProperty(value = "Scientific object type", example = ScientificObjectAPI.SCIENTIFIC_OBJECT_EXAMPLE_TYPE_NAME)
    protected String typeLabel;

    @JsonProperty("factor_level")
    @ApiModelProperty(value = "Scientific object factor levels")
    protected List<NamedResourceDTO<FactorLevelModel>> factorLevels;

    @Deprecated
    @ApiModelProperty("Object geometry. Depreciated : use location instead")
    private GeoJsonObject geometry;

    protected List<RDFObjectRelationDTO> relations;

    private LocationObservationDTO location;

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
        this.publisher = publisher;
    }

    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    public String getParentLabel() {
        return parentLabel;
    }

    public void setParentLabel(String parentLabel) {
        this.parentLabel = parentLabel;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public List<NamedResourceDTO<FactorLevelModel>> getFactorLevels() {
        return factorLevels;
    }

    public void setFactorLevels(List<NamedResourceDTO<FactorLevelModel>> factorLevels) {
        this.factorLevels = factorLevels;
    }

    public List<RDFObjectRelationDTO> getRelations() {
        return relations;
    }

    public void setRelations(List<RDFObjectRelationDTO> relations) {
        this.relations = relations;
    }

    public LocationObservationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationObservationDTO location) {
        this.location = location;
    }

    @Deprecated
    public GeoJsonObject getGeometry() {
        return geometry;
    }

    @Deprecated
    public void setGeometry(GeoJsonObject geometry) {
        this.geometry = geometry;
    }

    @Override
    public void toModel(ScientificObjectModel model) {
        super.toModel(model);
    }

    @Override
    public void fromModel(ScientificObjectModel model) {
        super.fromModel(model);
        setType(model.getType());
        setTypeLabel(model.getTypeLabel().getDefaultValue());

        List<RDFObjectRelationDTO> relationsDTO = new ArrayList<>(model.getRelations().size() + 1);

        RDFObjectRelationDTO isPartOfRelation = new RDFObjectRelationDTO();

        if (model.getParent() != null) {
            setParent(model.getParent().getUri());
            setParentLabel(model.getParent().getName());

            URI isPartOfURI;
            try {
                isPartOfURI = SPARQLDeserializers.formatURI(new URI(Oeso.isPartOf.getURI()));
                isPartOfRelation.setProperty(isPartOfURI);
                isPartOfRelation.setValue(SPARQLDeserializers.formatURI(getParent()).toString());
                relationsDTO.add(isPartOfRelation);
            } catch (URISyntaxException ex) {
            }
        }

        if (model.getCreationDate() != null) {
            try {
                RDFObjectRelationDTO dateRelation = new RDFObjectRelationDTO();
                dateRelation.setProperty(SPARQLDeserializers.formatURI(new URI(Oeso.hasCreationDate.getURI())));
                dateRelation.setValue(CustomParamConverterProvider.localDateConverter.toString(model.getCreationDate()));
                relationsDTO.add(dateRelation);
            } catch (URISyntaxException ex) {
            }
        }

        if (model.getDestructionDate() != null) {
            try {
                RDFObjectRelationDTO dateRelation = new RDFObjectRelationDTO();
                dateRelation.setProperty(SPARQLDeserializers.formatURI(new URI(Oeso.hasDestructionDate.getURI())));
                dateRelation.setValue(CustomParamConverterProvider.localDateConverter.toString(model.getDestructionDate()));
                relationsDTO.add(dateRelation);
            } catch (URISyntaxException ex) {
            }
        }

        for (SPARQLModelRelation relation : model.getRelations()) {
            relationsDTO.add(RDFObjectRelationDTO.getDTOFromModel(relation));
        }

        setRelations(relationsDTO);

        if(model.getFactorLevels() != null){
            for (FactorLevelModel level : model.getFactorLevels()) {
                SPARQLModelRelation relation = new SPARQLModelRelation();
                relation.setProperty(Oeso.hasFactorLevel);
                relation.setValue(level.getUri().toString());
                relationsDTO.add(RDFObjectRelationDTO.getDTOFromModel(relation));
            }
        }
    }

    @Override
    public ScientificObjectModel newModelInstance() {
        return new ScientificObjectModel();
    }

    public static ScientificObjectDetailDTO getDTOFromModel(ScientificObjectModel model) {
        ScientificObjectDetailDTO dto = new ScientificObjectDetailDTO();
        dto.fromModel(model);

        return dto;
    }

    public static ScientificObjectDetailDTO getDTOFromModel(ScientificObjectModel model, LocationObservationModel lastLocation) {
        ScientificObjectDetailDTO dto = getDTOFromModel(model);

        if (Objects.nonNull(lastLocation)) {
            dto.setLocation(LocationObservationDTO.getDTOFromModel(lastLocation));
            dto.setGeometry(dto.getLocation().getGeojson());
            if (dto.getLocation().getTo() != null) {
                dto.getRelations().add(new RDFObjectRelationDTO(URI.create(Oeso.isHosted.getURI()), dto.getLocation().getTo().toString(), false));
            }
        }

        return dto;
    }
}
