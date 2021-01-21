/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.geojson.GeoJsonObject;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.scientificObject.dal.ExperimentalObjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.response.NamedResourceDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

/**
 *
 * @author vmigot
 */
public class ScientificObjectDetailDTO extends NamedResourceDTO<ExperimentalObjectModel> {

    private URI parent;

    private String parentLabel;

    private URI type;

    private String typeLabel;

    private List<NamedResourceDTO<FactorLevelModel>> factorLevels;

    private List<RDFObjectRelationDTO> relations;

    private GeoJsonObject geometry;

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
    public void toModel(ExperimentalObjectModel model) {
        super.toModel(model);
    }

    @Override
    public void fromModel(ExperimentalObjectModel model) {
        super.fromModel(model);
        setType(model.getType());
        setTypeLabel(model.getTypeLabel().getDefaultValue());

        setFactorLevels(new ArrayList<>());
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

        for (SPARQLModelRelation relation : model.getRelations()) {
            relationsDTO.add(RDFObjectRelationDTO.getDTOFromModel(relation));
        }
        setRelations(relationsDTO);

    }

    @Override
    public ScientificObjectModel newModelInstance() {
        return new ScientificObjectModel();
    }

    public static ScientificObjectDetailDTO getDTOFromModel(ExperimentalObjectModel model) {
        ScientificObjectDetailDTO dto = new ScientificObjectDetailDTO();
        dto.fromModel(model);

        return dto;
    }

    public static ScientificObjectDetailDTO getDTOFromModel(ExperimentalObjectModel model, GeospatialModel geometryByURI) throws JsonProcessingException {
        ScientificObjectDetailDTO dto = getDTOFromModel(model);
        if (geometryByURI != null) {
            dto.setGeometry(geometryToGeoJson(geometryByURI.getGeometry()));
        }

        return dto;
    }
}
