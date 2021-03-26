//******************************************************************************
//                          EventApiTest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.event.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

/**
 * @author Renaud COLIN
 */
public class EventApiTest extends AbstractSecurityIntegrationTest {


    public String getByUriPath = EventAPI.PATH + "/{uri}";
    public String searchPath = EventAPI.PATH;
    public String createPath = EventAPI.PATH;
    public String updatePath = EventAPI.PATH;
    public String deletePath = EventAPI.PATH + "/{uri}";

    private EventCreationDTO getCreationDto() throws URISyntaxException {
        EventCreationDTO dto = new EventCreationDTO();
        dto.setDescription("A test event");
        dto.setIsInstant(true);
        dto.setEnd(OffsetDateTime.now().toString());
        dto.setType(new URI("http://www.opensilex.org/vocabulary/oeev#Trouble"));
        dto.setConcernedItems(Arrays.asList(new URI("test:scientificObject1"),new URI("test:scientificObject2")));
        return dto;
    }

    @Test
    public void testUpdate() throws Exception {

        EventCreationDTO creationDto = getCreationDto();
        final Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(creationDto));

        creationDto.setUri( extractUriListFromPaginatedListResponse(postResult).get(0));
        creationDto.setDescription("new event");
        creationDto.setConcernedItems(Collections.singletonList(new URI("test:scientificObject3")));

        // create a new entity to associate with the variable

        final Response updateResult = getJsonPutResponse(target(updatePath), creationDto);
        TestCase.assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new xp and compare to the expected xp
        final Response getResult = getJsonGetByUriResponse(target(getByUriPath), creationDto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<EventGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<EventGetDTO>>() {
        });
        EventGetDTO dtoFromDb = getResponse.getResult();

        // check that the object has been updated
        assertEquals(creationDto.getDescription(),dtoFromDb.getDescription());
        assertEquals(creationDto.getStart(),dtoFromDb.getStart());

