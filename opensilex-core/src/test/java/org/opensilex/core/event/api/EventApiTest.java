//******************************************************************************
//                          EventApiTest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.event.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLStatement;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

/**
 * @author Renaud COLIN
 */
public class EventApiTest extends AbstractSecurityIntegrationTest {

    public String getByUriPath = EventAPI.PATH + "/{uri}";
    public String getDetailsByUriPath = EventAPI.PATH + "/{uri}/details";
    public String searchPath = EventAPI.PATH;
    public String createPath = EventAPI.PATH;
    public String updatePath = EventAPI.PATH;
    public String deletePath = EventAPI.PATH + "/{uri}";

    private static final Node scientificObjectGraph = NodeFactory.createURI("test:MoveEventApiTestGraph");
    private InfrastructureFacilityModel facilityA;
    private ScientificObjectModel so1, so2, so3;
    private DeviceModel device1;

    private final static String START_DATE_TIME_QUERY_PARAM = "start";
    private final static String END_DATE_TIME_QUERY_PARAM = "end";
    private final static String DESCRIPTION_QUERY_PARAM = "description";
    private final static String TARGET_QUERY_PARAM = "target";
    private final static String EVENT_TYPE_QUERY_PARAM = "rdf_type";

    @Before
    public void before() throws Exception {

        // create infra and scientific objects

        facilityA = new InfrastructureFacilityModel();
        facilityA.setUri(new URI("test:greenHouseA"));
        facilityA.setName("greenHouseA");

        SPARQLService sparql = getSparqlService();
        Node facilityGraph = sparql.getDefaultGraph(InfrastructureFacilityModel.class);
        sparql.create(facilityGraph, Arrays.asList(facilityA));

        so1 = new ScientificObjectModel();
        so1.setName("so1");
        so1.setUri(new URI("dev:so1"));

        so2 = new ScientificObjectModel();
        so2.setName("so2");
        so2.setUri(new URI("dev:so2"));

        so3 = new ScientificObjectModel();
        so3.setName("so3");
        so3.setUri(new URI("dev:so3"));

        sparql.create(scientificObjectGraph, Arrays.asList(so1, so2, so3));

        device1 = new DeviceModel();
        device1.setName("device1");
        device1.setUri(new URI("dev:device1"));
    }

    @After
    public void after() throws URISyntaxException, SPARQLException {
        getSparqlService().clearGraph(new URI(scientificObjectGraph.getURI()));
        getSparqlService().clearGraph(getSparqlService().getDefaultGraphURI(InfrastructureFacilityModel.class));
    }

    private EventCreationDTO getCreationDto() throws URISyntaxException {
        EventCreationDTO dto = new EventCreationDTO();
        dto.setDescription("A test event");
        dto.setIsInstant(true);
        dto.setEnd(OffsetDateTime.now().toString());
        dto.setType(new URI("http://www.opensilex.org/vocabulary/oeev#Trouble"));
        dto.setTargets(Arrays.asList(so1.getUri(), so2.getUri()));
        return dto;
    }

    private void checkEquals(EventCreationDTO expected, EventGetDTO actual){

        assertTrue(SPARQLDeserializers.compareURIs(expected.getUri(),actual.getUri()));
        assertTrue(SPARQLDeserializers.compareURIs(expected.getType(),actual.getType()));

        assertEquals(expected.getIsInstant(),actual.getIsInstant());
        assertEquals(expected.getStart(),actual.getStart());
        assertEquals(expected.getEnd(),actual.getEnd());
        assertEquals(expected.getDescription(),actual.getDescription());

        Set<URI> expectedUris = expected.getTargets().stream().map(SPARQLDeserializers::formatURI).collect(Collectors.toSet());
        Set<URI> actualUris = expected.getTargets().stream().map(SPARQLDeserializers::formatURI).collect(Collectors.toSet());
        assertEquals(expectedUris,actualUris);

    }

    @Test
    public void testUpdate() throws Exception {

        EventCreationDTO creationDto = getCreationDto();
        final Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(creationDto));

