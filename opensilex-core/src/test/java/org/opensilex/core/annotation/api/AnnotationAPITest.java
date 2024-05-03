package org.opensilex.core.annotation.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OpenSilexTestEnvironment;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class AnnotationAPITest extends AbstractMongoIntegrationTest {

    // TODO : These tests should be rewritten with the new UserCall class

    protected final static String PATH = "/core/annotations";
    protected final static String URI_PATH = PATH + "/{uri}";
    protected final static String SEARCH_PATH = PATH;
    protected final static String CREATE_PATH = PATH;
    protected final static String UPDATE_PATH = PATH;
    protected final static String DELETE_PATH = PATH + "/{uri}";

    protected final static String COUNT_PATH = PATH + "/count";
    protected final static String MOTIVATIONS_PATH = PATH + "/motivations";

    private static OpenSilexTestEnvironment openSilexTestEnv;
    // targets
    private static ScientificObjectModel scientificObject;
    private static GermplasmModel germplasm;
    private static DeviceModel device;
    private static ProjectModel project;
    private static ExperimentModel experiment;
    private static FacilityModel facility;

    protected static final TypeReference<PaginatedListResponse<AnnotationGetDTO>> listTypeReference = new TypeReference<PaginatedListResponse<AnnotationGetDTO>>() {};
    protected static final TypeReference<PaginatedListResponse<MotivationGetDTO>> listTypeReferenceMotivation = new TypeReference<PaginatedListResponse<MotivationGetDTO>>() {};


    @BeforeClass
    public static void createTargets() throws Exception {
        openSilexTestEnv = OpenSilexTestEnvironment.getInstance();
        SPARQLService sparql = openSilexTestEnv.getSparql();

        scientificObject = new ScientificObjectModel();
        scientificObject.setUri(URI.create("test:annot-test-so"));
        sparql.create(ScientificObjectModel.class, Arrays.asList(scientificObject));

        germplasm = new GermplasmModel();
        germplasm.setUri(URI.create("test:annot-test-germplasm"));
        germplasm.setLabel(new SPARQLLabel());
        sparql.create(GermplasmModel.class, Arrays.asList(germplasm));

        device = new DeviceModel();
        device.setUri(URI.create("test:annot-test-device"));
        sparql.create(DeviceModel.class, Arrays.asList(device));

        project = new ProjectModel();
        project.setUri(URI.create("test:annot-test-project"));
        project.setStartDate(LocalDate.now().minusDays(3));
        sparql.create(ProjectModel.class, Arrays.asList(project));

        experiment = new ExperimentModel();
        experiment.setUri(URI.create("test:annot-test-exp"));
        experiment.setStartDate(LocalDate.now().minusDays(3));
        experiment.setObjective("test objective");
        sparql.create(ExperimentModel.class, Arrays.asList(experiment));

        facility = new FacilityModel();
        facility.setUri(URI.create("test:annot-test-facility"));
        sparql.create(FacilityModel.class, Arrays.asList(facility));
    }

    private AnnotationCreationDTO getCreationDTO(int i) {
        AnnotationCreationDTO annotation = new AnnotationCreationDTO();
        annotation.setUri(URI.create("test:annotation" + i));
        annotation.setDescription("test create annotation " + i);
        annotation.setMotivation(URI.create("http://www.w3.org/ns/oa#describing"));

        return annotation;
    }

    @Test
    public void testCreateAnnotation() throws Exception {

        AnnotationCreationDTO annotation1 = getCreationDTO(1);
        annotation1.setTargets(Arrays.asList(scientificObject.getUri(), germplasm.getUri()));
        AnnotationCreationDTO annotation2 = getCreationDTO(2);
        annotation2.setTargets(Arrays.asList(experiment.getUri(), facility.getUri()));

        Response postResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), annotation1);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        postResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), annotation2);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());

        Map<String,Object> searchParams = new HashMap<>();
        List<AnnotationGetDTO> results = getSearchResultsAsAdmin(SEARCH_PATH, searchParams, listTypeReference);

        assertEquals(2,results.size());
        assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),annotation1.getUri())));
        assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),annotation2.getUri())));
    }

    @Test
    public void testUpdateAnnotation() throws Exception {
        AnnotationCreationDTO annotation1 = getCreationDTO(1);
        annotation1.setTargets(Arrays.asList(device.getUri()));

        Response postResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), annotation1);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String uri = extractUriFromResponse(postResponse).toString();

        String updatedDescription = "updated";

        annotation1.setDescription(updatedDescription);
        Response putResponse = getJsonPutResponse(target(UPDATE_PATH), annotation1);
        assertEquals(Response.Status.OK.getStatusCode(), putResponse.getStatus());

        Response getResponse = getJsonGetByUriResponseAsAdmin(target(URI_PATH), uri);
        JsonNode node = getResponse.readEntity(JsonNode.class);
        SingleObjectResponse<AnnotationGetDTO> result = mapper.convertValue(node, new TypeReference<SingleObjectResponse<AnnotationGetDTO>>() {
        });
        AnnotationGetDTO dtoFromApi = result.getResult();

        assertNotNull(dtoFromApi);
        assertEquals(annotation1.getDescription(), dtoFromApi.getDescription(), updatedDescription);
    }

    @Test
    public void testDeleteAnnotation() throws Exception {
        AnnotationCreationDTO annotation1 = getCreationDTO(1);
        annotation1.setTargets(Arrays.asList(device.getUri()));

        Response postResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), annotation1);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String uri = extractUriFromResponse(postResponse).toString();

        Response deleteResponse = getDeleteByUriResponse(target(DELETE_PATH), uri);
        assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());
    }

    @Test
    public void testGetAnnotation() throws Exception {
        AnnotationCreationDTO annotation1 = getCreationDTO(1);
        annotation1.setTargets(Arrays.asList(device.getUri()));

        Response postResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), annotation1);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String uri = extractUriFromResponse(postResponse).toString();

        Response getResponse = getJsonGetByUriResponseAsAdmin(target(URI_PATH), uri);
        JsonNode node = getResponse.readEntity(JsonNode.class);
        SingleObjectResponse<AnnotationGetDTO> result = mapper.convertValue(node, new TypeReference<SingleObjectResponse<AnnotationGetDTO>>() {
        });
        AnnotationGetDTO dtoFromApi = result.getResult();

        assertNotNull(dtoFromApi);
        assertTrue(SPARQLDeserializers.compareURIs(annotation1.getUri(), dtoFromApi.getUri()));
        assertEquals(annotation1.getDescription(), dtoFromApi.getDescription());
        assertTrue(SPARQLDeserializers.compareURIs(annotation1.getMotivation(), dtoFromApi.getMotivation().getUri()));
    }

    @Test
    public void testSearchMotivations() throws Exception {

        Map<String,Object> searchParams = new HashMap<>();
        searchParams.put("name", "classifying");
        List<MotivationGetDTO> results = getSearchResultsAsAdmin(MOTIVATIONS_PATH, searchParams, listTypeReferenceMotivation);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(SPARQLDeserializers.compareURIs(URI.create("http://www.w3.org/ns/oa#classifying"), results.get(0).getUri()));
    }

    public void testSearchAnnotations() throws Exception {

        AnnotationCreationDTO annotation1 = getCreationDTO(1);
        annotation1.setTargets(Arrays.asList(device.getUri()));
        AnnotationCreationDTO annotation2 = getCreationDTO(2);
        annotation2.setTargets(Arrays.asList(device.getUri(), facility.getUri()));
        AnnotationCreationDTO annotation3 = getCreationDTO(3);
        annotation3.setTargets(Arrays.asList(device.getUri(), facility.getUri(), germplasm.getUri()));

        Response postResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), annotation1);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        postResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), annotation2);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        postResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), annotation3);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());

        Map<String,Object> searchParams = new HashMap<>();
        searchParams.put("target", facility.getUri());
        List<AnnotationGetDTO> results = getSearchResultsAsAdmin(SEARCH_PATH, searchParams, listTypeReference);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),annotation2.getUri())));
        assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),annotation3.getUri())));

        searchParams.put("target", germplasm.getUri());
        results = getSearchResultsAsAdmin(SEARCH_PATH, searchParams, listTypeReference);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),annotation3.getUri())));
    }

    @Test
    public void testCountAnnotations() throws Exception {

        AnnotationCreationDTO annotation1 = getCreationDTO(1);
        annotation1.setTargets(Arrays.asList(device.getUri()));
        AnnotationCreationDTO annotation2 = getCreationDTO(2);
        annotation2.setTargets(Arrays.asList(device.getUri(), facility.getUri()));
        AnnotationCreationDTO annotation3 = getCreationDTO(3);
        annotation3.setTargets(Arrays.asList(device.getUri(), facility.getUri(), germplasm.getUri()));

        Response postResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), annotation1);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        postResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), annotation2);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        postResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), annotation3);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());

        Response countResponse = getJsonGetResponseAsAdmin(target(COUNT_PATH));
        JsonNode node = countResponse.readEntity(JsonNode.class);
        int count = (node.get("result")).asInt(-1);
        assertEquals(3, count);

        //TODO count by target
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(AnnotationModel.class);
    }
}