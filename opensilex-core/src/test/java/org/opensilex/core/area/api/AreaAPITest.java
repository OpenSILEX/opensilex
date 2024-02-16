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
import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import org.geojson.GeoJsonObject;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.area.dal.AreaModel;
import org.opensilex.core.geospatial.api.GeometryDTO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;

import javax.ws.rs.core.MediaType;
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
    protected final ServiceDescription getByUri = new ServiceDescription(
            AreaAPI.class.getMethod("getByURI", URI.class),
            path + "/{uri}"
    );
    protected final ServiceDescription create = new ServiceDescription(
            AreaAPI.class.getMethod("createArea", AreaCreationDTO.class),
            path
    );
    protected final ServiceDescription update = new ServiceDescription(
            AreaAPI.class.getMethod("updateArea", AreaUpdateDTO.class),
            path
    );
    protected final ServiceDescription delete = new ServiceDescription(
            AreaAPI.class.getMethod("deleteArea", URI.class),
            path + "/{uri}"
    );
    protected final ServiceDescription exportGeospatial = new ServiceDescription(
            AreaAPI.class.getMethod("exportGeospatial", List.class, List.class, String.class, int.class),
            path + "/export_geospatial"
    );
    private int soCount = 1;

    public AreaAPITest() throws NoSuchMethodException {
    }

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

    protected URI createDefaultArea() throws Exception {
        UserCall createArea = new UserCallBuilder(create).setBody(getCreationDTO(false)).buildAdmin();
        return createArea.executeCallAndReturnURI();
    }

    protected Response createDefaultAreaWithError() throws Exception {
        UserCall createArea = new UserCallBuilder(create).setBody(getCreationDTO(true)).buildAdmin();
        return createArea.executeCall();
    }

    @Test
    public void testCreate() throws Exception {
        // ensure that the result is a well-formed URI, else throw exception
        URI createdUri = createDefaultArea();
        new UserCallBuilder(getByUri)
                .setUriInPath(createdUri)
                .buildAdmin()
                .executeCallAndAssertStatus(Response.Status.OK);
    }

    @Test
    public void testUpdate() throws Exception {
        // create the area
        URI createdUri = createDefaultArea();

        // update the area
        AreaCreationDTO areaDTO = getCreationDTO(false);
        areaDTO.setUri(createdUri);
        areaDTO.setName("new name");
        areaDTO.setRdfType(new URI("vocabulary:FloodableArea"));
        Geometry geometry = new Point(new Position(3.97167246, 43.61328981));
        areaDTO.setGeometry(geometryToGeoJson(geometry));

        new UserCallBuilder(update).setBody(areaDTO).buildAdmin().executeCallAndAssertStatus(Response.Status.OK);

        // retrieve the new area and compare it to the expected area
        SingleObjectResponse<AreaCreationDTO> getResponse = new UserCallBuilder(getByUri)
                .setUriInPath(createdUri)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<SingleObjectResponse<AreaCreationDTO>>() {
                })
                .getDeserializedResponse();
        AreaCreationDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(areaDTO.getName(), dtoFromApi.getName());
        assertEquals(areaDTO.getRdfType(), dtoFromApi.getRdfType());
        assertEquals(areaDTO.getGeometry(), dtoFromApi.getGeometry());
    }

    @Test
    public void testDelete() throws Exception {
        // create object and check if URI exists
        URI uri = createDefaultArea();

        // delete object and check if URI no longer exists
        UserCall deleteCall = new UserCallBuilder(delete).setUriInPath(uri).buildAdmin();
        URI uriDelete = deleteCall.executeCallAndReturnURI();
        assertEquals(uri, uriDelete);

        UserCall getCall = new UserCallBuilder(getByUri).setUriInPath(uri).buildAdmin();
        getCall.executeCallAndAssertStatus(Response.Status.NOT_FOUND);
    }

    @Test
    public void testGetByURI() throws Exception {
        URI uri = createDefaultArea();

        UserCall getArea = new UserCallBuilder(getByUri)
                .setUriInPath(uri)
                .buildAdmin();
        Result<SingleObjectResponse<AreaGetDTO>> getResponse = getArea.executeCallAndDeserialize(new TypeReference<SingleObjectResponse<AreaGetDTO>>() {
        });
        assertNotNull(getResponse.getDeserializedResponse().getResult());
    }

    @Test
    public void testGetByUriBadUri() throws Exception {
        URI uri = createDefaultArea();

        // call the service with a non-existing pseudo random URI
        UserCall getArea = new UserCallBuilder(getByUri)
                .setUriInPath( new URI(uri.toString() + "7FG4FG89FG4GH4GH57"))
                .buildAdmin();
        getArea.executeCallAndAssertStatus(Response.Status.NOT_FOUND);
    }

    @Test
    public void testSearchIntersectsArea() throws Exception {
        URI uri = createDefaultArea();

        UserCall getArea = new UserCallBuilder(getByUri)
                .setUriInPath(uri)
                .buildAdmin();
        assertNotNull(getArea.executeCallAndDeserialize(new TypeReference<SingleObjectResponse<AreaGetDTO>>() {
        }));
    }

    @Test
    public void testSearchIntersectsAreaErrorGeometry() throws Exception {
        Response responseFail = createDefaultAreaWithError();
        assertEquals(responseFail.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testExportAreasAsShpandGeoJson() throws Exception {
        // This test is kind of weird. Ask Alexia what it does
        //Create one Area
        URI uri = createDefaultArea();
        // Especially here. Why add params to a model of an Area whose URI already exists?
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
        String geoJSON = geometry.toJson();
        GeoJsonObject geoJsonGeometry = ObjectMapperContextResolver.getObjectMapper().readValue(geoJSON, GeoJsonObject.class);

        GeometryDTO objToExport=new GeometryDTO();
        objToExport.setGeometry(geoJsonGeometry);
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

        UserCallBuilder exportCallBuilder = new UserCallBuilder(exportGeospatial)
                .setParams(paramsShp)
                .setBody(objectsList)
                .setResponseMediaTypes(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM_TYPE));

        UserCall exportCall = exportCallBuilder.buildAdmin();
        exportCall.executeCall();

        exportCallBuilder.setParams(paramsGJson);
        exportCall = exportCallBuilder.buildAdmin();
        exportCall.executeCall();
    }
}