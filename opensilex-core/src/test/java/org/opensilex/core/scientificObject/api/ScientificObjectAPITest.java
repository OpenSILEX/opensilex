//******************************************************************************
//                          ScientificObjectAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.experiment.api.ExperimentGetDTO;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.germplasm.api.GermplasmAPITest;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;

/**
 * @author Vincent MIGOT
 * @author Jean Philippe VERT
 */
public class ScientificObjectAPITest extends AbstractMongoIntegrationTest {

    protected static final String path = "/core/scientific_objects";

    public static final String uriPath = path + "/{uri}";
    public static final String createPath = path + "/";
    public static final String updatePath = path + "/";
    public static final String deletePath = path + "/{uri}";
    private int soCount = 1;
    private URI experiment;
    private URI speciesUri;

    @Before
    public void beforeTest() throws Exception {
        final Response postResultXP = getJsonPostResponse(target(ExperimentAPITest.createPath), ExperimentAPITest.getCreationDTO());
        assertEquals(Status.CREATED.getStatusCode(), postResultXP.getStatus());
        final Response postResultGermplasm = getJsonPostResponse(target(GermplasmAPITest.createPath), GermplasmAPITest.getCreationSpeciesDTO());
        assertEquals(Status.CREATED.getStatusCode(), postResultGermplasm.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        experiment = extractUriFromResponse(postResultXP);
        speciesUri = extractUriFromResponse(postResultGermplasm);
        final Response getResultXP = getJsonGetByUriResponse(target(ExperimentAPITest.uriPath), experiment.toString());
        assertEquals(Status.OK.getStatusCode(), getResultXP.getStatus());
    }

    @After
    public void afterTest() throws Exception {

        // delete xp
        final Response delResult = getDeleteByUriResponse(target(ExperimentAPITest.deletePath), experiment.toString());
        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());

        // delete xp graph if some os has been inserted into experiment
        SPARQLService sparql = getSparqlService();
        Node experimentNode = NodeFactory.createURI(experiment.toString());
        if (sparql.count(experimentNode, ScientificObjectModel.class) > 0) {
            sparql.clearGraph(experiment);
        }

        final Response delResultGermplasm = getDeleteByUriResponse(target(GermplasmAPITest.deletePath), speciesUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), delResultGermplasm.getStatus());

