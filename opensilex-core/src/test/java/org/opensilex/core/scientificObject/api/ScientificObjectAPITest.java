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
import org.apache.jena.riot.Lang;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.data.DataAPITest;
import org.opensilex.core.data.DataFileAPITest;
import org.opensilex.core.data.api.DataCreationDTO;
import org.opensilex.core.data.api.DataFileCreationDTO;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.experiment.api.ExperimentGetDTO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.germplasm.api.GermplasmAPITest;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.provenance.api.ProvenanceAPITest;
import org.opensilex.core.provenance.api.ProvenanceCreationDTO;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.api.VariableApiTest;
import org.opensilex.core.variable.api.VariableCreationDTO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
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

    private static final String GERMPLASM_RESTRICTION_ONTOLOGY_GRAPH = "http://www.opensilex.org/vocabulary/test-germplasm-restriction#";
    private static final Path GERMPLASM_RESTRICTION_ONTOLOGY_PATH = Paths.get("ontologies", "germplasmRestriction.owl");

    private int soCount = 1;
    private URI experiment;
    private URI speciesUri;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void beforeTest() throws Exception {
        final Response postResultXP = getJsonPostResponseAsAdmin(target(ExperimentAPITest.createPath), ExperimentAPITest.getCreationDTO());
        assertEquals(Status.CREATED.getStatusCode(), postResultXP.getStatus());
        final Response postResultGermplasm = getJsonPostResponseAsAdmin(target(GermplasmAPITest.createPath), GermplasmAPITest.getCreationSpeciesDTO());
        assertEquals(Status.CREATED.getStatusCode(), postResultGermplasm.getStatus());

        // ensure that the result is a well-formed URI, else throw exception
        experiment = extractUriFromResponse(postResultXP);
        speciesUri = extractUriFromResponse(postResultGermplasm);
        final Response getResultXP = getJsonGetByUriResponseAsAdmin(target(ExperimentAPITest.uriPath), experiment.toString());
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
        return Arrays.asList(ScientificObjectModel.class, VariableModel.class);
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return Arrays.asList(
                GeospatialDAO.GEOSPATIAL_COLLECTION_NAME,
                DataDAO.DATA_COLLECTION_NAME,
                DataDAO.FILE_COLLECTION_NAME,
                ProvenanceDAO.PROVENANCE_COLLECTION_NAME
        );
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
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(withGeometry));
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

    /**
     * Tests the automatic update of the species in an experiment based on their scientific objects.
     * This test needs the "hasGermplasm" restriction on scientific objects to work, thus the ontology
     * "germplasmRestriction" is added.
     *
     */
    @Test
    public void testCreateAndDeleteWithGermplasmWithRestriction() throws Exception {
        URI germplasmRestrictionOntologyGraphUri = new URI(GERMPLASM_RESTRICTION_ONTOLOGY_GRAPH);
        getSparqlService().loadOntology(germplasmRestrictionOntologyGraphUri,
                OpenSilex.getResourceAsStream(GERMPLASM_RESTRICTION_ONTOLOGY_PATH.toString()), Lang.RDFXML);

        ScientificObjectCreationDTO scientificObjectDTO = getCreationDTO(false, false, true);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), scientificObjectDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        // Check that the experiment is updated with the correct species
        ExperimentGetDTO experimentGetDTO = retrieveExperiment(experiment);

        assertEquals(1, experimentGetDTO.getSpecies().size());
        assertEquals(SPARQLDeserializers.getShortURI(experimentGetDTO.getSpecies().get(0)),
                SPARQLDeserializers.getShortURI(speciesUri));

        // Remove the relation from the scientific object
        scientificObjectDTO.setUri(extractUriFromResponse(postResult));
        scientificObjectDTO.setRelations(Collections.emptyList());
        final Response putResult = getJsonPutResponse(target(updatePath), scientificObjectDTO);
        assertEquals(Status.OK.getStatusCode(), putResult.getStatus());

        // Check that the species of the experiment is gone
        experimentGetDTO = retrieveExperiment(experiment);

        assertEquals(0, experimentGetDTO.getSpecies().size());

        getSparqlService().clearGraph(germplasmRestrictionOntologyGraphUri);
    }

    /**
     * Tests that creating a scientific object with a germplasm fails when the "hasGermplasm" restriction is not present
     * in the ontology
     *
     */
    @Test
    public void testCreateWithGermplasmWithoutRestrictionShouldFail() throws Exception {
        ScientificObjectCreationDTO scientificObjectDTO = getCreationDTO(false, false, true);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), scientificObjectDTO);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());
    }

    private ExperimentGetDTO retrieveExperiment(URI experimentUri) throws Exception {
        Response experimentResponse = getJsonGetByUriResponseAsAdmin(target(ExperimentAPITest.uriPath), experimentUri.toString());
        JsonNode experimentNode = experimentResponse.readEntity(JsonNode.class);
        SingleObjectResponse<ExperimentGetDTO> getExperimentResponse =
                mapper.convertValue(experimentNode, new TypeReference<SingleObjectResponse<ExperimentGetDTO>>() {});
        return getExperimentResponse.getResult();
    }

    @Test
    public void testUpdateOsWithSameNameOk() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        creationDTO.setUri(extractUriFromResponse(postResult));

        final Response putWithSameNameResult = getJsonPutResponse(target(updatePath),creationDTO);
        assertEquals(Status.OK.getStatusCode(), putWithSameNameResult.getStatus());
    }


    @Test
    public void testCreateWithDuplicateNameFail() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        final Response postDuplicateResult = getJsonPostResponseAsAdmin(target(createPath),creationDTO);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), postDuplicateResult.getStatus());
    }

    @Test
    public void testUpdateWithDuplicateNameFail() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        ScientificObjectCreationDTO creationDTO2 = getCreationDTO(false);
        creationDTO2.setName(creationDTO.getName()+"_diff");
        final Response postResult2 = getJsonPostResponseAsAdmin(target(createPath),creationDTO2);
        assertEquals(Status.CREATED.getStatusCode(), postResult2.getStatus());

        creationDTO2.setName(creationDTO.getName());
        final Response duplicatePutResult = getJsonPostResponseAsAdmin(target(updatePath),creationDTO);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), duplicatePutResult.getStatus());
    }

    @Test
    public void testCreateGlobalOSWithDuplicateNameOk() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false, true, false);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        final Response postDuplicateResult = getJsonPostResponseAsAdmin(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postDuplicateResult.getStatus());
    }

    @Test
    public void testUpdateGlobalOSWithDuplicateNameOk() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false, true, false);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        creationDTO.setUri(extractUriFromResponse(postResult));

        ScientificObjectCreationDTO creationDTO2 = getCreationDTO(false, true, false);
        creationDTO2.setName(creationDTO.getName()+"_diff");
        final Response postResult2 = getJsonPostResponseAsAdmin(target(createPath),creationDTO2);
        assertEquals(Status.CREATED.getStatusCode(), postResult2.getStatus());
        creationDTO2.setUri(extractUriFromResponse(postResult2));

        creationDTO2.setName(creationDTO.getName());
        final Response duplicatePutResult = getJsonPutResponse(target(updatePath),creationDTO2);
        assertEquals(Status.OK.getStatusCode(), duplicatePutResult.getStatus());
    }

    private Response getResponse(URI createdUri) throws Exception {
        WebTarget target = target(uriPath).resolveTemplate("uri", createdUri.toString());
        target = target.queryParam("experiment", experiment.toString());

        return appendAdminToken(target).get();
    }

    @Test
    public void testUpdateWithNoUriFail() throws Exception {

        // create the so
        ScientificObjectCreationDTO soDTO = getCreationDTO(false);
        getJsonPostResponseAsAdmin(target(createPath), soDTO);

        final Response updateResult = getJsonPutResponse(target(updatePath), soDTO);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), updateResult.getStatus());
    }

    public void testUpdate(boolean withGeometry) throws Exception {
        // create the so
        ScientificObjectCreationDTO soDTO = getCreationDTO(withGeometry);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), soDTO);

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
        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(withGeometry));
        URI uri = extractUriFromResponse(postResponse);

        // delete object and check if URI no longer exists
        WebTarget getDeleteByUriTarget = target(deletePath);
        getDeleteByUriTarget = getDeleteByUriTarget.resolveTemplate("uri", uri);
        getDeleteByUriTarget = getDeleteByUriTarget.queryParam("experiment", experiment.toString());

        try(final Response delResult = appendAdminToken(getDeleteByUriTarget).delete()){
            assertEquals(Status.OK.getStatusCode(), delResult.getStatus());

            final Response getResult = getResponse(uri);
            assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
        }

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
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(withGeometry));
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

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(true));
        JsonNode node = postResult.readEntity(JsonNode.class);
        ObjectUriResponse postResponse = mapper.convertValue(node, ObjectUriResponse.class);
        String uri = postResponse.getResult();

        // call the service with a non existing pseudo random URI
        final Response getResult = getResponse(new URI(uri + "7FG4FG89FG4GH4GH57"));
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    public void testGetScientificObjectsListByUris(boolean withGeometry) throws Exception {
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(withGeometry));
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
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(withGeometry));
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

    @Test
    public void testDeleteWithChildrenFail() throws Exception {
        // create parent
        ScientificObjectCreationDTO parentDto = getCreationDTO(false,false,false);
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), parentDto);
        parentDto.setUri(extractUriFromResponse(postResult));

        // create child with link with parent
        ScientificObjectCreationDTO childDto = getCreationDTO(false,false,false);
        RDFObjectRelationDTO partOfRelation = new RDFObjectRelationDTO();
        partOfRelation.setProperty(URI.create(Oeso.isPartOf.getURI()));
        partOfRelation.setValue(parentDto.getUri().toString());
        childDto.setRelations(Collections.singletonList(partOfRelation));

        postResult = getJsonPostResponseAsAdmin(target(createPath), childDto);
        childDto.setUri(extractUriFromResponse(postResult));

        // ensure delete is a fail -> try to delete OS in a experiment but the OS has child
        try(final Response deleteResponse = appendAdminToken(target(deletePath)
                .resolveTemplate("uri", parentDto.getUri())
                .queryParam("experiment",experiment)).delete()){

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteResponse.getStatus());

            JsonNode node = deleteResponse.readEntity(JsonNode.class);
            ErrorResponse errorResponse = mapper.convertValue(node, new TypeReference<ErrorResponse>() {});
            assertEquals("Scientific object can't be deleted : object has child", errorResponse.getResult().message);
        }

    }

    @Test
    public void testGlobalDeleteWhenOsIsInExperimentFail() throws Exception {

        ScientificObjectCreationDTO dto = getCreationDTO(false,false,false);
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), dto);
        dto.setUri(extractUriFromResponse(postResult));

        // ensure delete is a fail -> try to delete OS globally but the OS is involved into an experiment

        try(final Response deleteResponse  = appendAdminToken(target(deletePath)
                .resolveTemplate("uri", dto.getUri()))
                .delete()){

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteResponse.getStatus());
            JsonNode node = deleteResponse.readEntity(JsonNode.class);
            ErrorResponse errorResponse = mapper.convertValue(node, new TypeReference<ErrorResponse>() {});
            assertEquals("Scientific object can't be deleted : object is used into an experiment", errorResponse.getResult().message);
        }
    }

    private void testDeleteFail(boolean globalOs, URI objectURI, String expectedErrorMsg) throws Exception {

        // ensure delete is a fail -> try to delete OS with associated data
        WebTarget deleteTarget  = target(deletePath)
                .resolveTemplate("uri", objectURI);

        // if the OS has an experiment, then delete from xp fail because of associated data
        if(! globalOs){
            deleteTarget = deleteTarget.queryParam("experiment",experiment);
        }

        try(Response deleteResponse = appendAdminToken(deleteTarget).delete()){
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteResponse.getStatus());

            JsonNode node = deleteResponse.readEntity(JsonNode.class);
            ErrorResponse errorResponse = mapper.convertValue(node, new TypeReference<ErrorResponse>() {});
            assertEquals(expectedErrorMsg, errorResponse.getResult().message);
        }

    }

    private void testOsDeleteFailWhenAssociatedDataFile(boolean globalOs) throws Exception {

        // create provenance
        ProvenanceCreationDTO provenance = new ProvenanceCreationDTO();
        provenance.setName("testOsDeleteFailWhenAssociatedDataFile");
        Response postProvenanceResponse = getJsonPostResponseAsAdmin(target(new ProvenanceAPITest().createPath), provenance);
        provenance.setUri(extractUriFromResponse(postProvenanceResponse));

        // create OS
        ScientificObjectCreationDTO scientificObject = getCreationDTO(false, globalOs, false);
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), scientificObject);
        scientificObject.setUri(extractUriFromResponse(postResult));

        // create data provenance
        DataProvenanceModel dataProvenance = new DataProvenanceModel();
        dataProvenance.setUri(provenance.getUri());
        if(! globalOs){
            dataProvenance.setExperiments(Collections.singletonList(experiment));
        }else{
            dataProvenance.setExperiments(Collections.emptyList());
        }

        //  create data file
        DataFileCreationDTO dataFile = new DataFileCreationDTO();
        dataFile.setTarget(scientificObject.getUri());
        dataFile.setDate(LocalDate.now().toString());
        dataFile.setRdfType(URI.create(Oeso.Datafile.getURI()));
        dataFile.setProvenance(dataProvenance);

        // create tmp text file
        File file = tmpFolder.newFile("testOsDeleteFailWhenAssociatedDataFile");
        try (OutputStream outputStream = Files.newOutputStream(file.toPath())) {
            outputStream.write("test_data_file".getBytes());
        }
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);

        try(FormDataMultiPart multiPart = new FormDataMultiPart()){
            multiPart.field("description", dataFile, MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);
            final Response postDataFileResult = getJsonPostResponseMultipart(target(DataFileAPITest.createPath), multiPart);
            dataFile.setUri(extractUriFromResponse(postDataFileResult));
        }

        testDeleteFail(globalOs,scientificObject.getUri(),"Scientific object can't be deleted : object has associated data files");
    }

    private void testOsDeleteFailWhenAssociatedData(boolean globalOs) throws Exception {

        // create provenance
        ProvenanceCreationDTO provenance = new ProvenanceCreationDTO();
        provenance.setName("testOsDeleteFailWhenAssociatedData");
        Response postProvenanceResponse = getJsonPostResponseAsAdmin(target(new ProvenanceAPITest().createPath), provenance);
        provenance.setUri(extractUriFromResponse(postProvenanceResponse));

        // create variable
        VariableApiTest variableApiTest = new VariableApiTest();
        VariableCreationDTO variable = variableApiTest.getCreationDto();
        Response postVariableResponse = getJsonPostResponseAsAdmin(target(variableApiTest.createPath), variableApiTest.getCreationDto());
        variable.setUri(extractUriFromResponse(postVariableResponse));

        // create OS
        ScientificObjectCreationDTO scientificObject = getCreationDTO(false, globalOs, false);
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), scientificObject);
        scientificObject.setUri(extractUriFromResponse(postResult));

        // create associated data
        DataCreationDTO data = new DataCreationDTO();
        data.setValue(50);
        data.setTarget(scientificObject.getUri());
        data.setDate(LocalDate.now().toString());
        data.setVariable(variable.getUri());

        // create data provenance
        DataProvenanceModel dataProvenance = new DataProvenanceModel();
        dataProvenance.setUri(provenance.getUri());
        if(! globalOs){
            dataProvenance.setExperiments(Collections.singletonList(experiment));
        }else{
            dataProvenance.setExperiments(Collections.emptyList());
        }
        data.setProvenance(dataProvenance);
        Response postDataResponse = getJsonPostResponseAsAdmin(target(DataAPITest.createListPath),Collections.singletonList(data));
        data.setUri(extractUriFromResponse(postDataResponse));

        testDeleteFail(globalOs,scientificObject.getUri(),"Scientific object can't be deleted : object has associated data");
    }
    @Test
    public void testOsDeleteFailWhenDataAssociated() throws Exception {
       testOsDeleteFailWhenAssociatedData(true);
    }

    @Test
    public void testOsDeleteFailWhenDataFilesAssociated() throws Exception {
        testOsDeleteFailWhenAssociatedDataFile(true);
    }

    @Test
    public void testOsDeleteFailWhenDataAssociatedWithinExperiment() throws Exception {
        testOsDeleteFailWhenAssociatedData(false);
    }

    @Test
    public void testOsDeleteFailWhenDataFilesAssociatedWithinExperiment() throws Exception {
        testOsDeleteFailWhenAssociatedDataFile(false);
    }

    private ScientificObjectCreationDTO getCreationDTO(URI experiment, String name, URI uri) throws Exception {
        ScientificObjectCreationDTO dto = new ScientificObjectCreationDTO();
        dto.setName(name);
        dto.setUri(uri);
        dto.setType(URI.create(Oeso.ScientificObject.getURI()));
        dto.setExperiment(experiment);
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), dto);
        dto.setUri(extractUriFromResponse(postResult));
        return dto;
    }

    @Test
    public void testUriGenerationInExperimentalContext() throws Exception {

        // create two experiments
        SPARQLService sparql = getSparqlService();
        ExperimentModel xp = new ExperimentModel();
        xp.setStartDate(LocalDate.now());
        xp.setObjective("testGraphLocationAndUriGeneration");
        xp.setName("testGraphLocationAndUriGeneration");
        sparql.create(xp);

        ExperimentModel xp2 = new ExperimentModel();
        xp2.setStartDate(LocalDate.now());
        xp2.setObjective("testGraphLocationAndUriGeneration2");
        xp2.setName("testGraphLocationAndUriGeneration2");
        sparql.create(xp2);

        ScientificObjectCreationDTO os1 = getCreationDTO(xp.getUri(),"os1",null);

        // two os with same name but in different experiment -> /1
        ScientificObjectCreationDTO os1_1 =  getCreationDTO(xp2.getUri(),"os1",null);
        Assert.assertFalse(SPARQLDeserializers.compareURIs(os1.getUri(),os1_1.getUri()));
        Assert.assertTrue(os1_1.getUri().toString().endsWith("os1/1"));

        // os with unique name -> OK
        ScientificObjectCreationDTO os1_2 = getCreationDTO(xp2.getUri(),"os2",null);
        Assert.assertTrue(os1_2.getUri().toString().endsWith("os2"));

        // object URI reuse in another experiment
        ScientificObjectCreationDTO os1_reuse = getCreationDTO(xp2.getUri(),"os1_reuse",os1.getUri());
        Assert.assertEquals(os1.getUri(),os1_reuse.getUri());

        // re-insert OS inside same experiment -> duplicate URI error
        ScientificObjectCreationDTO duplicateInXp = new ScientificObjectCreationDTO();
        duplicateInXp.setName("os1");
        duplicateInXp.setUri(os1.getUri());
        duplicateInXp.setType(URI.create(Oeso.ScientificObject.getURI()));
        duplicateInXp.setExperiment(xp2.getUri());
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), getJsonPostResponseAsAdmin(target(createPath), duplicateInXp).getStatus());

        // global context
        URI defaultGraphURI = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        ScientificObjectCreationDTO os_global_1 =  getCreationDTO(null,"os_global_1",null);

        // two os with same name into global graph -> /1
        ScientificObjectCreationDTO os_global_1_1 = getCreationDTO(null,"os_global_1",null) ;
        Assert.assertFalse(SPARQLDeserializers.compareURIs(os_global_1.getUri(),os_global_1_1.getUri()));
        Assert.assertTrue(os_global_1_1.getUri().toString().endsWith("os_global_1/1"));

        // os with unique name -> OK
        ScientificObjectCreationDTO os_global_1_2 = getCreationDTO(null,"os_global_2",null);
        Assert.assertTrue(os_global_1_2.getUri().toString().endsWith("os_global_2"));

        // re-insert OS inside global graph -> duplicate URI error
        // re-insert OS inside same experiment -> duplicate URI error
        ScientificObjectCreationDTO duplicateInGlobal = new ScientificObjectCreationDTO();
        duplicateInXp.setName("os_global_1");
        duplicateInXp.setUri(os_global_1.getUri());
        duplicateInXp.setType(URI.create(Oeso.ScientificObject.getURI()));
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), getJsonPostResponseAsAdmin(target(createPath), duplicateInGlobal).getStatus());
    }



}
