package org.opensilex.core.geospatial.dal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.jena.vocabulary.RDFS;
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
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class GeospatialExporter<T extends SPARQLNamedResourceModel>{

    private enum GeospatialFormat{
        SHP("shp"),
        GEOJSON("geojson");

        public final String label;

        GeospatialFormat(String label) {this.label = label;}

        public String getLabel() {return this.label;}
    }

    public static final Class<Polygon> POLY = Polygon.class;
    public static final Class<LineString> LINE = LineString.class;
    public static final Class<Point> POINT = Point.class;

    public static final URI COMMENT= URI.create(SPARQLDeserializers.getShortURI(RDFS.comment.getURI()));
    private static final Logger LOGGER = LoggerFactory.getLogger(GeospatialExporter.class);

    protected GeospatialExporter() {
    }

    /**
     * @return file(s) and its title
     * @param selectedProps short URI for object type properties
     * @param objDetailList list of objects with details of their properties
     * @param selectedObjectsMap long URI list of objects with geometry from map
     * @param format authorized format - shp/geojson
     * @throws IOException if shapefile creation fails
     */
    public Map<String, byte[]> exportFormat(List<URI> selectedProps, List<T> objDetailList, Map<URI, GeoJsonObject> selectedObjectsMap, String format) throws IOException {
        byte[] fileByte;
        String fileName;
        Map<String, byte[]> result = new HashMap<>();

        if(Objects.equals(format, GeospatialFormat.SHP.getLabel())){
            //Convert to shapefiles and create a zip file to export
            fileByte = shpExport(selectedProps, objDetailList, selectedObjectsMap);
            fileName = "exportShpZip_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".zip" ;
        } else if (Objects.equals(format, GeospatialFormat.GEOJSON.getLabel())){
            //Convert to geojson
            fileByte = gjsonExport(selectedProps, objDetailList, selectedObjectsMap);
            fileName = "exportGeoJSon_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".geojson" ;
        } else {
            throw new NotFoundException("Unknown format");
        }

        result.put(fileName,fileByte);
        return result;
    }

    //FORMAT SHAPEFILE

    /**
     * @return zipped shapefiles
     * @param selectedProps short URI for object type properties
     * @param objDetailList list of objects with details of their properties
     * @param selectedObjectsMap long URI list of objects with geometry from map
     * @throws IOException if shapefile creation fails
     */
    private byte[] shpExport(List<URI> selectedProps,List<T> objDetailList, Map<URI, GeoJsonObject> selectedObjectsMap) throws IOException{

        //Get title
        String title = objDetailList.get(0).getClass().getSimpleName();

        // Create feature type - Define features type (shapefile columns) according to the selected props- and collection matching to listed geometry
        List<Class<? extends Geometry>> typeList = Arrays.asList(POLY,LINE,POINT);
        Map<SimpleFeatureType, DefaultFeatureCollection> typeCollectionMap = new HashMap<>();

        typeList.forEach(type -> typeCollectionMap.put(createType(type,selectedProps,title),new DefaultFeatureCollection()));

        //Writing collections with the corresponding TYPE
        createCollection(typeCollectionMap,objDetailList,selectedObjectsMap, selectedProps);

        //Get an output file name
        File directory = new File(title + "_shp");
        Files.createDirectory(directory.toPath());

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
            Files.delete(currentFile.toPath());
        }
        Files.deleteIfExists(directory.toPath());

        return zippedFile;
    }

    private SimpleFeatureType createType(Class<? extends Geometry> geomType, List<URI> selectProps, String title) {

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
        String now = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String geomKey = "the_geom";

        //Basic props
        typeBuilder.setName(title + "_" + geomType.getSimpleName() + "_" + now);
        typeBuilder.add(geomKey,geomType);
        typeBuilder.add("Name", String.class);
        typeBuilder.add("URI", String.class);
        typeBuilder.add("type_name", String.class);
        typeBuilder.add("type_URI", String.class);

        //Custom props
        if(!selectProps.isEmpty()){
            selectProps.forEach( prop ->{
                // Column labels formatting: max 10 charact
                String propString = propFormatting(prop);

                typeBuilder.add(propString, String.class);
            });
        }

        return typeBuilder.buildFeatureType();
    }
    protected String propFormatting(URI prop){
        int min = prop.toString().contains(":has") ? prop.toString().lastIndexOf(":") +4 : prop.toString().lastIndexOf(":") +1 ;
        int max = prop.toString().substring(min).length() >= 10 ?  min +10 : prop.toString().length() ;

        return prop.toString().substring(min, max);
    }

    /**
     * @param selectProps short URI for object type properties
     * @param objDetailList list of objects with details of their properties
     * @param selectedObjectsMap long URI list of objects with geometry from map
     * @param typeCollectionMap template based on selectedProps and corresponding collection by geometry type
     */
    private void createCollection(Map<SimpleFeatureType, DefaultFeatureCollection> typeCollectionMap, List<T> objDetailList, Map<URI, GeoJsonObject> selectedObjectsMap, List<URI> selectProps){
        //create Map Type - geometry
        Map<String, SimpleFeatureType> geomTypeMap = new HashMap<>();
        typeCollectionMap.forEach((k,v) -> geomTypeMap.put(k.getGeometryDescriptor().getType().getBinding().getSimpleName(), k));

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
                    selectProps.forEach(prop -> buildShpCustomProps(prop, el, indexAttributes, featureBuilder));
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

    /**
     * @param prop short URI property
     * @param indexAttributes properties index
     * @param featureBuilder builder for features
     * @param el element uses to build feature
     */
    protected abstract void buildShpCustomProps(URI prop, T el, BidiMap<Integer, String> indexAttributes, SimpleFeatureBuilder featureBuilder);

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

            } catch (Exception e) {
                LOGGER.error("Error writing collection to shapefile", e);
                transaction.rollback();

            } finally {
                transaction.close();
            }
        } else {
            LOGGER.debug("{} does not support read/write access", typeName);
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
                try (FileInputStream inFile = new FileInputStream(file)) {
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
        }
        outZip.close();
        return outByte.toByteArray();
    }

    // FORMAT GEOJSON

    /**
     * @return geojson file
     * @param selectedProps short URI for object type properties
     * @param objDetailList list of objects with details of their properties
     * @param selectedObjectsMap long URI list of objects with geometry from map
     */
    private byte[] gjsonExport(List<URI> selectedProps, List<T> objDetailList, Map<URI, GeoJsonObject> selectedObjectsMap){
        ObjectMapper mapper = new ObjectMapper();
        byte[] featureCollectionByte = new byte[0];

        //Build features : ["type": "Feature", "properties": {}, "geometry" : { "type" : "", "coordinates" : []}]
        List<Map<String, Object>> features = buildFeatures(selectedProps, objDetailList, selectedObjectsMap);

        //Build featureCollection : {"type": "FeatureCollection", "features": []}
        Map<String, Object> featureCollection = new HashMap<>();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.put("features", features);

        try {
            featureCollectionByte = mapper.writeValueAsBytes(featureCollection);
        } catch (JsonProcessingException ex) {
            LOGGER.error("Error writing collection to file", ex);
        }
        return featureCollectionByte;
    }

    private List<Map<String, Object>> buildFeatures(List<URI> selectedProps, List<T> objDetailList, Map<URI, GeoJsonObject> selectedObjectsMap){
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> features = new ArrayList<>();

        objDetailList.forEach(el -> {
            // Basic props
            Map<String, Object> properties = new HashMap<>();
            properties.put("Name", el.getName());
            properties.put("URI", el.getUri());
            properties.put("type_name", el.getTypeLabel().getDefaultValue());
            properties.put("type_URI", el.getType());
            //Custom props
            if (!selectedProps.isEmpty()) {
                selectedProps.forEach(prop -> buildGeoJSONCustomProps(prop, el, properties));
            }

            // Geometry
            Map<String, Object> geometry = mapper.convertValue(selectedObjectsMap.get(el.getUri()),Map.class);

            //Build feature : { "type": "Feature","properties": {},"geometry": {}}}
            Map<String, Object> feature = new HashMap<>();
            feature.put("type", "Feature");
            feature.put("properties",properties);
            feature.put("geometry",geometry);

            features.add(feature);
        });
        return features;
    }

    /**
     * @param prop short URI property
     * @param el element uses to build feature
     * @param properties geoJSON properties
     */
    protected abstract void buildGeoJSONCustomProps(URI prop, T el, Map<String, Object> properties);
}