        experiment = null;
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(ScientificObjectModel.class);
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return Collections.singletonList(GeospatialDAO.GEOSPATIAL_COLLECTION_NAME);
    }

    protected ScientificObjectCreationDTO getCreationDTO(boolean withGeometry, boolean globalOs, boolean withGermplasm) throws Exception {
        ScientificObjectCreationDTO dto = new ScientificObjectCreationDTO();

        if (withGeometry) {
            List<Position> list = new LinkedList<>();
            list.add(new Position(3.97167246, 43.61328981));
            list.add(new Position(3.97171243, 43.61332417));
            list.add(new Position(3.9717427, 43.61330558));
            list.add(new Position(3.97170272, 43.61327122));
            list.add(new Position(3.97167246, 43.61328981));
            list.add(new Position(3.97167246, 43.61328981));
            Geometry geometry = new Polygon(list);
            dto.setGeometry(geometryToGeoJson(geometry));
        }

        dto.setName("SO " + soCount++);
        dto.setType(new URI("http://www.opensilex.org/vocabulary/oeso#ScientificObject"));
        if(! globalOs){
            dto.setExperiment(experiment);
        }

        if (withGermplasm) {
            RDFObjectRelationDTO germplasmRelation = new RDFObjectRelationDTO();
            germplasmRelation.setProperty(new URI(Oeso.hasGermplasm.getURI()));
            germplasmRelation.setValue(speciesUri.toString());
            List<RDFObjectRelationDTO> relationList = new ArrayList<>();
            relationList.add(germplasmRelation);
            dto.setRelations(relationList);
        }

        return dto;
    }

    public ScientificObjectCreationDTO getCreationDTO(boolean withGeometry) throws Exception {
        return getCreationDTO(withGeometry, false, false);
    }

    public void testCreate(boolean withGeometry) throws Exception {
        final Response postResult = getJsonPostResponse(target(createPath), getCreationDTO(withGeometry));
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        final Response getResult = getResponse(createdUri);
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testCreate() throws Exception {
        testCreate(true);
    }

    @Test
    public void testCreateWithoutGeometry() throws Exception {
        testCreate(false);
    }

    @Test
    public void testCreateWithGermplasm() throws Exception {
        ScientificObjectCreationDTO creationDTO = getCreationDTO(false, false, true);
        final Response postResult = getJsonPostResponse(target(createPath), creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        // Check that the experiment is updated with the correct species
        Response experimentResponse = getJsonGetByUriResponse(target(ExperimentAPITest.uriPath), experiment.toString());
        JsonNode experimentNode = experimentResponse.readEntity(JsonNode.class);
        SingleObjectResponse<ExperimentGetDTO> getExperimentResponse =
                mapper.convertValue(experimentNode, new TypeReference<SingleObjectResponse<ExperimentGetDTO>>() {});
        ExperimentGetDTO experimentGetDTO = getExperimentResponse.getResult();

        assertEquals(experimentGetDTO.getSpecies().size(), 1);
        assertEquals(SPARQLDeserializers.getShortURI(experimentGetDTO.getSpecies().get(0)),
                SPARQLDeserializers.getShortURI(speciesUri));
    }

    @Test
    public void testUpdateOsWithSameNameOk() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false);
        final Response postResult = getJsonPostResponse(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        creationDTO.setUri(extractUriFromResponse(postResult));

        final Response putWithSameNameResult = getJsonPutResponse(target(updatePath),creationDTO);
        assertEquals(Status.OK.getStatusCode(), putWithSameNameResult.getStatus());
    }


    @Test
    public void testCreateWithDuplicateNameFail() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false);
        final Response postResult = getJsonPostResponse(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        final Response postDuplicateResult = getJsonPostResponse(target(createPath),creationDTO);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), postDuplicateResult.getStatus());
    }

    @Test
    public void testUpdateWithDuplicateNameFail() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false);
        final Response postResult = getJsonPostResponse(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        ScientificObjectCreationDTO creationDTO2 = getCreationDTO(false);
        creationDTO2.setName(creationDTO.getName()+"_diff");
        final Response postResult2 = getJsonPostResponse(target(createPath),creationDTO2);
        assertEquals(Status.CREATED.getStatusCode(), postResult2.getStatus());

        creationDTO2.setName(creationDTO.getName());
        final Response duplicatePutResult = getJsonPostResponse(target(updatePath),creationDTO);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), duplicatePutResult.getStatus());
    }

    @Test
    public void testCreateGlobalOSWithDuplicateNameOk() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false, true, false);
        final Response postResult = getJsonPostResponse(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        final Response postDuplicateResult = getJsonPostResponse(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postDuplicateResult.getStatus());
    }

    @Test
    public void testUpdateGlobalOSWithDuplicateNameOk() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false, true, false);
        final Response postResult = getJsonPostResponse(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        creationDTO.setUri(extractUriFromResponse(postResult));

        ScientificObjectCreationDTO creationDTO2 = getCreationDTO(false, true, false);
        creationDTO2.setName(creationDTO.getName()+"_diff");
        final Response postResult2 = getJsonPostResponse(target(createPath),creationDTO2);
        assertEquals(Status.CREATED.getStatusCode(), postResult2.getStatus());
        creationDTO2.setUri(extractUriFromResponse(postResult2));

        creationDTO2.setName(creationDTO.getName());
        final Response duplicatePutResult = getJsonPutResponse(target(updatePath),creationDTO2);
        assertEquals(Status.OK.getStatusCode(), duplicatePutResult.getStatus());
    }

    private Response getResponse(URI createdUri) throws Exception {
        WebTarget target = target(uriPath).resolveTemplate("uri", createdUri.toString());
        target = target.queryParam("experiment", experiment.toString());

        return appendToken(target).get();
    }

    @Test
    public void testUpdateWithNoUriFail() throws Exception {

        // create the so
        ScientificObjectCreationDTO soDTO = getCreationDTO(false);
        final Response postResult = getJsonPostResponse(target(createPath), soDTO);

        final Response updateResult = getJsonPutResponse(target(updatePath), soDTO);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), updateResult.getStatus());
    }

    public void testUpdate(boolean withGeometry) throws Exception {
        // create the so
        ScientificObjectCreationDTO soDTO = getCreationDTO(withGeometry);
        final Response postResult = getJsonPostResponse(target(createPath), soDTO);

        // update the so
        soDTO.setUri(extractUriFromResponse(postResult));
        soDTO.setName("new alias");
        Geometry geometry = new Point(new Position(3.97167246, 43.61328981));
        soDTO.setGeometry(geometryToGeoJson(geometry));

        final Response updateResult = getJsonPutResponse(target(updatePath), soDTO);
        assertEquals(Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new scientific object and compare it to the expected scientific object
        final Response getResult = getResponse(soDTO.getUri());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ScientificObjectDetailDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ScientificObjectDetailDTO>>() {
        });
        ScientificObjectDetailDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(soDTO.getName(), dtoFromApi.getName());
        assertEquals(soDTO.getType(), new URI(SPARQLDeserializers.getExpandedURI(dtoFromApi.getType())));
        assertEquals(soDTO.getGeometry().toString(), dtoFromApi.getGeometry().toString());
    }

    @Test
    public void testUpdate() throws Exception {
        testUpdate(true);
    }

    @Test
    public void testUpdateWithoutGeometry() throws Exception {
        testUpdate(false);
    }

    public void testDelete(boolean withGeometry) throws Exception {
        // create object and check if URI exists
        Response postResponse = getJsonPostResponse(target(createPath), getCreationDTO(withGeometry));
        URI uri = extractUriFromResponse(postResponse);

        // delete object and check if URI no longer exists
        WebTarget getDeleteByUriTarget = target(deletePath);
        getDeleteByUriTarget = getDeleteByUriTarget.resolveTemplate("uri", uri);
        getDeleteByUriTarget = getDeleteByUriTarget.queryParam("experiment", experiment.toString());

        final Response delResult = appendToken(getDeleteByUriTarget).delete();
        assertEquals(Status.OK.getStatusCode(), delResult.getStatus());

        final Response getResult = getResponse(uri);
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testDelete() throws Exception {
        testDelete(true);
    }

    @Test
    public void testDeleteWithoutGeometry() throws Exception {
        testDelete(false);
    }

    public void testGetDetail(boolean withGeometry) throws Exception {
        final Response postResult = getJsonPostResponse(target(createPath), getCreationDTO(withGeometry));
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getResponse(uri);
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ScientificObjectDetailDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ScientificObjectDetailDTO>>() {
        });
        ScientificObjectDetailDTO soGetDetailDTO = getResponse.getResult();
        assertNotNull(soGetDetailDTO);
    }

    @Test
    public void testGetDetail() throws Exception {
        testGetDetail(true);
    }

    @Test
    public void testGetDetailWithoutGeometry() throws Exception {
        testGetDetail(false);
    }

    @Test
    public void testGetByUriFail() throws Exception {

        final Response postResult = getJsonPostResponse(target(createPath), getCreationDTO(true));
        JsonNode node = postResult.readEntity(JsonNode.class);
        ObjectUriResponse postResponse = mapper.convertValue(node, ObjectUriResponse.class);
        String uri = postResponse.getResult();

        // call the service with a non existing pseudo random URI
        final Response getResult = getResponse(new URI(uri + "7FG4FG89FG4GH4GH57"));
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    public void testGetScientificObjectsListByUris(boolean withGeometry) throws Exception {
        final Response postResult = getJsonPostResponse(target(createPath), getCreationDTO(withGeometry));
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getResponse(uri);
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ScientificObjectNodeDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ScientificObjectNodeDTO>>() {
        });
        ScientificObjectNodeDTO soGetDetailDTO = getResponse.getResult();
        assertNotNull(soGetDetailDTO);
    }

    @Test
    public void testGetScientificObjectsListByUris() throws Exception {
        testGetScientificObjectsListByUris(true);
    }

    @Test
    public void testGetScientificObjectsListByUrisWithoutGeometry() throws Exception {
        testGetScientificObjectsListByUris(false);
    }

    public void testSearchScientificObjectsWithGeometryListByUris(boolean withGeometry) throws Exception {
        final Response postResult = getJsonPostResponse(target(createPath), getCreationDTO(withGeometry));
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getResponse(uri);
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ScientificObjectNodeDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ScientificObjectNodeDTO>>() {
        });
        ScientificObjectNodeDTO soGetDetailDTO = getResponse.getResult();
        assertNotNull(soGetDetailDTO);
    }

    @Test
    public void testSearchScientificObjectsWithGeometryListByUris() throws Exception {
        testSearchScientificObjectsWithGeometryListByUris(true);
    }

    @Test
    public void testSearchScientificObjectsWithGeometryListByUrisWithoutGeometry() throws Exception {
        testSearchScientificObjectsWithGeometryListByUris(false);
    }
}
