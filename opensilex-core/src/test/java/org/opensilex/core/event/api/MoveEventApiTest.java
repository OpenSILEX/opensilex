package org.opensilex.core.event.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.geojson.Point;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.event.api.move.MoveEventCreationDTO;
import org.opensilex.core.event.api.move.MoveEventGetDTO;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.position.api.*;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class MoveEventApiTest extends AbstractMongoIntegrationTest {

    public String path = EventAPI.MOVE_PATH;

    public String getByUriPath = path + "/{uri}";
    public String createPath = path;
    public String updatePath = path ;
    public String deletePath = path+ "/{uri}";

    public String positionPath = PositionAPI.PATH;
    public String getPositionPath = positionPath + "/{uri}";
    public String getPositionHistoryPath = positionPath + "/history";

    private InfrastructureFacilityModel facilityA;
    private InfrastructureFacilityModel facilityB;
    private InfrastructureFacilityModel facilityC;

    private Node scientificObjectGraph;
    private ScientificObjectModel scientificObjectA;
    private ScientificObjectModel scientificObjectB;
    private ScientificObjectModel scientificObjectC;

    private List<InfrastructureFacilityModel> fromFacilities;
    private List<InfrastructureFacilityModel> toFacilities;
    private final static int nbMoveMax = 10;

    @Before
    public void createFacilities() throws Exception {


        facilityA = new InfrastructureFacilityModel();
        facilityA.setUri(new URI("test:greenHouseA"));
        facilityA.setName("greenHouseA");

        facilityB = new InfrastructureFacilityModel();
        facilityB.setUri(new URI("test:greenHouseB"));
        facilityB.setName("greenHouseB");

        facilityC = new InfrastructureFacilityModel();
        facilityC.setUri(new URI("test:greenHouseC"));
        facilityC.setName("greenHouseC");

        SPARQLService sparql = getSparqlService();
        Node facilityGraph = sparql.getDefaultGraph(InfrastructureFacilityModel.class);
        sparql.create(facilityGraph,Arrays.asList(facilityA, facilityB, facilityC));

        //
        fromFacilities = new ArrayList<>(nbMoveMax);
        toFacilities = new ArrayList<>(nbMoveMax);

        for(int i=0; i<nbMoveMax;i++){

            InfrastructureFacilityModel fromFacility = new InfrastructureFacilityModel();
            fromFacility.setUri(new URI("test:from_facility_"+i));
            fromFacility.setName("from_facility_"+i);
            fromFacilities.add(fromFacility);

            InfrastructureFacilityModel toFacility = new InfrastructureFacilityModel();
            toFacility.setUri(new URI("test:to_facility_"+i));
            toFacility.setName("to_facility_"+i);
            toFacilities.add(toFacility);
        }
        sparql.create(facilityGraph,fromFacilities);
        sparql.create(facilityGraph,toFacilities);
    }

    @Before
    public void createScientificObjects() throws Exception{

        scientificObjectA = new ScientificObjectModel();
        scientificObjectA.setUri(new URI("test:plantA"));
        scientificObjectA.setName("plantA");
        scientificObjectA.setType(new URI("oeso:ScientificObject"));

        scientificObjectB = new ScientificObjectModel();
        scientificObjectB.setUri(new URI("test:plantB"));
        scientificObjectB.setName("plantB");
        scientificObjectB.setType(new URI("oeso:ScientificObject"));

        scientificObjectC = new ScientificObjectModel();
        scientificObjectC.setUri(new URI("test:plantC"));
        scientificObjectC.setName("plantC");
        scientificObjectC.setType(new URI("oeso:ScientificObject"));

        SPARQLService sparql = getSparqlService();
        scientificObjectGraph = NodeFactory.createURI("test:MoveEventApiTestGraph");
        sparql.create(scientificObjectGraph,Arrays.asList(scientificObjectA, scientificObjectB, scientificObjectC));

    }


    private MoveEventCreationDTO getCreationDto() {

        MoveEventCreationDTO dto = new MoveEventCreationDTO();
        dto.setDescription("A test event");
        dto.setIsInstant(false);
        OffsetDateTime now = OffsetDateTime.now();
        dto.setStart(now.toString());
        dto.setEnd(now.plusMinutes(2).toString());

        dto.setConcernedItems(Arrays.asList(
                scientificObjectA.getUri(),
                scientificObjectB.getUri(),
                scientificObjectC.getUri()
        ));

        dto.setFrom(facilityA.getUri());
        dto.setTo(facilityB.getUri());

        // first position
        PositionCreationDTO k2Summit = new PositionCreationDTO();
        k2Summit.setDescription("Not far from sky");
        k2Summit.setX(35);
        k2Summit.setY(76);
        k2Summit.setZ(8611);
        k2Summit.setPoint(new Point(76.513333, 35.8825, 8611));

        ConcernedItemPositionCreationDTO so1Position = new ConcernedItemPositionCreationDTO();
        so1Position.setConcernedItem(dto.getConcernedItems().get(0));
        so1Position.setPosition(k2Summit);

        // 2nd position
        PositionCreationDTO k2Camp4 = new PositionCreationDTO();
        k2Camp4.setDescription("Just behind the Bottleneck");
        k2Camp4.setZ(7600);

        ConcernedItemPositionCreationDTO so2Position = new ConcernedItemPositionCreationDTO();
        so2Position.setConcernedItem(dto.getConcernedItems().get(1));
        so2Position.setPosition(k2Camp4);

        dto.setConcernedItemPositions(Arrays.asList(so1Position,so2Position));

        return dto;
    }

//    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateGetAndDelete(createPath, getByUriPath, deletePath, Collections.singletonList(getCreationDto()));
    }

    @Test
    public void testFailWithUnknownTo() throws Exception {

        MoveCreationDTO creationDTO = getCreationDto();
        creationDTO.setTo(new URI("test:unknownFacilityA"));

        final Response postResult = getJsonPostResponse(target(createPath),Collections.singletonList(creationDTO));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(),postResult.getStatus());
    }

    @Test
    public void testFailWithUnknownFrom() throws Exception {
        MoveCreationDTO creationDTO = getCreationDto();
        creationDTO.setFrom(new URI("test:unknownFacilityA"));

        final Response postResult = getJsonPostResponse(target(createPath),Collections.singletonList(creationDTO));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(),postResult.getStatus());
    }

    @Test
    public void testFailWithConcernedItems() throws Exception {
        MoveCreationDTO creationDTO = getCreationDto();

        creationDTO.setConcernedItems(Arrays.asList(
                new URI("test:unknown_target1"),
                new URI("test:unknown_target2")
        ));

        final Response postResult = getJsonPostResponse(target(createPath),Collections.singletonList(creationDTO));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(),postResult.getStatus());
    }


    @Test
    public void testCreateAll() throws Exception {

        int n = 10;

        List<MoveEventCreationDTO> dtos = new ArrayList<>(n);

        for(int i=0; i<n;i++){

            InfrastructureFacilityModel fromFacility = fromFacilities.get(i);
            InfrastructureFacilityModel toFacility = toFacilities.get(i);

            MoveEventCreationDTO creationDTO = getCreationDto();
            creationDTO.setDescription("Description "+i);
            creationDTO.setFrom(fromFacility.getUri());
            creationDTO.setTo(toFacility.getUri());
            dtos.add(creationDTO);
        }

        Response postResult = getJsonPostResponse(target(createPath), dtos);

        List<URI> createdUris = extractUriListFromPaginatedListResponse(postResult);
        assertEquals(n,createdUris.size());
    }

