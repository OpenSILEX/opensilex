/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.dao;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import opensilex.service.dao.manager.MongoDAO;
import opensilex.service.model.ScientificObject;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

/**
 *
 * @author vmigot
 */
public class ScientificObjectMongoDAO {

    private final static String SO_COLLECTION = "scientificObjects";
    private final static String URI_FIELD = "uri";
    private final static String GEOMETRY_FIELD = "geometry";
    private final static String RDF_TYPE_FIELD = "rdfType";
    private final static String GRAPH_FIELD = "graph";
    
    private MongoDatabase db;
    
    public ScientificObjectMongoDAO() {
        db = MongoDAO.getStaticDataBase();
    }
    
    public void checkAndInsertListAO(ArrayList<ScientificObject> scientificObjects) throws ParseException {
        ArrayList<Document> documents = new ArrayList<>();
        for (ScientificObject so : scientificObjects) {

            if(StringUtils.isEmpty(so.getGeometry())){
                continue;
            }
            Document document = new Document();
            document.append(URI_FIELD, so.getUri());
            document.append(RDF_TYPE_FIELD, so.getRdfType());
            document.append(GRAPH_FIELD, so.getExperiment());
            document.append(GEOMETRY_FIELD, convertWktToGeoJson(so.getGeometry()));
            
            documents.add(document);
        };

        if(! documents.isEmpty()){
            db.getCollection(SO_COLLECTION).insertMany(documents);
        }
    }

    /**
     * Return a map of geometry indexed by Scientific Object URI
     * @param scientificObjectsUris
     * @return 
     */
    public HashMap<String, String> getGeometries(ArrayList<String> scientificObjectsUris) {
        FindIterable<Document> soGeom = db.getCollection(SO_COLLECTION).find(new Document(URI_FIELD, new BasicDBObject("$in", scientificObjectsUris)));
        HashMap<String, String> geometries = new HashMap<>();
        MongoCursor<Document> soIterator = soGeom.iterator();
        while (soIterator.hasNext()) {
            Document doc = soIterator.next();
            geometries.put(doc.get(URI_FIELD).toString(), doc.get(GEOMETRY_FIELD).toString());
        }
        
        return geometries;
    }

    public boolean existInDB(ScientificObject scientificObjectToSearchInDB) {
        long count = db.getCollection(SO_COLLECTION).countDocuments(new Document(URI_FIELD, scientificObjectToSearchInDB.getUri()));
        return (count > 0);
    }

    public void updateOneGeometry(String uri, String wkt, String rdfType, String experiment) throws ParseException {
        Document document = new Document();
        document.append(URI_FIELD, uri);
        document.append(RDF_TYPE_FIELD, rdfType);
        document.append(GRAPH_FIELD, experiment);
        document.append(GEOMETRY_FIELD, convertWktToGeoJson(wkt));
        
        db.getCollection(SO_COLLECTION).insertOne(document);
    }
    
    private static String convertWktToGeoJson(String wkt) throws ParseException {
        WKTReader reader = new WKTReader();
        Geometry parsedGeometry = reader.read(wkt);
        GeoJsonWriter writer = new GeoJsonWriter();
        return writer.write(parsedGeometry);
    }
    
}
