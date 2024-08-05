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
import org.opensilex.core.event.api.move.MoveCreationDTO;
import org.opensilex.core.event.api.move.MoveDetailsDTO;
import org.opensilex.core.event.api.move.MoveGetDTO;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.move.MoveEventNoSqlDao;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.position.api.*;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.security.SecurityModule;
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

    public static String path = EventAPI.MOVE_PATH;

    public static String getByUriPath = path + "/{uri}";
    public static String getByUriListPath = path + "/by_uris";
    public static String createPath = path;
    public static String updatePath = path ;
    public static String deletePath = path+ "/{uri}";

    public static String positionPath = PositionAPI.PATH;
    public static String getPositionPath = positionPath + "/{uri}";
    public static String getPositionHistoryPath = positionPath + "/history";

    private FacilityModel facilityA;
    private FacilityModel facilityB;
    private FacilityModel facilityC;

    private Node scientificObjectGraph;
    private ScientificObjectModel scientificObjectA;
    private ScientificObjectModel scientificObjectB;
    private ScientificObjectModel scientificObjectC;

    private List<FacilityModel> fromFacilities;
    private List<FacilityModel> toFacilities;
    private final static int nbMoveMax = 10;

    private static final ServiceDescription create;
    private static final ServiceDescription getByUriList;

    static {
        try {
            create = new ServiceDescription(
                    EventAPI.class.getMethod("createMoves", List.class),
                    path
            );
            getByUriList = new ServiceDescription(
                    EventAPI.class.getMethod("getMoveEventByUris", List.class),
                    getByUriListPath
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void createFacilities() throws Exception {

        facilityA = new FacilityModel();
        facilityA.setUri(new URI("test:greenHouseA"));
        facilityA.setName("greenHouseA");

        facilityB = new FacilityModel();
        facilityB.setUri(new URI("test:greenHouseB"));
        facilityB.setName("greenHouseB");

        facilityC = new FacilityModel();
        facilityC.setUri(new URI("test:greenHouseC"));
        facilityC.setName("greenHouseC");

        SPARQLService sparql = getSparqlService();
        Node facilityGraph = sparql.getDefaultGraph(FacilityModel.class);
        sparql.create(facilityGraph,Arrays.asList(facilityA, facilityB, facilityC));

        //
        fromFacilities = new ArrayList<>(nbMoveMax);
        toFacilities = new ArrayList<>(nbMoveMax);

        for(int i=0; i<nbMoveMax;i++){

            FacilityModel fromFacility = new FacilityModel();
            fromFacility.setUri(new URI("test:from_facility_"+i));
            fromFacility.setName("from_facility_"+i);
            fromFacilities.add(fromFacility);

            FacilityModel toFacility = new FacilityModel();
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

    private MoveCreationDTO getCreationDtoWithoutPosition() {
        MoveCreationDTO dto = new MoveCreationDTO();
        dto.setDescription("A test event");
        dto.setIsInstant(false);
        OffsetDateTime now = OffsetDateTime.now();
        dto.setStart(now.toString());
        dto.setEnd(now.plusMinutes(2).toString());

        dto.setTargets(Arrays.asList(
                scientificObjectA.getUri(),
                scientificObjectB.getUri(),
                scientificObjectC.getUri()
        ));

        dto.setFrom(facilityA.getUri());
        dto.setTo(facilityB.getUri());

        return dto;
    }

    private MoveCreationDTO getCreationDto() {
        var dto = getCreationDtoWithoutPosition();

        // first position
        PositionCreationDTO k2Summit = new PositionCreationDTO();
        k2Summit.setDescription("Not far from sky");
        k2Summit.setX("A");
        k2Summit.setY("76");
        k2Summit.setZ("8,611");
        k2Summit.setPoint(new Point(76.513333, 35.8825, 8611));

        TargetPositionCreationDTO so1Position = new TargetPositionCreationDTO();
        so1Position.setTarget(dto.getTargets().get(0));
        so1Position.setPosition(k2Summit);

        // 2nd position
        PositionCreationDTO k2Camp4 = new PositionCreationDTO();
        k2Camp4.setDescription("Just behind the Bottleneck");
        k2Camp4.setZ("7600");

        TargetPositionCreationDTO so2Position = new TargetPositionCreationDTO();
        so2Position.setTarget(dto.getTargets().get(1));
        so2Position.setPosition(k2Camp4);

        dto.setTargetsPositions(Arrays.asList(so1Position,so2Position));

        return dto;
    }

    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateListGetAndDelete(createPath, getByUriPath, deletePath, Collections.singletonList(getCreationDto()));
    }

    @Test
    public void testFailWithUnknownTo() throws Exception {

        MoveCreationDTO creationDTO = getCreationDto();
        creationDTO.setTo(new URI("test:unknownFacilityA"));

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),Collections.singletonList(creationDTO));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),postResult.getStatus());
    }

    @Test
    public void testFailWithUnknownFrom() throws Exception {
        MoveCreationDTO creationDTO = getCreationDto();
        creationDTO.setFrom(new URI("test:unknownFacilityA"));

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),Collections.singletonList(creationDTO));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),postResult.getStatus());
    }

    @Test
    public void testFailWithUnknownTargets() throws Exception {

        MoveCreationDTO dto = getCreationDto();
        dto.setTargets(Arrays.asList(new URI("oeev:unknown_target")));

        Response postResult = getJsonPostResponseAsAdmin(target(createPath),Collections.singletonList(dto));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());

    }


    @Test
    public void testCreateAll() throws Exception {

        int n = 10;

        List<MoveCreationDTO> dtos = new ArrayList<>(n);

        for(int i=0; i<n;i++){

            FacilityModel fromFacility = fromFacilities.get(i);
            FacilityModel toFacility = toFacilities.get(i);

            MoveCreationDTO creationDTO = getCreationDto();
            creationDTO.setDescription("Description "+i);
            creationDTO.setFrom(fromFacility.getUri());
            creationDTO.setTo(toFacility.getUri());
            dtos.add(creationDTO);
        }

        Response postResult = getJsonPostResponseAsAdmin(target(createPath), dtos);

        List<URI> createdUris = extractUriListFromPaginatedListResponse(postResult);
        assertEquals(n,createdUris.size());
    }