//    @Test
    public void testUpdate() throws Exception {
        MoveEventCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(creationDTO));
        URI uri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        // update move by setting no positions
        creationDTO.setDescription("new description");
        Response putResponse = getJsonPutResponse(target(updatePath),creationDTO);
        assertEquals(Response.Status.OK.getStatusCode(),putResponse.getStatus());

        // check that positions have been deleted
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), uri.toString());
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<MoveEventGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<MoveEventGetDTO>>() {
        });
        MoveEventGetDTO dtoFromDb = getResponse.getResult();
    }

//    @Test
    public void testUpdateWithNullNoSqlModel() throws Exception {

        MoveEventCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(creationDTO));
        URI uri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        // update move by setting no positions
        creationDTO.setDescription("new description");
        creationDTO.setConcernedItemPositions(null);
        Response putResponse = getJsonPutResponse(target(updatePath),creationDTO);
        assertEquals(Response.Status.OK.getStatusCode(),putResponse.getStatus());

        // check that positions have been deleted
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), uri.toString());
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<MoveEventGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<MoveEventGetDTO>>() {
        });
        MoveEventGetDTO dtoFromDb = getResponse.getResult();

        assertNotEquals(creationDTO.getDescription(), dtoFromDb.getDescription());
        assertEquals(creationDTO.getStart(), dtoFromDb.getStart());
        assertEquals(creationDTO.getEnd(), dtoFromDb.getEnd());
        assertEquals(creationDTO.getIsInstant(), dtoFromDb.getIsInstant());

        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getTo(), dtoFromDb.getTo().getUri()));
        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getFrom(), dtoFromDb.getFrom().getUri()));

        assertFalse(StringUtils.isEmpty(dtoFromDb.getCreator().toString()));

        Collections.sort(creationDTO.getConcernedItems());
        Collections.sort(dtoFromDb.getConcernedItems());
        assertEquals(creationDTO.getConcernedItems(), dtoFromDb.getConcernedItems());

        assertNull(dtoFromDb.getConcernedItemPositions());
    }

