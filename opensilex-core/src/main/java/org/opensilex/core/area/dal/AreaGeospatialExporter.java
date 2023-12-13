package org.opensilex.core.area.dal;

import org.apache.commons.collections4.BidiMap;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opensilex.core.geospatial.dal.GeospatialExporter;

import java.net.URI;
import java.util.Map;

public class AreaGeospatialExporter extends GeospatialExporter<AreaModel> {

    public AreaGeospatialExporter() {
        super();
    }

    @Override
    protected void buildShpCustomProps(URI prop, AreaModel el, BidiMap<Integer, String> indexAttributes, SimpleFeatureBuilder featureBuilder ) {

        if (prop.equals(COMMENT) && el.getDescription() != null) {
            int index = indexAttributes.getKey(propFormatting(prop));
            featureBuilder.set(index, el.getDescription());
        }
    }

    @Override
    protected void buildGeoJSONCustomProps(URI prop, AreaModel el, Map<String, Object> properties){

        if (prop.equals(COMMENT) && el.getDescription() != null) {
            properties.put(prop.toString(), el.getDescription());
        }
    }
}

