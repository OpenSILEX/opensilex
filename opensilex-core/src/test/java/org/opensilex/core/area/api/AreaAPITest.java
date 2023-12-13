/*
 *  ********************************************************************************
 *                       AreaAPITest.java
 *   OpenSILEX
 *   Copyright © INRAE 2020
 *   Creation date: October 08, 2020
 *   Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *   *******************************************************************************
 */
package org.opensilex.core.area.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.area.dal.AreaModel;
import org.opensilex.core.geospatial.api.GeometryDTO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;

/**
 * @author Jean Philippe VERT
 */
public class AreaAPITest extends AbstractMongoIntegrationTest {

    protected final String path = "/core/area";

    protected final String uriPath = path + "/{uri}";
    protected final String createPath = path;
    protected final String updatePath = path;
    protected final String deletePath = path + "/{uri}";
    protected final String exportGeospatialPath = path + "/export_geospatial";
    private int soCount = 1;

    protected AreaCreationDTO getCreationDTO(boolean geometryError) throws Exception {
        AreaCreationDTO dto = new AreaCreationDTO();
        List<Position> list = new LinkedList<>();
        if (geometryError) {
            list.add(new Position(200.97167246, 43.61328981));
            list.add(new Position(3.97171243, 43.61332417));
            list.add(new Position(3.9717427, 43.61330558));
            list.add(new Position(3.97170272, 43.61327122));
            list.add(new Position(3.97167246, 43.61328981));
            list.add(new Position(200.97167246, 43.61328981));
        } else {
            list.add(new Position(3.97167246, 43.61328981));
            list.add(new Position(3.97171243, 43.61332417));
            list.add(new Position(3.9717427, 43.61330558));
            list.add(new Position(3.97170272, 43.61327122));
            list.add(new Position(3.97167246, 43.61328981));
            list.add(new Position(3.97167246, 43.61328981));
        }
        Geometry geometry = new Polygon(list);

        dto.setName("Area " + soCount++);
        dto.setRdfType(new URI("vocabulary:WindyArea"));
        dto.setGeometry(geometryToGeoJson(geometry));
        dto.setIsStructuralArea(true);

        return dto;
    }

    @Test
    public void testCreate() throws Exception {
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), createdUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {
        // create the area
        AreaCreationDTO areaDTO = getCreationDTO(false);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), areaDTO);

        // update the area
        areaDTO.setUri(extractUriFromResponse(postResult));
        areaDTO.setName("new name");
        areaDTO.setRdfType(new URI("vocabulary:FloodableArea"));
        Geometry geometry = new Point(new Position(3.97167246, 43.61328981));
        areaDTO.setGeometry(geometryToGeoJson(geometry));

        final Response updateResult = getJsonPutResponse(target(updatePath), areaDTO);
        assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new area and compare it to the expected area
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), areaDTO.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<AreaCreationDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<AreaCreationDTO>>() {
        });
        AreaCreationDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(areaDTO.getName(), dtoFromApi.getName());
        assertEquals(areaDTO.getRdfType(), dtoFromApi.getRdfType());
        assertEquals(areaDTO.getGeometry(), dtoFromApi.getGeometry());
    }

    @Test
    public void testDelete() throws Exception {
        // create object and check if URI exists
        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false));
        String uri = extractUriFromResponse(postResponse).toString();

        // delete object and check if URI no longer exists
        Response delResult = getDeleteByUriResponse(target(deletePath), uri);
        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());

        Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testGetByURI() throws Exception {
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false));
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<AreaGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<AreaGetDTO>>() {
        });
        AreaGetDTO soGetDetailDTO = getResponse.getResult();
        assertNotNull(soGetDetailDTO);
    }

    @Test
    public void testGetByUriBadUri() throws Exception {
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false));
        JsonNode node = postResult.readEntity(JsonNode.class);
        ObjectUriResponse postResponse = mapper.convertValue(node, ObjectUriResponse.class);
        String uri = postResponse.getResult();

        // call the service with a non existing pseudo random URI
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri + "7FG4FG89FG4GH4GH57");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testSearchIntersectsArea() throws Exception {
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false));
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<AreaGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<AreaGetDTO>>() {
        });
        AreaGetDTO soGetDetailDTO = getResponse.getResult();
        assertNotNull(soGetDetailDTO);
    }

    @Test(expected = Exception.class)
    public void testSearchIntersectsAreaErrorGeometry() throws Exception {
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(true));
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<AreaGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<AreaGetDTO>>() {
        });
        AreaGetDTO soGetDetailDTO = getResponse.getResult();
        assertNotNull(soGetDetailDTO);
    }

    @Test
    public void testExportAreasAsShpandGeoJson() throws Exception {

        //Create one Device Model
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false));
        URI uri = extractUriFromResponse(postResult);

        AreaModel areaModel = new AreaModel();

        areaModel.setName("AreaExported");
        areaModel.setUri(uri);
        areaModel.setType(new URI("http://www.opensilex.org/vocabulary/oeso#Area"));
        areaModel.setTypeLabel(new SPARQLLabel("Area","en"));

        areaModel.setDescription("Non de Zeus !");

        //build geometry
        List<Position> list = new LinkedList<>();
        list.add(new Position(3.97167246, 43.61328981));
        list.add(new Position(3.97171243, 43.61332417));
        list.add(new Position(3.9717427, 43.61330558));
        list.add(new Position(3.97170272, 43.61327122));
        list.add(new Position(3.97167246, 43.61328981));
        list.add(new Position(3.97167246, 43.61328981));
        Geometry geometry = new Polygon(list);

        GeometryDTO objToExport=new GeometryDTO();
        objToExport.setGeometry(geometryToGeoJson(geometry));
        objToExport.setUri(areaModel.getUri());

        ArrayList<GeometryDTO> objectsList= new ArrayList<>();
        objectsList.add(objToExport);

        //build params
        ArrayList<URI> propsList= new ArrayList<>();
        propsList.add(new URI(SPARQLDeserializers.getShortURI(Oeso.hasGeometry.getURI())));
        propsList.add(new URI("rdfs:comment"));

        Map<String, Object> paramsShp = new HashMap<>() {{
            put("format", "shp");
            put("selected_props",propsList);
            put("pageSize",10000);
        }};

        Map<String, Object> paramsGJson = new HashMap<>() {{
            put("format", "geojson");
            put("selected_props",propsList);
            put("pageSize",10000);
        }};

        // assert service
        final Response resultShp =  getOctetPostResponseAsAdmin(appendQueryParams(target(exportGeospatialPath),paramsShp),objectsList);
        assertEquals(Response.Status.OK.getStatusCode(), resultShp.getStatus());
        // assert service
        final Response resultGJson =  getOctetPostResponseAsAdmin(appendQueryParams(target(exportGeospatialPath),paramsGJson),objectsList);
        assertEquals(Response.Status.OK.getStatusCode(), resultGJson.getStatus());
    }
}