//    @Test
    public void testGetByUri() throws Exception {

        MoveEventCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(creationDTO));

        URI uri =  extractUriListFromPaginatedListResponse(postResult).get(0);
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), uri.toString());

        // try to deserialize object and check if the fields value are the same
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<MoveEventGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<MoveEventGetDTO>>() {
        });
        MoveEventGetDTO dtoFromDb = getResponse.getResult();
        assertNotNull(dtoFromDb);

        testEquals(dtoFromDb,creationDTO);
    }

    private void testEquals(MoveEventGetDTO dtoFromDb, MoveEventCreationDTO creationDTO){
        assertEquals(creationDTO.getDescription(), dtoFromDb.getDescription());
        assertEquals(creationDTO.getStart(), dtoFromDb.getStart());
        assertEquals(creationDTO.getEnd(), dtoFromDb.getEnd());
        assertEquals(creationDTO.getIsInstant(), dtoFromDb.getIsInstant());

        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getTo(), dtoFromDb.getTo().getUri()));
        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getFrom(), dtoFromDb.getFrom().getUri()));

        assertFalse(StringUtils.isEmpty(dtoFromDb.getCreator().toString()));

        Collections.sort(creationDTO.getConcernedItems());
        Collections.sort(dtoFromDb.getConcernedItems());
        assertEquals(creationDTO.getConcernedItems(), dtoFromDb.getConcernedItems());
    }

    private void testEquals(MoveCreationDTO creationDTO, int concernedItemIdx, PositionGetDTO dtoFromDb, URI moveEventUri) {

        assertEquals(SPARQLDeserializers.getExpandedURI(dtoFromDb.getTo().getUri()), SPARQLDeserializers.getExpandedURI(creationDTO.getTo()));
        assertEquals(SPARQLDeserializers.getExpandedURI(dtoFromDb.getTo().getUri()), SPARQLDeserializers.getExpandedURI(creationDTO.getTo()));
        assertEquals(SPARQLDeserializers.getExpandedURI(dtoFromDb.getEvent()), SPARQLDeserializers.getExpandedURI(moveEventUri));
        assertEquals(creationDTO.getEnd(),dtoFromDb.getMoveTime());

        PositionCreationDTO itemPositionCreationDto = creationDTO.getConcernedItemPositions().get(concernedItemIdx).getPosition();
        PositionGetDetailDTO positionNoSqlGetDto = dtoFromDb.getPosition();

        assertNotNull(positionNoSqlGetDto);

        assertEquals(positionNoSqlGetDto.getDescription(), itemPositionCreationDto.getDescription());
        assertEquals(positionNoSqlGetDto.getX(), itemPositionCreationDto.getX());
        assertEquals(positionNoSqlGetDto.getY(), itemPositionCreationDto.getY());
        assertEquals(positionNoSqlGetDto.getZ(), itemPositionCreationDto.getZ());
        assertEquals(positionNoSqlGetDto.getPoint(), itemPositionCreationDto.getPoint());
    }

    @Test
    public void testGetPosition() throws Exception {

        MoveEventCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(creationDTO));
        URI moveEventUri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        // get last move concerning first scientific object
        Response getPositionResult = getJsonGetByUriResponse(target(getPositionPath), creationDTO.getConcernedItems().get(0).toString());
        JsonNode node = getPositionResult.readEntity(JsonNode.class);
        SingleObjectResponse<PositionGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PositionGetDTO>>() {
        });
        PositionGetDTO dtoFromDb = getResponse.getResult();

        testEquals(creationDTO, 0,dtoFromDb, moveEventUri);

        // get last move concerning second scientific object
        getPositionResult = getJsonGetByUriResponse(target(getPositionPath), creationDTO.getConcernedItems().get(1).toString());
        node = getPositionResult.readEntity(JsonNode.class);
        getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PositionGetDTO>>() {
        });
        dtoFromDb = getResponse.getResult();

        testEquals(creationDTO, 1,dtoFromDb, moveEventUri);


        // create a newer event
        MoveEventCreationDTO newEventCreationDto = getCreationDto();
        OffsetDateTime newerEndTime = OffsetDateTime.parse(creationDTO.getEnd()).plusDays(2);
        newEventCreationDto.setEnd(newerEndTime.toString());
        newEventCreationDto.setFrom(creationDTO.getTo());
        newEventCreationDto.setTo(facilityC.getUri());

        postResult = getJsonPostResponse(target(createPath), Collections.singletonList(newEventCreationDto));
        URI newerMoveEventUri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        getPositionResult = getJsonGetByUriResponse(target(getPositionPath), newEventCreationDto.getConcernedItems().get(0).toString());
        node = getPositionResult.readEntity(JsonNode.class);
        getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PositionGetDTO>>() {
        });
        dtoFromDb = getResponse.getResult();

        testEquals(newEventCreationDto,0, dtoFromDb, newerMoveEventUri);

        // get last move concerning second scientific object
        getPositionResult = getJsonGetByUriResponse(target(getPositionPath), newEventCreationDto.getConcernedItems().get(1).toString());
        node = getPositionResult.readEntity(JsonNode.class);
        getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PositionGetDTO>>() {
        });
        dtoFromDb = getResponse.getResult();

        testEquals(newEventCreationDto, 1,dtoFromDb, newerMoveEventUri);
    }

    static final String CONCERNED_ITEM_HISTORY_PARAM = "concernedItemUri";

    @Test
    public void testGetPositionHistory() throws Exception {

        MoveEventCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), Collections.singletonList(creationDTO));
        URI moveEventUri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        // search given a description witch match both events
        Map<String, Object> params = new HashMap<String, Object>() {{
            put(CONCERNED_ITEM_HISTORY_PARAM,creationDTO.getConcernedItems().get(0));
        }};
        List<PositionGetDTO> history = getResults(getPositionHistoryPath,params,new TypeReference<PaginatedListResponse<PositionGetDTO>>() {});

        assertEquals(1,history.size());

        testEquals(creationDTO,0,history.get(0),moveEventUri);

        // create a newer event
        MoveEventCreationDTO newEventCreationDto = getCreationDto();
        OffsetDateTime newerEndTime = OffsetDateTime.parse(creationDTO.getEnd()).plusDays(2);
        newEventCreationDto.setEnd(newerEndTime.toString());
        newEventCreationDto.setFrom(creationDTO.getTo());
        newEventCreationDto.setTo(facilityC.getUri());

        postResult = getJsonPostResponse(target(createPath), Collections.singletonList(newEventCreationDto));
        URI newerMoveEventUri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        history = getResults(getPositionHistoryPath,params,new TypeReference<PaginatedListResponse<PositionGetDTO>>() {});

        assertEquals(2,history.size());

        testEquals(newEventCreationDto,0,history.get(0),newerMoveEventUri);
        testEquals(creationDTO,0, history.get(1),moveEventUri);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Arrays.asList(InfrastructureFacilityModel.class,EventModel.class);
    }

    @After
    public void deleteScientificObjectGraph() throws URISyntaxException, SPARQLException {
        getSparqlService().clearGraph(new URI(scientificObjectGraph.getURI()));
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return Collections.singletonList(MoveEventDAO.moveCollectionName);
    }
}