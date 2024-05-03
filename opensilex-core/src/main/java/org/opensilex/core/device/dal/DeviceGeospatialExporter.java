package org.opensilex.core.device.dal;

import org.apache.commons.collections4.BidiMap;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opensilex.core.geospatial.dal.GeospatialExporter;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.net.URI;
import java.util.Map;

public class DeviceGeospatialExporter extends GeospatialExporter<DeviceModel> {

    static URI HAS_MODEL = URI.create(SPARQLDeserializers.getShortURI(Oeso.hasModel.getURI()));
    static URI REMOVAL = URI.create(SPARQLDeserializers.getShortURI(Oeso.removal.getURI()));
    static URI HAS_SERIAL_NUMBER = URI.create(SPARQLDeserializers.getShortURI(Oeso.hasSerialNumber.getURI()));
    static URI START_UP = URI.create(SPARQLDeserializers.getShortURI(Oeso.startUp.getURI()));
    static URI HAS_BRAND = URI.create(SPARQLDeserializers.getShortURI(Oeso.hasBrand.getURI()));
    static URI PERSON_IN_CHARGE = URI.create(SPARQLDeserializers.getShortURI(Oeso.personInCharge.getURI()));

    public DeviceGeospatialExporter() {
        super();
    }

    @Override
    protected void buildShpCustomProps(URI prop, DeviceModel el, BidiMap<Integer, String> indexAttributes, SimpleFeatureBuilder featureBuilder)  {

        if (prop.equals(HAS_MODEL) && el.getModel() != null) {
            int index = indexAttributes.getKey(propFormatting(prop));

            featureBuilder.set(index, el.getModel());
        }else if (prop.equals(REMOVAL) && el.getRemoval() != null) {
            int index = indexAttributes.getKey(propFormatting(prop));

            featureBuilder.set(index, el.getRemoval());
        }else if (prop.equals(HAS_SERIAL_NUMBER) && el.getSerialNumber() != null) {
            int index = indexAttributes.getKey(propFormatting(prop));

            featureBuilder.set(index, el.getSerialNumber());
        }else if (prop.equals(START_UP) && el.getStartUp() != null) {
            int index = indexAttributes.getKey(propFormatting(prop));

            featureBuilder.set(index, el.getStartUp());
        }else if (prop.equals(HAS_BRAND) && el.getBrand() != null) {
            int index = indexAttributes.getKey(propFormatting(prop));

            featureBuilder.set(index, el.getBrand());
        }else if (prop.equals(COMMENT) && el.getDescription() != null) {
            int index = indexAttributes.getKey(propFormatting(prop));

            featureBuilder.set(index, el.getDescription());
        } else if (prop.equals(PERSON_IN_CHARGE) && el.getPersonInCharge() != null) {
                int index = indexAttributes.getKey(propFormatting(prop));

                featureBuilder.set(index, el.getPersonInCharge());
        }

    }

    @Override
    protected void buildGeoJSONCustomProps(URI prop, DeviceModel el, Map<String, Object> properties) {

        if (prop.equals(HAS_MODEL) && el.getModel() != null) {
            properties.put(prop.toString(), el.getModel());
        } else if (prop.equals(REMOVAL) && el.getRemoval() != null) {
            properties.put(prop.toString(), el.getRemoval().toString());
        } else if (prop.equals(HAS_SERIAL_NUMBER) && el.getSerialNumber() != null) {
            properties.put(prop.toString(), el.getSerialNumber());
        } else if (prop.equals(START_UP) && el.getStartUp() != null) {
            properties.put(prop.toString(), el.getStartUp().toString());
        } else if (prop.equals(HAS_BRAND) && el.getBrand() != null) {
            properties.put(prop.toString(), el.getBrand());
        } else if (prop.equals(COMMENT) && el.getDescription() != null) {
            properties.put(prop.toString(), el.getDescription());
        } else if (prop.equals(PERSON_IN_CHARGE) && el.getPersonInCharge() != null) {
            properties.put(prop.toString(), el.getPersonInCharge());
        }
    }
}


