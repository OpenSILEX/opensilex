package org.opensilex.core.geospatial.dal;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.geojson.GeoJsonObject;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.utils.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ShapeFileExporter{

    public static final Class<Polygon> POLY = Polygon.class;
    public static final Class<LineString> LINE = LineString.class;
    public static final Class<Point> POINT = Point.class;
    public static final String WKT = "WKT";
    public static final String GEOJSON = "GEOJSON";
    static URI HAS_GEOMETRY;

    static {
        try {
            HAS_GEOMETRY = new URI(SPARQLDeserializers.getShortURI(Oeso.hasGeometry.getURI()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    static URI HAS_FACTORLEVEL;

    static {
        try {
            HAS_FACTORLEVEL = new URI("vocabulary:hasFactorLevel");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ShapeFileExporter.class);

    public ShapeFileExporter(){}

    public byte[] shpExport(List<URI> selectedProps,List<ScientificObjectModel> objDetailList, Map<URI, GeoJsonObject> selectedObjectsMap) throws IOException{

        // Create feature type - Define features type (shapefile columns) according to the selected props- and collection matching to listed geometry
        List<Class<? extends Geometry>> typeList = Arrays.asList(POLY,LINE,POINT);
        Map<SimpleFeatureType, DefaultFeatureCollection> typeCollectionMap = new HashMap<>();

        typeList.forEach(type -> typeCollectionMap.put(createType(type, selectedProps),new DefaultFeatureCollection()));

        //Writing collections with the corresponding TYPE
        createCollection(typeCollectionMap,objDetailList,selectedObjectsMap, selectedProps);

        //Get an output file name
        File directory = new File("OS_shp");
        directory.mkdirs();

        //Create a new shapefile for each TYPE/collection
        typeCollectionMap.forEach((k,v) -> {
            if(!v.isEmpty()){
                try {
                    createShapefile(k,v,directory);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        byte[] zippedFile = zipFile(directory);

        //Delete directory
        String[] entries = directory.list();
        assert entries != null;
        for(String s: entries){
            File currentFile = new File(directory.getPath(),s);
            currentFile.delete();
        }
        Files.deleteIfExists(directory.toPath());

        return zippedFile;
    }

    private SimpleFeatureType createType(Class<? extends Geometry> geomType, List<URI> selectProps) {

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
        String now = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String geomKey = "the_geom";

        //Basic props
        typeBuilder.setName("OS_" + geomType.getSimpleName() + "_" + now);
        typeBuilder.add(geomKey,geomType);
        typeBuilder.add("Name", String.class);
        typeBuilder.add("URI", String.class);
        typeBuilder.add("type_name", String.class);
        typeBuilder.add("type_URI", String.class);

        //Custom props
        if(!selectProps.isEmpty()){
            selectProps.forEach( prop ->{
                if(prop.equals(HAS_GEOMETRY)){
                    typeBuilder.add(GEOJSON, String.class);
                    typeBuilder.add(WKT, String.class);
                }
                else{
                    // Column labels formatting: max 10 charact
                    String propString = propFormatting(prop);

                    typeBuilder.add(propString, String.class);
                }
            });
        }

        return typeBuilder.buildFeatureType();
    }
    private String propFormatting(URI prop){
        int min = prop.toString().contains(":has") ? prop.toString().lastIndexOf(":") +4 : prop.toString().lastIndexOf(":") +1 ;
        int max = prop.toString().substring(min).length() >= 10 ?  min +10 : prop.toString().length() ;

        return prop.toString().substring(min, max);
    }

    private void createCollection(Map<SimpleFeatureType, DefaultFeatureCollection> typeCollectionMap, List<ScientificObjectModel> objDetailList, Map<URI, GeoJsonObject> selectedObjectsMap, List<URI> selectProps){
        //create Map Type - geometry
        Map<String, SimpleFeatureType> geomTypeMap = new HashMap<>();
        typeCollectionMap.forEach((k,v) -> {
            geomTypeMap.put(k.getGeometryDescriptor().getType().getBinding().getSimpleName(), k);
        });

        //Create features
        objDetailList.forEach(el -> {
            //Get geometry from objects
            GeoJsonObject geoJson = selectedObjectsMap.get(URI.create(SPARQLDeserializers.getExpandedURI(el.getUri())));

            try {
                //Get the TYPE corresponding to the object geometry
                com.mongodb.client.model.geojson.Geometry geometry = GeospatialDAO.geoJsonToGeometry(geoJson);
                String geomStg = geometry.getType().getTypeName();

                SimpleFeatureType TYPE = geomTypeMap.get(geomStg);

                //Get an index map of the props
                BidiMap<Integer, String> indexAttributes = new DualHashBidiMap<>();
                List<AttributeDescriptor> attributes = TYPE.getAttributeDescriptors();

                attributes.forEach(att -> indexAttributes.put(TYPE.indexOf(att.getLocalName()),att.getLocalName()));

                // Build and set value for each feature - according to selected props
                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

                //Formatting coordinates
                String wkt = GeospatialDAO.geometryToWkt(geometry);

                //Basic props
                featureBuilder.add(wkt);
                featureBuilder.add(el.getName());
                featureBuilder.add(el.getUri());
                featureBuilder.add(el.getTypeLabel());
                featureBuilder.add(el.getType());
                //Custom props
                if(!selectProps.isEmpty()){
                    selectProps.forEach( prop -> {
                        if(prop.equals(HAS_GEOMETRY)){
                            int indexGeoJson = indexAttributes.getKey(GEOJSON);
                            int indexWKT = indexAttributes.getKey(WKT);

                            featureBuilder.set(indexGeoJson, geoJson.toString());
                            featureBuilder.set(indexWKT, geometry);
                        }
                        else if (prop.equals(HAS_FACTORLEVEL) && el.getFactorLevels() != null) {
                            int index = indexAttributes.getKey(propFormatting(prop));

                            featureBuilder.set(index, el.getFactorLevels().parallelStream().map(FactorLevelModel::getUri).collect(Collectors.toList()).toString());
                        } else{
                            if(!el.getRelations().isEmpty()){
                                //Get the value of the corresponding property
                                SPARQLModelRelation relation= el.getRelation(Ontology.property(prop));

                                int index=indexAttributes.getKey(propFormatting(prop));

                                if(relation != null){
                                    featureBuilder.set(index, relation.getValue());
                                }
                                else{
                                    featureBuilder.set(index, null);
                                }
                            }
                        }
                    });
                }

                SimpleFeature simpleFeature = featureBuilder.buildFeature(null);

                //Add a feature matching the right TYPE in the collection
                DefaultFeatureCollection collection = typeCollectionMap.get(geomTypeMap.get(geomStg));

                collection.add(simpleFeature);

            } catch (JsonProcessingException | ParseException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void createShapefile(SimpleFeatureType type, DefaultFeatureCollection collection, File directory) throws IOException {
        File shapeFile = new File(directory.getPath(), type.getTypeName() + ".shp");

        //params to tell DataStoreFactory which file to use and indicate that we need to store a spatial index when we create our DataStore
        Map<String, Serializable> params = new HashMap<>();
        params.put("url", shapeFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        //create the DataStoreFactory using the parameters and use that factory to create the DataStore = represent a source of geospatial data
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        dataStore.createSchema(type);
        dataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

        // Write the features to the shapefile
        String typeName = dataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);

        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            Transaction transaction = new DefaultTransaction("create");
            featureStore.setTransaction(transaction);

            try {
                featureStore.addFeatures(collection);
                transaction.commit();

            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();

            } finally {
                transaction.close();
            }
        } else {
            LOGGER.debug(typeName +" does not support read/write access");
        }
    }

    private byte[] zipFile(File sourceDirectory) throws IOException {

        // open the incoming/outgoing flows
        ByteArrayOutputStream outByte = new ByteArrayOutputStream();
        ZipOutputStream outZip = new ZipOutputStream(outByte);

        // Get the file list in the directory
        File[] sources = sourceDirectory.listFiles();
        assert sources != null;

        for (File source : sources) {
            if(source.isFile()){
                String file = source.getPath();
                FileInputStream inFile = new FileInputStream(file);
                // Add a new file to zip
                outZip.putNextEntry(new ZipEntry(file));
                // Write the content of the added file
                byte[] buf = new byte[file.length()];
                int len = 0;
                while ((len = inFile.read(buf)) > 0) {
                    outZip.write(buf, 0, len);
                }
            }
        }
        outZip.close();
        return outByte.toByteArray();
    }
}
