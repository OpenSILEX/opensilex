package org.opensilex.core.event.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.geojson.Feature;
import org.geojson.Point;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.event.api.move.MoveCreationDTO;
import org.opensilex.core.event.api.move.MoveDetailsDTO;
import org.opensilex.core.event.api.move.MoveGetDTO;
import org.opensilex.core.event.api.move.MoveUpdateDTO;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.location.dal.LocationObservationDAO;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.position.api.PositionAPI;
import org.opensilex.core.position.api.PositionCreationDTO;
import org.opensilex.core.position.api.PositionGetDTO;
import org.opensilex.core.position.api.TargetPositionCreationDTO;
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
import static junit.framework.TestCase.assertNull;
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
    private static final ServiceDescription update;
    private static final ServiceDescription getByUriList;

    static {
        try {
            create = new ServiceDescription(
                    EventAPI.class.getMethod("createMoves", List.class),
                    path
            );
            update = new ServiceDescription(
                    EventAPI.class.getMethod("updateMoveEvent", MoveUpdateDTO.class),
                    updatePath
            );
            getByUriList = new ServiceDescription(
                    EventAPI.class.getMethod("searchMoveEventByUris", List.class),
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
        return getCreationDto(false);
    }

    private MoveCreationDTO getCreationDtoWithPosition(){
        return getCreationDto(true);
    }

    private MoveCreationDTO getCreationDto(boolean withPosition){
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

        //Moves now depend on LocationObservations so we first need to make that
        LocationModel locationModel = new LocationModel();
        locationModel.setFrom(facilityA.getUri());
        locationModel.setTo(facilityB.getUri());

        LocationObservationModel locationObservationModel = new LocationObservationModel();
        locationObservationModel.setHasGeometry(!withPosition);
        locationObservationModel.setLocation(locationModel);

        dto.setLocation(LocationObservationDTO.getDTOFromModel(locationObservationModel));

        if(withPosition){
            dto.getLocation().setX("72");
            dto.getLocation().setY("500");
            dto.getLocation().setZ("400");
        }

        return dto;
    }

    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateListGetAndDelete(createPath, getByUriPath, deletePath, Collections.singletonList(getCreationDtoWithoutPosition()));
    }

    @Test
    public void testFailWithUnknownTo() throws Exception {

        MoveCreationDTO creationDTO = getCreationDtoWithoutPosition();
        creationDTO.getLocation().setTo(new URI("test:unknownFacilityA"));

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),Collections.singletonList(creationDTO));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),postResult.getStatus());
    }

    @Test
    public void testFailWithUnknownFrom() throws Exception {
        MoveCreationDTO creationDTO = getCreationDtoWithoutPosition();
        creationDTO.getLocation().setFrom(new URI("test:unknownFacilityA"));

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),Collections.singletonList(creationDTO));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),postResult.getStatus());
    }

    @Test
    public void testFailWithUnknownTargets() throws Exception {

        MoveCreationDTO dto = getCreationDtoWithoutPosition();
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

            MoveCreationDTO creationDTO = getCreationDtoWithoutPosition();

            OffsetDateTime newerEndTime = OffsetDateTime.parse(creationDTO.getEnd()).plusDays(i);
            creationDTO.setEnd(newerEndTime.toString());
            OffsetDateTime newerStartTime = OffsetDateTime.parse(creationDTO.getStart()).plusDays(i);
            creationDTO.setStart(newerStartTime.toString());

            creationDTO.setDescription("Description "+i);

            creationDTO.getLocation().setFrom(fromFacility.getUri());
            creationDTO.getLocation().setTo(toFacility.getUri());

            dtos.add(creationDTO);
        }

        Response postResult = getJsonPostResponseAsAdmin(target(createPath), dtos);

        List<URI> createdUris = extractUriListFromPaginatedListResponse(postResult);
        assertEquals(n,createdUris.size());
    }

    @Test
    public void testUpdate() throws Exception {
        MoveCreationDTO creationDTO = getCreationDtoWithoutPosition();
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), Collections.singletonList(creationDTO));
        URI uri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        // update move by setting no positions
        creationDTO.setDescription("new description");
        creationDTO.setUri(uri);
        creationDTO.getLocation().setX("72");
        Response putResponse = getJsonPutResponse(target(updatePath),creationDTO);
        assertEquals(Response.Status.OK.getStatusCode(),putResponse.getStatus());

        // check that position has been added and description changed
        Response getResult = getJsonGetByUriResponseAsAdmin(target(getByUriPath), uri.toString());
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<MoveGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<MoveGetDTO>>() {
        });
        MoveGetDTO dtoFromDb = getResponse.getResult();
        assertEquals(dtoFromDb.getLocation().getX(),creationDTO.getLocation().getX());
        assertEquals(dtoFromDb.getDescription(), creationDTO.getDescription());
    }

    @Test
    public void testUpdateWithDeletingPosition() throws Exception {

        MoveCreationDTO creationDTO = getCreationDtoWithPosition();
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), Collections.singletonList(creationDTO));
        URI uri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        // update move by setting no positions
        String oldDescription = creationDTO.getDescription();
        creationDTO.setDescription("new description");
        creationDTO.getLocation().setX(null);
        creationDTO.setUri(uri);

        Response putResponse = getJsonPutResponse(target(updatePath),creationDTO);
        assertEquals(Response.Status.OK.getStatusCode(),putResponse.getStatus());

        // check that positions have been deleted
        Response getResult = getJsonGetByUriResponseAsAdmin(target(getByUriPath), uri.toString());
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<MoveGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<MoveGetDTO>>() {
        });
        MoveGetDTO dtoFromDb = getResponse.getResult();

        assertNotEquals(oldDescription, dtoFromDb.getDescription());
        assertEquals(creationDTO.getStart(), dtoFromDb.getStart());
        assertEquals(creationDTO.getEnd(), dtoFromDb.getEnd());
        assertEquals(creationDTO.getIsInstant(), dtoFromDb.getIsInstant());

        assertFalse(StringUtils.isEmpty(dtoFromDb.getPublisher().toString()));

        Collections.sort(creationDTO.getTargets());
        Collections.sort(dtoFromDb.getTargets());
        assertEquals(creationDTO.getTargets(), dtoFromDb.getTargets());

        assertNull(dtoFromDb.getLocation().getX());
    }

    @Test
    public void testGetByUri() throws Exception {

        MoveCreationDTO creationDTO = getCreationDtoWithoutPosition();
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
        var creationDto1 = getCreationDtoWithoutPosition();
        // This move MUST be returned by the service with its position information
        var creationDto2 = getCreationDtoWithPosition();
        OffsetDateTime newerEndTime = OffsetDateTime.parse(creationDto2.getEnd()).plusDays(2);
        OffsetDateTime newerStartTime = OffsetDateTime.parse(creationDto2.getStart()).plusDays(2);
        creationDto2.setEnd(newerEndTime.toString());
        creationDto2.setStart(newerStartTime.toString());
        // This move MUST be returned by the service even if its position information is non existant
        var creationDto3 = getCreationDtoWithoutPosition();
        OffsetDateTime superNewerEndTime = OffsetDateTime.parse(creationDto3.getEnd()).plusDays(4);
        OffsetDateTime superNewerStartTime = OffsetDateTime.parse(creationDto3.getStart()).plusDays(4);
        creationDto3.setEnd(superNewerEndTime.toString());
        creationDto3.setStart(superNewerStartTime.toString());

        var uriList = new UserCallBuilder(create)
                .setBody(List.of(creationDto1, creationDto2, creationDto3))
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<URI>>() {})
                .getDeserializedResponse();

        var uri2 = uriList.getResult().get(1);
        var uri3 = uriList.getResult().get(2);

        // Retrieve the moves 2 and 3
        var result23 = getMoveByUris(List.of(uri2, uri3));

        // The service MUST return the moves 2 and 3
        assertEquals(2, result23.size());
        assertTrue(result23.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(), uri2)));
        assertTrue(result23.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(), uri3)));

        var result2 = result23.stream().filter(dto -> SPARQLDeserializers.compareURIs(dto.getUri(), uri2)).findFirst()
                .orElseThrow();
        var result3 = result23.stream().filter(dto -> SPARQLDeserializers.compareURIs(dto.getUri(), uri3)).findFirst()
                .orElseThrow();

        // Move 2 MUST have its position information. Move 3 MUST NOT have any position information.
        assertNotNull(result2.getLocation().getX());
        assertEquals("72", result2.getLocation().getX());
        assertNull(result3.getLocation().getX());

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

        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getLocation().getTo(), dtoFromDb.getLocation().getTo()));
        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getLocation().getFrom(), dtoFromDb.getLocation().getFrom()));

        assertFalse(StringUtils.isEmpty(dtoFromDb.getPublisher().toString()));

        Collections.sort(creationDTO.getTargets());
        Collections.sort(dtoFromDb.getTargets());
        assertEquals(creationDTO.getTargets(), dtoFromDb.getTargets());
    }

    private void testEquals(
            LocationObservationDTO creationDTO,
            LocationObservationDTO dtoFromDb,
            URI moveEventUri
    ) {
        assertNotNull(dtoFromDb);

        assertEquals(SPARQLDeserializers.getExpandedURI(dtoFromDb.getFrom()), SPARQLDeserializers.getExpandedURI(creationDTO.getFrom()));
        assertEquals(SPARQLDeserializers.getExpandedURI(dtoFromDb.getTo()), SPARQLDeserializers.getExpandedURI(creationDTO.getTo()));

        assertEquals(creationDTO.getX(), dtoFromDb.getX());
        assertEquals(creationDTO.getY(), dtoFromDb.getY());
        assertEquals(creationDTO.getZ(), dtoFromDb.getZ());
    }

    @Test
    public void testGetPosition() throws Exception {

        MoveCreationDTO moveCreationDTO = getCreationDtoWithPosition();
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), Collections.singletonList(moveCreationDTO));
        URI moveEventUri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        // get last position concerning first scientific object
        Response getPositionResult = getJsonGetByUriResponseAsAdmin(target(getPositionPath), moveCreationDTO.getTargets().get(0).toString());
        JsonNode node = getPositionResult.readEntity(JsonNode.class);
        SingleObjectResponse<PositionGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PositionGetDTO>>() {
        });
        PositionGetDTO dtoFromDb = getResponse.getResult();

        testEquals(moveCreationDTO.getLocation(),dtoFromDb.getLocation(), moveEventUri);

        // get last move concerning second scientific object
        getPositionResult = getJsonGetByUriResponseAsAdmin(target(getPositionPath), moveCreationDTO.getTargets().get(1).toString());
        node = getPositionResult.readEntity(JsonNode.class);
        getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PositionGetDTO>>() {
        });
        dtoFromDb = getResponse.getResult();

        testEquals(moveCreationDTO.getLocation(),dtoFromDb.getLocation(), moveEventUri);


        // create a newer event
        MoveCreationDTO newEventCreationDto = getCreationDtoWithPosition();
        OffsetDateTime newerEndTime = OffsetDateTime.parse(moveCreationDTO.getEnd()).plusDays(2);
        OffsetDateTime newerStartTime = OffsetDateTime.parse(moveCreationDTO.getStart()).plusDays(2);
        newEventCreationDto.setEnd(newerEndTime.toString());
        newEventCreationDto.setStart(newerStartTime.toString());
        newEventCreationDto.getLocation().setFrom(moveCreationDTO.getLocation().getTo());
        newEventCreationDto.getLocation().setTo(facilityC.getUri());

        postResult = getJsonPostResponseAsAdmin(target(createPath), Collections.singletonList(newEventCreationDto));
        URI newerMoveEventUri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        getPositionResult = getJsonGetByUriResponseAsAdmin(target(getPositionPath), newEventCreationDto.getTargets().get(0).toString());
        node = getPositionResult.readEntity(JsonNode.class);
        getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PositionGetDTO>>() {
        });
        dtoFromDb = getResponse.getResult();

        testEquals(newEventCreationDto.getLocation(), dtoFromDb.getLocation(), newerMoveEventUri);

        // get last move concerning second scientific object
        getPositionResult = getJsonGetByUriResponseAsAdmin(target(getPositionPath), newEventCreationDto.getTargets().get(1).toString());
        node = getPositionResult.readEntity(JsonNode.class);
        getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PositionGetDTO>>() {
        });
        dtoFromDb = getResponse.getResult();

        testEquals(newEventCreationDto.getLocation(),dtoFromDb.getLocation(), newerMoveEventUri);
    }

    static final String TARGET_HISTORY_PARAM = "target";

    @Test
    public void testGetPositionHistory() throws Exception {

        MoveCreationDTO creationDTO = getCreationDtoWithPosition();
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), Collections.singletonList(creationDTO));
        URI moveEventUri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        // search given a description witch match both events
        Map<String, Object> params = new HashMap<String, Object>() {{
            put(TARGET_HISTORY_PARAM,creationDTO.getTargets().get(0));
        }};
        List<MoveGetDTO> history = getSearchResultsAsAdmin(getPositionHistoryPath,params,new TypeReference<PaginatedListResponse<MoveGetDTO>>() {});

        assertEquals(1,history.size());

        testEquals(creationDTO.getLocation(),history.get(0).getLocation(),moveEventUri);

        // create a newer event
        MoveCreationDTO newEventCreationDto = getCreationDtoWithPosition();
        OffsetDateTime newerEndTime = OffsetDateTime.parse(creationDTO.getEnd()).plusDays(2);
        newEventCreationDto.setEnd(newerEndTime.toString());
        OffsetDateTime newerStartTime = OffsetDateTime.parse(creationDTO.getStart()).plusDays(2);
        newEventCreationDto.setStart(newerStartTime.toString());
        newEventCreationDto.getLocation().setFrom(creationDTO.getLocation().getTo());
        newEventCreationDto.getLocation().setTo(facilityC.getUri());

        postResult = getJsonPostResponseAsAdmin(target(createPath), Collections.singletonList(newEventCreationDto));
        URI newerMoveEventUri =  extractUriListFromPaginatedListResponse(postResult).get(0);

        history = getSearchResultsAsAdmin(getPositionHistoryPath,params,new TypeReference<PaginatedListResponse<MoveGetDTO>>() {});

        assertEquals(2,history.size());

        testEquals(newEventCreationDto.getLocation(),history.get(0).getLocation(),newerMoveEventUri);
        testEquals(creationDTO.getLocation(), history.get(1).getLocation(),moveEventUri);
    }

    //#region retro compatibility to OpenSILEX 1.4

    /**
     * @return a MoveCreationDTO without location, but with deprecated fields (from, to, targets_positions)
     */
    private MoveCreationDTO getMoveDTOFilledAsIn1dot4(){
        MoveCreationDTO dto = new MoveCreationDTO();
        dto.setDescription("A test event");
        dto.setIsInstant(false);
        OffsetDateTime now = OffsetDateTime.now();
        dto.setStart(now.toString());
        dto.setEnd(now.plusMinutes(2).toString());

        dto.setTargets(Arrays.asList(scientificObjectA.getUri()));

        dto.setFrom(facilityA.getUri());
        dto.setTo(facilityB.getUri());

        TargetPositionCreationDTO targetPositionDTO = new TargetPositionCreationDTO();
        PositionCreationDTO positionDTO = new PositionCreationDTO();
        targetPositionDTO.setPosition(positionDTO);
        dto.setTargetsPositions(new ArrayList<>(List.of(targetPositionDTO)));

        positionDTO.setX("72");
        positionDTO.setY("500");
        positionDTO.setZ("400");
        positionDTO.setDescription("textual position");
        positionDTO.setPoint(new Point(1, 1));

        return dto;
    }


    /**
     * check that for a move created with 3 targets_positions we create 3 different moves with right fields
     * also check that deprecated properties are well retrieved in the location property
     */
    @Test
    public void ImportWithManyTargetsPositionCreateManyMovesRetroCompatibility() throws Exception {
        MoveCreationDTO moveDTO = getMoveDTOFilledAsIn1dot4();

        moveDTO.getTargetsPositions().get(0).getPosition().setDescription(null);

        for (int i = 0; i < 2; i++) {
            TargetPositionCreationDTO targetPositionDTO = new TargetPositionCreationDTO();
            PositionCreationDTO positionDTO = new PositionCreationDTO();
            targetPositionDTO.setPosition(positionDTO);
            positionDTO.setPoint(new Point(5, 10));
            positionDTO.setDescription("textual position");
            moveDTO.getTargetsPositions().add(targetPositionDTO);
        }

        moveDTO.getTargetsPositions().get(1).setTarget(scientificObjectB.getUri());
        moveDTO.getTargetsPositions().get(2).setTarget(scientificObjectC.getUri());

        var uriList = new UserCallBuilder(create)
                .setBody(List.of(moveDTO))
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<URI>>() {})
                .getDeserializedResponse()
                .getResult();

        assertEquals("3 different move should have been created as the move has 3 target_positions", 3, uriList.size());

        var resultModels = getMoveByUris(uriList);

        var firstMove = resultModels.stream().filter(m -> SPARQLDeserializers.compareURIs(m.getUri(), uriList.get(0))).findFirst().orElseThrow();
        assertEquals("target_position.position.x should now be on location.x", "72", firstMove.getLocation().getX());
        assertEquals("target_position.position.y should now be on location.y", "500", firstMove.getLocation().getY());
        assertEquals("target_position.position.z should now be on location.z", "400", firstMove.getLocation().getZ());
        assertNull("target_position.position.description should now be on location.description", firstMove.getLocation().getTextualPosition());
        Feature firstMoveGeojson = (Feature) firstMove.getLocation().getGeojson();
        assertEquals("target_position.position.point should now be on location.geojson", new Point(1, 1), firstMoveGeojson.getGeometry());

        var otherMoves = resultModels.stream().filter(m -> !SPARQLDeserializers.compareURIs(m.getUri(), uriList.get(0))).toList();
        for (MoveDetailsDTO move : otherMoves) {
            assertNull("other moves was created without x position", move.getLocation().getX());
            assertNull("other moves was created without y position", move.getLocation().getY());
            assertNull("other moves was created without z position", move.getLocation().getZ());
            assertEquals("target_position.position.description should now be on location.description", "textual position", move.getLocation().getTextualPosition());
            Feature otherMoveGeojson = (Feature) move.getLocation().getGeojson();
            assertEquals("other moves created from the same moveDTO should have the same position information", new Point(5, 10), otherMoveGeojson.getGeometry());
        }

    }

    @Test
    public void updateMoveWithDeprecatedPropertiesReturnError() {
        MoveCreationDTO moveDTO = getMoveDTOFilledAsIn1dot4();
        new UserCallBuilder(update)
                .setBody(List.of(moveDTO))
                .buildAdmin()
                .executeCallAndAssertStatus("move with some deprecated properties should return a bad request error", Response.Status.BAD_REQUEST);

    }

    /**
     *
     */
    @Test
    public void createWithLocationIgnoreDeprecatedProperties() throws Exception {
        URI moveURI = URI.create("http://my/move/without/deprecated/infos");
        LocationModel locationModel = new LocationModel();
        locationModel.setFrom(facilityB.getUri());
        locationModel.setTo(facilityC.getUri());

        LocationObservationModel locationObservationModel = new LocationObservationModel();
        locationObservationModel.setHasGeometry(true);
        locationObservationModel.setLocation(locationModel);
        locationModel.setX("800");
        locationModel.setY("800");
        locationModel.setZ("800");

        MoveCreationDTO moveDTO = getMoveDTOFilledAsIn1dot4();
        moveDTO.setLocation(LocationObservationDTO.getDTOFromModel(locationObservationModel));
        moveDTO.setUri(moveURI);

        new UserCallBuilder(create)
                .setBody(List.of(moveDTO))
                .buildAdmin()
                .executeCallAndAssertStatus("move creation with location should ignore deprecated properties and be created successfully", Response.Status.CREATED);

        MoveGetDTO returnedMove = getMoveByUris(List.of(moveURI)).get(0);
        assertEquals("move should save the 'X' property from location and not from deprecated property", "800", returnedMove.getLocation().getX());
        assertEquals("move should save the 'Y' property from location and not from deprecated property", "800", returnedMove.getLocation().getY());
        assertEquals("move should save the 'Z' property from location and not from deprecated property", "800", returnedMove.getLocation().getZ());
        assertTrue("move should save the 'from' property from location and not from deprecated property", SPARQLDeserializers.compareURIs(facilityB.getUri(), returnedMove.getLocation().getFrom()));
        assertTrue("move should save the 'to' property from location and not from deprecated property", SPARQLDeserializers.compareURIs(facilityC.getUri(), returnedMove.getLocation().getTo()));
        assertEquals("move should save the 'textualPosition' property from location and not from deprecated property", null, returnedMove.getLocation().getTextualPosition());
    }
    //#endregion

    //#region private methods
    private List<MoveDetailsDTO> getMoveByUris(List<URI> uris) throws Exception {
        return new UserCallBuilder(getByUriList)
                .setBody(uris)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<MoveDetailsDTO>>() {})
                .getDeserializedResponse()
                .getResult();
    }
    //#endregion

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
        return Collections.singletonList(LocationObservationDAO.LOCATION_COLLECTION_NAME);
    }
}