        creationDto.setUri(extractUriListFromPaginatedListResponse(postResult).get(0));
        creationDto.setDescription("new event");
        creationDto.setTargets(Collections.singletonList(so3.getUri()));

        // update the event

        final Response updateResult = getJsonPutResponse(target(updatePath), creationDto);
        assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new event and compare to the expected event
        final Response getResult = getJsonGetByUriResponse(target(getByUriPath), creationDto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<EventGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<EventGetDTO>>() {
        });
        EventGetDTO dtoFromDb = getResponse.getResult();

        // check that the object has been updated
        assertEquals(creationDto.getDescription(), dtoFromDb.getDescription());
        assertEquals(creationDto.getStart(), dtoFromDb.getStart());

        Collections.sort(creationDto.getTargets());
        Collections.sort(dtoFromDb.getTargets());
        assertEquals(creationDto.getTargets(), dtoFromDb.getTargets());
    }

    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateListGetAndDelete(createPath, getByUriPath, deletePath, Collections.singletonList(getCreationDto()));
    }

    @Test
    public void testCreateWithRelations() throws Exception {

        // associate so1 with so2
        RDFObjectRelationDTO relationDTO = new RDFObjectRelationDTO();
        relationDTO.setProperty(new URI(Oeev.associatedWithScientificObject.getURI()));
        relationDTO.setValue(so2.getUri().toString());

        EventCreationDTO dto = getCreationDto();
        dto.setTargets(Collections.singletonList(so1.getUri()));
        dto.setType(new URI(Oeev.AssociationWithScientificObject.getURI()));
        dto.setRelations(Collections.singletonList(relationDTO));

        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void testUpdateWithRelations() throws Exception {

        // associate so1 with so2
        RDFObjectRelationDTO relationWithSo2 = new RDFObjectRelationDTO();
        relationWithSo2.setProperty(new URI(Oeev.associatedWithScientificObject.getURI()));
        relationWithSo2.setValue(so2.getUri().toString());

        EventCreationDTO dto = getCreationDto();
        dto.setTargets(Collections.singletonList(so1.getUri()));
        dto.setType(new URI(Oeev.AssociationWithScientificObject.getURI()));
        dto.setRelations(Collections.singletonList(relationWithSo2));

        // create the event and get URI
        final Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        dto.setUri(extractUriListFromPaginatedListResponse(postResult).get(0));

        // update the event relation
        RDFObjectRelationDTO relationWithSo3 = new RDFObjectRelationDTO();
        relationWithSo3.setProperty(new URI(Oeev.associatedWithScientificObject.getURI()));
        relationWithSo3.setValue(so3.getUri().toString());
        dto.setRelations(Collections.singletonList(relationWithSo3));

        final Response updateResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());

        // try to deserialize object
        // retrieve the new event and compare to the expected event
        final Response getResult = getJsonGetByUriResponse(target(getDetailsByUriPath), dto.getUri().toString());
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<EventDetailsDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<EventDetailsDTO>>() {});
        EventDetailsDTO dtoFromDb = getResponse.getResult();

        // ensure relations are equals
        this.assertRelationsEquals(dto.getRelations(),dtoFromDb.getRelations());
    }

    @Test
    public void testDeleteWithRelations() throws Exception {

        // associate so1 with so2
        RDFObjectRelationDTO relationWithSo2 = new RDFObjectRelationDTO();
        relationWithSo2.setProperty(new URI(Oeev.associatedWithScientificObject.getURI()));
        relationWithSo2.setValue(so2.getUri().toString());

        EventCreationDTO dto = getCreationDto();
        dto.setTargets(Collections.singletonList(so1.getUri()));
        dto.setType(new URI(Oeev.AssociationWithScientificObject.getURI()));
        dto.setRelations(Collections.singletonList(relationWithSo2));

        // create the event and get URI
        final Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        dto.setUri(extractUriListFromPaginatedListResponse(postResult).get(0));

        // delete event
        final Response deleteResponse = getDeleteByUriResponse(target(deletePath), dto.getUri().toString());
        assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());

        // ensure that all event relations have been deleted
        // more precisely : must ensure that all relations associated to OWL restrictions are deleted
        // (e.g. if some triple related to the event( as subject or object) is inserted into the repository by another application.
        //       Only the event relations with a property described into ontologies, must be deleted
        SPARQLService sparql = getSparqlService();

        Node eventGraph = sparql.getDefaultGraph(EventModel.class);
        List<SPARQLStatement> statements = sparql.describe(eventGraph,dto.getUri());
        assertTrue(CollectionUtils.isEmpty(statements));

    }

    protected void assertRelationsEquals(List<RDFObjectRelationDTO> expected, List<RDFObjectRelationDTO> actual){

        assertEquals(expected.size(),actual.size());

        for(int i=0;i<expected.size();i++){
            RDFObjectRelationDTO expectedRelation = expected.get(i);
            RDFObjectRelationDTO actualRelation = actual.get(i);

            assertTrue(SPARQLDeserializers.compareURIs(expectedRelation.getProperty(),actualRelation.getProperty()));
            assertEquals(expectedRelation.getValue(),actualRelation.getValue());
        }
    }


    @Test
    public void testCreateWithRelationsWithUnknownTarget() throws Exception {

        EventCreationDTO dto = getCreationDto();
        dto.setTargets(Arrays.asList(new URI("oeev:unknown_target")));

        Response postResult = getJsonPostResponse(target(createPath),Collections.singletonList(dto));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());
    }


    @Test
    public void testCreateWithRelationsWithUnknownProperty() throws Exception {

        RDFObjectRelationDTO relationDTO = new RDFObjectRelationDTO();
        relationDTO.setProperty(new URI("oeev:unknownProperty"));
        relationDTO.setValue("value");

        EventCreationDTO dto = getCreationDto();
        dto.setRelations(Collections.singletonList(relationDTO));

        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void testCreateFailWithNoRequiredFields() throws Exception {

        EventCreationDTO dto = new EventCreationDTO();
        // no fields provided
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());

        // only description provided
        dto.setDescription("A test event");
        postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());

        // only concerned items provided
        dto.setDescription(null);
        dto.setTargets(Collections.singletonList(new URI("test:so1")));
        postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());

        // null concerned item provided
        dto.setDescription("A test event");
        dto.setTargets(Collections.emptyList());
        postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());

    }

    @Test
    public void testGetByUri() throws Exception {

        EventCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(creationDTO));

        URI uri = extractUriListFromPaginatedListResponse(postResult).get(0);
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), uri.toString());

        // try to deserialize object and check if the fields value are the same
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<EventGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<EventGetDTO>>() {
        });
        EventGetDTO dtoFromDb = getResponse.getResult();
        assertNotNull(dtoFromDb);

        assertEquals(creationDTO.getDescription(), dtoFromDb.getDescription());
        assertEquals(creationDTO.getStart(), dtoFromDb.getStart());
        assertEquals(creationDTO.getIsInstant(), dtoFromDb.getIsInstant());
        assertFalse(StringUtils.isEmpty(dtoFromDb.getCreator().toString()));

        Collections.sort(creationDTO.getTargets());
        Collections.sort(dtoFromDb.getTargets());
        assertEquals(creationDTO.getTargets(), dtoFromDb.getTargets());
    }

    @Test
    public void testSearchAll() throws Exception {
        EventCreationDTO dto = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        URI uri = extractUriListFromPaginatedListResponse(postResult).get(0);

        List<EventGetDTO> results = getResults(searchPath, new HashMap<>(), new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(results.size(), 1);
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(), uri)));

        dto.setDescription("new description");
        postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        URI uri2 = extractUriListFromPaginatedListResponse(postResult).get(0);
        ;

        results = getResults(searchPath, new HashMap<>(), new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(results.size(), 2);
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(), uri)));
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(), uri2)));
    }


    @Test
    public void testSearchByDescription() throws Exception {

        EventCreationDTO postDto1 = getCreationDto();
        postDto1.setDescription("Pest attack");
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(postDto1));
        postDto1.setUri(extractUriListFromPaginatedListResponse(postResult).get(0));

        EventCreationDTO postDto2 = getCreationDto();
        postDto2.setDescription("Grasshopper attack");
        postResult = getJsonPostResponse(target(createPath), Collections.singletonList(postDto2));
        postDto2.setUri(extractUriListFromPaginatedListResponse(postResult).get(0));

        // search given a description witch match both events
        Map<String, Object> params = new HashMap<String, Object>() {{
            put(DESCRIPTION_QUERY_PARAM, "attack");
        }};
        List<EventGetDTO> results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(2, results.size());
        EventGetDTO getDto1 = results.stream().filter(event -> SPARQLDeserializers.compareURIs(event.getUri(), postDto1.getUri())).findAny().get();
        checkEquals(postDto1,getDto1);

        EventGetDTO getDto2 = results.stream().filter(event -> SPARQLDeserializers.compareURIs(event.getUri(), postDto2.getUri())).findAny().get();
        checkEquals(postDto2,getDto2);

        // search given a description witch match no events
        params.put(DESCRIPTION_QUERY_PARAM, "Unknown");
        results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(0, results.size());

        // search given a description witch match only the first event
        params.put(DESCRIPTION_QUERY_PARAM, "Pest");
        results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(1, results.size());
        getDto1 = results.stream().filter(event -> SPARQLDeserializers.compareURIs(event.getUri(), postDto1.getUri())).findAny().get();
        checkEquals(postDto1,getDto1);

        // search given a description witch match only the second event
        params.put(DESCRIPTION_QUERY_PARAM, "Grasshopper");
        results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(1, results.size());
        getDto2 = results.stream().filter(event -> SPARQLDeserializers.compareURIs(event.getUri(), postDto2.getUri())).findAny().get();
        checkEquals(postDto2,getDto2);
    }


    @Test
    public void testSearchByDate() throws Exception {

        OffsetDateTime offsetDateTime = OffsetDateTime.now();

        EventCreationDTO dto = getCreationDto();
        dto.setIsInstant(false);
        dto.setStart(offsetDateTime.toString());
        dto.setEnd(offsetDateTime.plusDays(1).toString());
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        URI uri = extractUriListFromPaginatedListResponse(postResult).get(0);

        EventCreationDTO dto2 = getCreationDto();
        dto2.setIsInstant(false);
        dto2.setStart(offsetDateTime.plusYears(1).toString());
        dto2.setEnd(offsetDateTime.plusYears(1).plusDays(1).toString());
        postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto2));
        URI uri2 = extractUriListFromPaginatedListResponse(postResult).get(0);

        // search given a description witch match both events
        Map<String, Object> params = new HashMap<String, Object>() {{
            put(START_DATE_TIME_QUERY_PARAM, offsetDateTime.minusYears(2));
            put(END_DATE_TIME_QUERY_PARAM, offsetDateTime.plusYears(2));
        }};

        List<EventGetDTO> results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(), uri)));
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(), uri2)));

        // search given a description witch match no events
        // test with a too high start date
        params = new HashMap<String, Object>() {{
            put(START_DATE_TIME_QUERY_PARAM, offsetDateTime.plusYears(2));
        }};
        results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(0, results.size());

        // test with a too soon end date
        params = new HashMap<String, Object>() {{
            put(END_DATE_TIME_QUERY_PARAM, offsetDateTime.minusYears(2));
        }};
        results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(0, results.size());

        // test with a too soon period
        params = new HashMap<String, Object>() {{
            put(START_DATE_TIME_QUERY_PARAM, offsetDateTime.minusYears(2));
            put(END_DATE_TIME_QUERY_PARAM, offsetDateTime.minusYears(2).plusDays(1));
        }};
        results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(0, results.size());


        // search given a period witch match only the first event
        params = new HashMap<String, Object>() {{
            put(START_DATE_TIME_QUERY_PARAM, offsetDateTime.minusMonths(6));
            put(END_DATE_TIME_QUERY_PARAM, offsetDateTime.plusMonths(6));
        }};
        results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(1, results.size());
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(), uri)));

        // search given a period witch match only the second event
        params = new HashMap<String, Object>() {{
            put(START_DATE_TIME_QUERY_PARAM, offsetDateTime.plusMonths(6));
            put(END_DATE_TIME_QUERY_PARAM, offsetDateTime.plusMonths(18));
        }};
        results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(1, results.size());
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(), uri2)));
    }


    @Test
    public void testSearchByTarget() throws Exception {

        EventCreationDTO postDto1 = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(postDto1));
        postDto1.setUri(extractUriListFromPaginatedListResponse(postResult).get(0));

        EventCreationDTO postDto2 = getCreationDto();
        postDto2.setTargets(Arrays.asList(so2.getUri(),so3.getUri()));
        postResult = getJsonPostResponse(target(createPath), Collections.singletonList(postDto2));
        postDto2.setUri(extractUriListFromPaginatedListResponse(postResult).get(0));

        // search given a concernedItem witch match both events
        Map<String, Object> params = new HashMap<String, Object>() {{
            put(TARGET_QUERY_PARAM, so2.getUri());
        }};
        List<EventGetDTO> results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(2, results.size());

        EventGetDTO getDto1 = results.stream().filter(event -> SPARQLDeserializers.compareURIs(event.getUri(), postDto1.getUri())).findAny().get();
        checkEquals(postDto1,getDto1);

        EventGetDTO getDto2 = results.stream().filter(event -> SPARQLDeserializers.compareURIs(event.getUri(), postDto2.getUri())).findAny().get();
        checkEquals(postDto2,getDto2);


        // search given a concernedItem witch match no events
        params.put(TARGET_QUERY_PARAM, "test:scientificObject4");
        results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(0, results.size());

        // search given a concernedItem witch match only the first event
        params.put(TARGET_QUERY_PARAM, so1.getUri());
        results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(1, results.size());
        getDto1 = results.stream().filter(event -> SPARQLDeserializers.compareURIs(event.getUri(), postDto1.getUri())).findAny().get();
        checkEquals(postDto1,getDto1);

        // search given a concernedItem witch match only the second event
        params.put(TARGET_QUERY_PARAM, so3.getUri());
        results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(1, results.size());
        getDto1 = results.stream().filter(event -> SPARQLDeserializers.compareURIs(event.getUri(), postDto2.getUri())).findAny().get();
        checkEquals(postDto2,getDto1);
    }


    @Test
    public void testSearchByType() throws Exception {

        EventCreationDTO postDto1 = getCreationDto();
        postDto1.setType(new URI("oeev:Move"));
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(postDto1));
        postDto1.setUri(extractUriListFromPaginatedListResponse(postResult).get(0));

        EventCreationDTO postDto2 = getCreationDto();
        postDto2.setType(new URI("oeev:PestAttack"));
        postResult = getJsonPostResponse(target(createPath), Collections.singletonList(postDto2));
        postDto2.setUri(extractUriListFromPaginatedListResponse(postResult).get(0));

        // search given a description witch match no events
        Map<String, Object> params = new HashMap<String, Object>() {{
            put(EVENT_TYPE_QUERY_PARAM, new URI("test:UnknownEvent"));
        }};
        List<EventGetDTO> results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(0, results.size());

        // search given a description witch match only the first event
        params.put(EVENT_TYPE_QUERY_PARAM, postDto1.getType());
        results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(1, results.size());

        EventGetDTO getDto1 = results.stream().filter(event -> SPARQLDeserializers.compareURIs(event.getUri(), postDto1.getUri())).findAny().get();
        checkEquals(postDto1,getDto1);

        // search given a description witch match only the second event
        params.put(EVENT_TYPE_QUERY_PARAM, postDto2.getType());
        results = getResults(searchPath, params, new TypeReference<PaginatedListResponse<EventGetDTO>>() {
        });
        assertEquals(1, results.size());
        EventGetDTO getDto2 = results.stream().filter(event -> SPARQLDeserializers.compareURIs(event.getUri(), postDto2.getUri())).findAny().get();
        checkEquals(postDto2,getDto2);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Arrays.asList(EventModel.class);
    }

}
