package org.opensilex.core.scientificObject.dal;


import org.apache.commons.collections4.BidiMap;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.geospatial.dal.GeospatialExporter;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

public class ScientificObjectGeospatialExporter extends GeospatialExporter<ScientificObjectModel> {

    static URI HAS_FACTORLEVEL = URI.create(SPARQLDeserializers.getShortURI(Oeso.hasFactorLevel.getURI()));
    static URI HAS_CREATION_DATE = URI.create(SPARQLDeserializers.getShortURI(Oeso.hasCreationDate.getURI()));
    static URI HAS_DESTRUCTIONTION_DATE = URI.create(SPARQLDeserializers.getShortURI(Oeso.hasDestructionDate.getURI()));

    public ScientificObjectGeospatialExporter() {
        super();
    }

    @Override
    protected void buildShpCustomProps(URI prop, ScientificObjectModel el, BidiMap<Integer, String> indexAttributes, SimpleFeatureBuilder featureBuilder) {

        if (prop.equals(HAS_FACTORLEVEL) && el.getFactorLevels() != null) {
            int index = indexAttributes.getKey(propFormatting(prop));
            featureBuilder.set(index, el.getFactorLevels().parallelStream().map(FactorLevelModel::getUri).collect(Collectors.toList()).toString());
        } else if (prop.equals(HAS_CREATION_DATE) && el.getCreationDate() != null) {
            int index = indexAttributes.getKey(propFormatting(prop));
            featureBuilder.set(index, el.getCreationDate());
        } else if (prop.equals(HAS_DESTRUCTIONTION_DATE) && el.getDestructionDate() != null) {
            int index = indexAttributes.getKey(propFormatting(prop));
            featureBuilder.set(index, el.getDestructionDate());
        } else {
            if (!el.getRelations().isEmpty()) {
                //Get the value of the corresponding property
                SPARQLModelRelation relation = el.getRelation(Ontology.property(prop));
                int index = indexAttributes.getKey(propFormatting(prop));

                if (relation != null) {
                    featureBuilder.set(index, relation.getValue());
                } else {
                    featureBuilder.set(index, null);
                }
            }
        }
    }

    @Override
    protected void buildGeoJSONCustomProps(URI prop, ScientificObjectModel el, Map<String, Object> properties) {

        if (prop.equals(HAS_FACTORLEVEL) && el.getFactorLevels() != null) {
            properties.put(prop.toString(), el.getFactorLevels().parallelStream().map(FactorLevelModel::getUri).collect(Collectors.toList()).toString());
        } else if (prop.equals(HAS_CREATION_DATE) && el.getCreationDate() != null) {
            properties.put(prop.toString(), el.getCreationDate().toString());
        } else if (prop.equals(HAS_DESTRUCTIONTION_DATE) && el.getDestructionDate() != null) {
            properties.put(prop.toString(), el.getDestructionDate().toString());
        } else {
            if (!el.getRelations().isEmpty()) {
                //Get the value of the corresponding property
                SPARQLModelRelation relation = el.getRelation(Ontology.property(prop));
                if (relation != null) {
                    properties.put(prop.toString(), relation.getValue());
                }
            }
        }
    }

}
