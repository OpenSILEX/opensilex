/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModelProperty;
import org.geojson.GeoJsonObject;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.response.NamedResourceDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.event.dal.move.MoveModel;

import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.server.rest.serialization.CustomParamConverterProvider;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

/**
 *
 * @author vmigot
 */
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "parent", "parent_name", "factor_level", "relations", "geometry"})
public class ScientificObjectDetailDTO extends NamedResourceDTO<ScientificObjectModel> {

    @ApiModelProperty(value = "Scientific object parent URI")
    protected URI parent;

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

    protected List<RDFObjectRelationDTO> relations;

    protected GeoJsonObject geometry;

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

    public GeoJsonObject getGeometry() {
        return geometry;
    }

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

        for (FactorLevelModel level : model.getFactorLevels()) {
            SPARQLModelRelation relation = new SPARQLModelRelation();
            relation.setProperty(Oeso.hasFactorLevel);
            relation.setValue(level.getUri().toString());
            relationsDTO.add(RDFObjectRelationDTO.getDTOFromModel(relation));
        }

        setRelations(relationsDTO);

    }

    public void fromModel(ScientificObjectModel model, MoveModel lastMove) {
        this.fromModel(model);
        boolean hasFacility = false;
        URI facilityURI = null;
        if (lastMove != null && lastMove.getTo() != null) {
            facilityURI = lastMove.getTo().getUri();
        }
        for (RDFObjectRelationDTO relation : this.getRelations()) {
            if (SPARQLDeserializers.compareURIs(relation.getProperty(), Oeso.hasFacility.getURI())) {
                hasFacility = true;
                relation.setValue(facilityURI.toString());
                break;
            }
        }

        if (!hasFacility && facilityURI != null) {
            SPARQLModelRelation relation = new SPARQLModelRelation();
            relation.setProperty(Oeso.hasFacility);
            relation.setValue(facilityURI.toString());
            this.getRelations().add(RDFObjectRelationDTO.getDTOFromModel(relation));
        }
    }

    @Override
    public ScientificObjectModel newModelInstance() {
        return new ScientificObjectModel();
    }

    public static ScientificObjectDetailDTO getDTOFromModel(ScientificObjectModel model, MoveModel lastMove) {
        ScientificObjectDetailDTO dto = new ScientificObjectDetailDTO();
        dto.fromModel(model, lastMove);

        return dto;
    }

    public static ScientificObjectDetailDTO getDTOFromModel(ScientificObjectModel model, GeospatialModel geometryByURI, MoveModel lastMove) throws JsonProcessingException {
        ScientificObjectDetailDTO dto = getDTOFromModel(model, lastMove);
        if (geometryByURI != null) {
            dto.setGeometry(geometryToGeoJson(geometryByURI.getGeometry()));
        }

        return dto;
    }
}