        Collections.sort(creationDto.getConcernedItems());
        Collections.sort(dtoFromDb.getConcernedItems());
        assertEquals(creationDto.getConcernedItems(),dtoFromDb.getConcernedItems());

    }

    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateListGetAndDelete(createPath,getByUriPath, deletePath, Collections.singletonList(getCreationDto()));
    }

    @Test
    public void testCreateFailWithNoRequiredFields() throws Exception {

        EventCreationDTO dto = new EventCreationDTO();
        // no fields provided
        Response postResult = getJsonPostResponse(target(createPath),Collections.singletonList(dto));
        TestCase.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());

        // only description provided
        dto.setDescription("A test event");
        postResult = getJsonPostResponse(target(createPath),Collections.singletonList(dto));
        TestCase.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());

        // only concerned items provided
        dto.setDescription(null);
        dto.setConcernedItems(Collections.singletonList(new URI("test:scientificObject1")));
        postResult = getJsonPostResponse(target(createPath),Collections.singletonList(dto));
        TestCase.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());

        // null concerned item provided
        dto.setDescription("A test event");
        dto.setConcernedItems(Collections.emptyList());
        postResult = getJsonPostResponse(target(createPath),Collections.singletonList(dto));
        TestCase.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());

    }



    @Test
    public void testGetByUri() throws Exception {

        EventCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(creationDTO));

        URI uri =  extractUriListFromPaginatedListResponse(postResult).get(0);
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), uri.toString());

        // try to deserialize object and check if the fields value are the same
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<EventGetDTO> getResponse =  mapper.convertValue(node, new TypeReference<SingleObjectResponse<EventGetDTO>>() {
        });
        EventGetDTO dtoFromDb = getResponse.getResult();
        assertNotNull(dtoFromDb);

        assertEquals(creationDTO.getDescription(),dtoFromDb.getDescription());
        assertEquals(creationDTO.getStart(),dtoFromDb.getStart());
        assertEquals(creationDTO.getIsInstant(),dtoFromDb.getIsInstant());
        assertFalse(StringUtils.isEmpty(dtoFromDb.getCreator().toString()));

        Collections.sort(creationDTO.getConcernedItems());
        Collections.sort(dtoFromDb.getConcernedItems());
        assertEquals(creationDTO.getConcernedItems(),dtoFromDb.getConcernedItems());
    }

    @Test
    public void testSearchAll() throws Exception {
        EventCreationDTO dto = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        URI uri = extractUriListFromPaginatedListResponse(postResult).get(0);

        List<EventGetDTO> results = getResults(searchPath,new HashMap<>(),new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(results.size(),1);
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri)));

        dto.setDescription("new description");
        postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        URI uri2 = extractUriListFromPaginatedListResponse(postResult).get(0);;

        results = getResults(searchPath,new HashMap<>(),new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(results.size(),2);
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri)));
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri2)));
    }

    private final static String DESCRIPTION_QUERY_PARAM = "description";


    @Test
    public void testSearchByDescription() throws Exception {

        EventCreationDTO dto = getCreationDto();
        dto.setDescription("Pest attack");
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        URI uri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        dto.setDescription("Grasshopper attack");
        postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        URI uri2 =  extractUriListFromPaginatedListResponse(postResult).get(0);

        // search given a description witch match both events
        Map<String, Object> params = new HashMap<String, Object>() {{
            put(DESCRIPTION_QUERY_PARAM,"attack");
        }};
        List<EventGetDTO> results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(2,results.size());
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri)));
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri2)));

        // search given a description witch match no events
        params.put(DESCRIPTION_QUERY_PARAM,"Unknown");
        results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(0,results.size());

        // search given a description witch match only the first event
        params.put(DESCRIPTION_QUERY_PARAM,"Pest");
        results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(1,results.size());
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri)));

        // search given a description witch match only the second event
        params.put(DESCRIPTION_QUERY_PARAM,"Grasshopper");
        results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(1,results.size());
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri2)));
    }

    private final static String START_DATE_TIME_QUERY_PARAM = "start";
    private final static String END_DATE_TIME_QUERY_PARAM = "end";
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
            put(START_DATE_TIME_QUERY_PARAM,offsetDateTime.minusYears(2));
            put(END_DATE_TIME_QUERY_PARAM,offsetDateTime.plusYears(2));
        }};

        List<EventGetDTO> results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(2,results.size());
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri)));
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri2)));

        // search given a description witch match no events
        // test with a too high start date
        params = new HashMap<String, Object>() {{
            put(START_DATE_TIME_QUERY_PARAM,offsetDateTime.plusYears(2));
        }};
        results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(0,results.size());

        // test with a too soon end date
        params = new HashMap<String, Object>() {{
          put(END_DATE_TIME_QUERY_PARAM,offsetDateTime.minusYears(2));
        }};
        results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(0,results.size());

        // test with a too soon period
        params = new HashMap<String, Object>() {{
            put(START_DATE_TIME_QUERY_PARAM,offsetDateTime.minusYears(2));
            put(END_DATE_TIME_QUERY_PARAM,offsetDateTime.minusYears(2).plusDays(1));
        }};
        results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(0,results.size());


        // search given a period witch match only the first event
        params = new HashMap<String, Object>() {{
            put(START_DATE_TIME_QUERY_PARAM,offsetDateTime.minusMonths(6));
            put(END_DATE_TIME_QUERY_PARAM,offsetDateTime.plusMonths(6));
        }};
        results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(1,results.size());
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri)));

        // search given a period witch match only the second event
        params = new HashMap<String, Object>() {{
            put(START_DATE_TIME_QUERY_PARAM,offsetDateTime.plusMonths(6));
            put(END_DATE_TIME_QUERY_PARAM,offsetDateTime.plusMonths(18));
        }};
        results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(1,results.size());
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri2)));
    }

    private final static String CONCERNED_ITEM_QUERY_PARAM = "target";

    @Test
    public void testSearchByConcernedItem() throws Exception {
        EventCreationDTO dto = getCreationDto();
        dto.setConcernedItems(Arrays.asList(new URI("test:scientificObject1"),new URI("test:scientificObject2")));
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        URI uri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        dto.setConcernedItems(Arrays.asList(new URI("test:scientificObject2"),new URI("test:scientificObject3")));
        postResult = getJsonPostResponse(target(createPath),Collections.singletonList(dto));
        URI uri2 =  extractUriListFromPaginatedListResponse(postResult).get(0);

        // search given a concernedItem witch match both events
        Map<String, Object> params = new HashMap<String, Object>() {{
            put(CONCERNED_ITEM_QUERY_PARAM,new URI("test:scientificObject2"));
        }};
        List<EventGetDTO> results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(2,results.size());
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri)));
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri2)));

        // search given a concernedItem witch match no events
        params.put(CONCERNED_ITEM_QUERY_PARAM,new URI("test:scientificObject4"));
        results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(0,results.size());

        // search given a concernedItem witch match only the first event
        params.put(CONCERNED_ITEM_QUERY_PARAM,new URI("test:scientificObject1"));
        results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(1,results.size());
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri)));

        // search given a concernedItem witch match only the second event
        params.put(CONCERNED_ITEM_QUERY_PARAM,new URI("test:scientificObject3"));
        results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(1,results.size());
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),uri2)));
    }


    private final static String EVENT_TYPE_QUERY_PARAM = "rdf_type";

    @Test
    public void testSearchByType() throws Exception {

        EventCreationDTO dto = getCreationDto();
        dto.setType(new URI("oeev:Move"));
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto));
        dto.setUri(extractUriListFromPaginatedListResponse(postResult).get(0));

        EventCreationDTO dto2 = getCreationDto();
        dto2.setType(new URI("oeev:PestAttack"));
        postResult = getJsonPostResponse(target(createPath), Collections.singletonList(dto2));
        dto2.setUri(extractUriListFromPaginatedListResponse(postResult).get(0));

        // search given a description witch match no events
        Map<String, Object> params = new HashMap<String, Object>() {{
            put(EVENT_TYPE_QUERY_PARAM,new URI("test:UnknownEvent"));
        }};
        List<EventGetDTO> results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(0,results.size());

        // search given a description witch match only the first event
        params.put(EVENT_TYPE_QUERY_PARAM,dto.getType());
        results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(1,results.size());
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),dto.getUri())));

        // search given a description witch match only the second event
        params.put(EVENT_TYPE_QUERY_PARAM,dto2.getType());
        results = getResults(searchPath,params,new TypeReference<PaginatedListResponse<EventGetDTO>>() {});
        assertEquals(1,results.size());
        assertTrue(results.stream().anyMatch(event -> SPARQLDeserializers.compareURIs(event.getUri(),dto2.getUri())));
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Arrays.asList(EventModel.class, AnnotationModel.class);
    }

}