//    @Test
    public void testUpdate() throws Exception {
        MoveCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), Collections.singletonList(creationDTO));
        URI uri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        // update move by setting no positions
        creationDTO.setDescription("new description");
        Response putResponse = getJsonPutResponse(target(updatePath),creationDTO);
        assertEquals(Response.Status.OK.getStatusCode(),putResponse.getStatus());

        // check that positions have been deleted
        Response getResult = getJsonGetByUriResponseAsAdmin(target(getByUriPath), uri.toString());
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<MoveGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<MoveGetDTO>>() {
        });
        MoveGetDTO dtoFromDb = getResponse.getResult();
    }

//    @Test
    public void testUpdateWithNullNoSqlModel() throws Exception {

        MoveCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), Collections.singletonList(creationDTO));
        URI uri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        // update move by setting no positions
        creationDTO.setDescription("new description");
        creationDTO.setTargetsPositions(null);
        Response putResponse = getJsonPutResponse(target(updatePath),creationDTO);
        assertEquals(Response.Status.OK.getStatusCode(),putResponse.getStatus());

        // check that positions have been deleted
        Response getResult = getJsonGetByUriResponseAsAdmin(target(getByUriPath), uri.toString());
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<MoveGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<MoveGetDTO>>() {
        });
        MoveGetDTO dtoFromDb = getResponse.getResult();

        assertNotEquals(creationDTO.getDescription(), dtoFromDb.getDescription());
        assertEquals(creationDTO.getStart(), dtoFromDb.getStart());
        assertEquals(creationDTO.getEnd(), dtoFromDb.getEnd());
        assertEquals(creationDTO.getIsInstant(), dtoFromDb.getIsInstant());

        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getTo(), dtoFromDb.getTo().getUri()));
        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getFrom(), dtoFromDb.getFrom().getUri()));

        assertFalse(StringUtils.isEmpty(dtoFromDb.getPublisher().toString()));

        Collections.sort(creationDTO.getTargets());
        Collections.sort(dtoFromDb.getTargets());
        assertEquals(creationDTO.getTargets(), dtoFromDb.getTargets());

        assertNull(dtoFromDb.getTargetsPositions());
    }

    @Test
    public void testGetByUri() throws Exception {

        MoveCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), Collections.singletonList(creationDTO));

        URI uri =  extractUriListFromPaginatedListResponse(postResult).get(0);
        Response getResult = getJsonGetByUriResponseAsAdmin(target(getByUriPath), uri.toString());

        // try to deserialize object and check if the fields value are the same
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<MoveGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<MoveGetDTO>>() {
        });
        MoveGetDTO dtoFromDb = getResponse.getResult();
        assertNotNull(dtoFromDb);

        testEquals(dtoFromDb,creationDTO);
    }

    @Test
    public void testGetListByUri() throws Exception {
        // This move MUST NOT be returned by the service
        var creationDto1 = getCreationDto();
        // This move MUST be returned by the service with its position information
        var creationDto2 = getCreationDto();
        // This move MUST be returned by the service even if its position information is non existant
        var creationDto3 = getCreationDtoWithoutPosition();

        var uriList = new UserCallBuilder(create)
                .setBody(List.of(creationDto1, creationDto2, creationDto3))
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<URI>>() {})
                .getDeserializedResponse();

        var uri1 = uriList.getResult().get(0);
        var uri2 = uriList.getResult().get(1);
        var uri3 = uriList.getResult().get(2);

        // Retrieve the moves 2 and 3
        var result23 = new UserCallBuilder(getByUriList)
                .addParam("uris", List.of(uri2, uri3))
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<MoveDetailsDTO>>() {})
                .getDeserializedResponse();

        // The service MUST return the moves 2 and 3
        assertEquals(2, result23.getResult().size());
        assertTrue(result23.getResult().stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(), uri2)));
        assertTrue(result23.getResult().stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(), uri3)));

        var result2 = result23.getResult().stream().filter(dto -> SPARQLDeserializers.compareURIs(dto.getUri(), uri2)).findFirst()
                .orElseThrow();
        var result3 = result23.getResult().stream().filter(dto -> SPARQLDeserializers.compareURIs(dto.getUri(), uri3)).findFirst()
                .orElseThrow();

        // Move 2 MUST have its position information. Move 3 MUST NOT have any position information.
        assertNotNull(result2.getTargetsPositions());
        assertEquals(2, result2.getTargetsPositions().size());
        assertEquals("A", result2.getTargetsPositions().get(0).getPosition().getX());
        assertNull(result3.getTargetsPositions());

        // Both moves MUST have their metadata
        assertNotNull(result2.getPublisher());
        assertEquals(SecurityModule.DEFAULT_SUPER_ADMIN_EMAIL, result2.getPublisher().getEmail());
        assertNotNull(result3.getPublisher());
        assertEquals(SecurityModule.DEFAULT_SUPER_ADMIN_EMAIL, result3.getPublisher().getEmail());
    }

    private void testEquals(MoveGetDTO dtoFromDb, MoveCreationDTO creationDTO){
        assertEquals(creationDTO.getDescription(), dtoFromDb.getDescription());
        assertEquals(creationDTO.getStart(), dtoFromDb.getStart());
        assertEquals(creationDTO.getEnd(), dtoFromDb.getEnd());
        assertEquals(creationDTO.getIsInstant(), dtoFromDb.getIsInstant());

        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getTo(), dtoFromDb.getTo().getUri()));
        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getFrom(), dtoFromDb.getFrom().getUri()));

        assertFalse(StringUtils.isEmpty(dtoFromDb.getPublisher().toString()));

        Collections.sort(creationDTO.getTargets());
        Collections.sort(dtoFromDb.getTargets());
        assertEquals(creationDTO.getTargets(), dtoFromDb.getTargets());
    }

    private void testEquals(MoveCreationDTO creationDTO, int concernedItemIdx, PositionGetDTO dtoFromDb, URI moveEventUri) {

        assertEquals(SPARQLDeserializers.getExpandedURI(dtoFromDb.getTo().getUri()), SPARQLDeserializers.getExpandedURI(creationDTO.getTo()));
        assertEquals(SPARQLDeserializers.getExpandedURI(dtoFromDb.getTo().getUri()), SPARQLDeserializers.getExpandedURI(creationDTO.getTo()));
        assertEquals(SPARQLDeserializers.getExpandedURI(dtoFromDb.getEvent()), SPARQLDeserializers.getExpandedURI(moveEventUri));
        assertEquals(creationDTO.getEnd(),dtoFromDb.getMoveTime());

        PositionCreationDTO itemPositionCreationDto = creationDTO.getTargetsPositions().get(concernedItemIdx).getPosition();
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

        MoveCreationDTO moveCreationDTO = getCreationDto();
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), Collections.singletonList(moveCreationDTO));
        URI moveEventUri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        // get last move concerning first scientific object
        Response getPositionResult = getJsonGetByUriResponseAsAdmin(target(getPositionPath), moveCreationDTO.getTargets().get(0).toString());
        JsonNode node = getPositionResult.readEntity(JsonNode.class);
        SingleObjectResponse<PositionGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PositionGetDTO>>() {
        });
        PositionGetDTO dtoFromDb = getResponse.getResult();

        testEquals(moveCreationDTO, 0,dtoFromDb, moveEventUri);

        // get last move concerning second scientific object
        getPositionResult = getJsonGetByUriResponseAsAdmin(target(getPositionPath), moveCreationDTO.getTargets().get(1).toString());
        node = getPositionResult.readEntity(JsonNode.class);
        getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PositionGetDTO>>() {
        });
        dtoFromDb = getResponse.getResult();

        testEquals(moveCreationDTO, 1,dtoFromDb, moveEventUri);


        // create a newer event
        MoveCreationDTO newEventCreationDto = getCreationDto();
        OffsetDateTime newerEndTime = OffsetDateTime.parse(moveCreationDTO.getEnd()).plusDays(2);
        newEventCreationDto.setEnd(newerEndTime.toString());
        newEventCreationDto.setFrom(moveCreationDTO.getTo());
        newEventCreationDto.setTo(facilityC.getUri());

        postResult = getJsonPostResponseAsAdmin(target(createPath), Collections.singletonList(newEventCreationDto));
        URI newerMoveEventUri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        getPositionResult = getJsonGetByUriResponseAsAdmin(target(getPositionPath), newEventCreationDto.getTargets().get(0).toString());
        node = getPositionResult.readEntity(JsonNode.class);
        getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PositionGetDTO>>() {
        });
        dtoFromDb = getResponse.getResult();

        testEquals(newEventCreationDto,0, dtoFromDb, newerMoveEventUri);

        // get last move concerning second scientific object
        getPositionResult = getJsonGetByUriResponseAsAdmin(target(getPositionPath), newEventCreationDto.getTargets().get(1).toString());
        node = getPositionResult.readEntity(JsonNode.class);
        getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PositionGetDTO>>() {
        });
        dtoFromDb = getResponse.getResult();

        testEquals(newEventCreationDto, 1,dtoFromDb, newerMoveEventUri);
    }

    static final String TARGET_HISTORY_PARAM = "target";

    @Test
    public void testGetPositionHistory() throws Exception {

        MoveCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), Collections.singletonList(creationDTO));
        URI moveEventUri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        // search given a description witch match both events
        Map<String, Object> params = new HashMap<String, Object>() {{
            put(TARGET_HISTORY_PARAM,creationDTO.getTargets().get(0));
        }};
        List<PositionGetDTO> history = getSearchResultsAsAdmin(getPositionHistoryPath,params,new TypeReference<PaginatedListResponse<PositionGetDTO>>() {});

        assertEquals(1,history.size());

        testEquals(creationDTO,0,history.get(0),moveEventUri);

        // create a newer event
        MoveCreationDTO newEventCreationDto = getCreationDto();
        OffsetDateTime newerEndTime = OffsetDateTime.parse(creationDTO.getEnd()).plusDays(2);
        newEventCreationDto.setEnd(newerEndTime.toString());
        newEventCreationDto.setFrom(creationDTO.getTo());
        newEventCreationDto.setTo(facilityC.getUri());

        postResult = getJsonPostResponseAsAdmin(target(createPath), Collections.singletonList(newEventCreationDto));
        URI newerMoveEventUri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        history = getSearchResultsAsAdmin(getPositionHistoryPath,params,new TypeReference<PaginatedListResponse<PositionGetDTO>>() {});

        assertEquals(2,history.size());

        testEquals(newEventCreationDto,0,history.get(0),newerMoveEventUri);
        testEquals(creationDTO,0, history.get(1),moveEventUri);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Arrays.asList(FacilityModel.class,EventModel.class);
    }

    @After
    public void deleteScientificObjectGraph() throws URISyntaxException, SPARQLException {
        getSparqlService().clearGraph(new URI(scientificObjectGraph.getURI()));
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return Collections.singletonList(MoveEventNoSqlDao.COLLECTION_NAME);
    }
}