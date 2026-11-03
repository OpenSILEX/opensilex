/*
 * *****************************************************************************
 *                         ScientificObjectGeospatialExporterTest.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 02/11/2026 14:02
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */
package org.opensilex.core.scientificObject.dal;

import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.geojson.Point;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectGeospatialExporter;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ScientificObjectGeospatialExporterTest {

    private static final URI PARTICIPATES_IN = URI.create(SPARQLDeserializers.getShortURI(Oeso.participatesIn.getURI()));

    private static final URI OS_URI = URI.create("test:os-1");
    private static final URI XP_URI = URI.create("test:xp-1");

    private static ScientificObjectModel buildObject() {
        ScientificObjectModel os = new ScientificObjectModel();
        os.setUri(OS_URI);
        os.setName("os 1");
        os.setType(URI.create("vocabulary:Plot"));
        os.setTypeLabel(new SPARQLLabel("Plot", "en"));
        return os;
    }

    private static ExperimentModel buildExperiment(String name) {
        ExperimentModel xp = new ExperimentModel();
        xp.setUri(XP_URI);
        xp.setName(name);
        return xp;
    }

    private static Map<URI, GeoJsonObject> buildGeometries() {
        // exported URIs are expanded URIs
        Feature feature = new Feature();
        feature.setGeometry(new Point(1.3, 2.4));
        return Collections.singletonMap(URI.create(SPARQLDeserializers.getExpandedURI(OS_URI)), feature);
    }

    /**
     * Unzip the exported shapefiles and return the .shp file path
     */
    private static Path unzipShp(byte[] zip, Path directory) throws IOException {
        Path shp = null;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                Path target = directory.resolve(new File(entry.getName()).getName());
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                if (target.toString().endsWith(".shp")) {
                    shp = target;
                }
            }
        }
        assertNotNull("No .shp file in the export", shp);
        return shp;
    }

    private SimpleFeature exportAndReadFeature(ExperimentModel experiment) throws IOException {
        ScientificObjectGeospatialExporter exporter = new ScientificObjectGeospatialExporter(experiment);
        List<ScientificObjectModel> objects = Collections.singletonList(buildObject());

        Map<String, byte[]> result = exporter.exportFormat(Collections.singletonList(PARTICIPATES_IN), objects, buildGeometries(), "shp");
        byte[] zip = result.values().iterator().next();

        Path directory = Files.createTempDirectory("os-shp-export");
        Path shp = unzipShp(zip, directory);

        ShapefileDataStore dataStore = new ShapefileDataStore(shp.toUri().toURL());
        try (SimpleFeatureIterator iterator = dataStore.getFeatureSource().getFeatures().features()) {
            SimpleFeature feature = iterator.next();
            assertEquals(false, iterator.hasNext());
            return feature;
        } finally {
            dataStore.dispose();
        }
    }

    @Test
    public void shpExportContainsExperimentInfos() throws IOException {
        String expe_name = "xp 1";
        SimpleFeature feature = exportAndReadFeature(buildExperiment(expe_name));

        assertEquals(XP_URI.toString(), feature.getAttribute("exp_URI"));
        assertEquals(expe_name, feature.getAttribute("exp_name"));
    }

    @Test
    public void shpExportWithoutExperimentLeavesInfosEmpty() throws IOException {
        SimpleFeature feature = exportAndReadFeature(null);

        assertNotNull(feature.getFeatureType().getDescriptor("exp_URI"));
        assertNotNull(feature.getFeatureType().getDescriptor("exp_name"));
    }
}
