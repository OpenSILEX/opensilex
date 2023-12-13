/*******************************************************************************
 *                         DeviceAPITest.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.core.device.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.apache.jena.vocabulary.XSD;
import org.junit.Assert;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.data.api.DataAPI;
import org.opensilex.core.data.api.DataCreationDTO;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.dal.ProvEntityModel;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.geospatial.api.GeometryDTO;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.provenance.api.ProvenanceAPI;
import org.opensilex.core.provenance.api.ProvenanceCreationDTO;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.variable.api.VariableApiTest;
import org.opensilex.core.variable.api.VariableCreationDTO;
import org.opensilex.core.variable.dal.*;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.person.api.ORCIDClient;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.mail.internet.InternetAddress;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.BiPredicate;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;

/**
 * @author rcolin
 */
public class DeviceAPITest extends AbstractMongoIntegrationTest {

    public String path = DeviceAPI.PATH;
    public String getByUriPath = path + "/{uri}";
    public String createPath = path;
    public String updatePath = path;
    public String deletePath = path + "/{uri}";
    public String facilityPath = path + "/{uri}/facility";
    public String exportGeospatialPath = path + "/export_geospatial";

    public String provenancePath = ProvenanceAPI.PATH;
    public String dataPath = DataAPI.PATH;

    private static final URI sensingDeviceType = URI.create(Oeso.SensingDevice.toString());

    public DeviceCreationDTO getCreationDto() throws Exception {

        DeviceCreationDTO dto = new DeviceCreationDTO();
        dto.setType(sensingDeviceType);
        dto.setName("name");

        dto.setConstructorModel("constructor");
        dto.setSerialNumber("serialNumber");
        dto.setBrand("brand");

        dto.setStartUp(LocalDate.now());
        dto.setRemoval(dto.getStartUp().plusYears(1));
        dto.setDescription("description");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("prop1", "value1");
        metadata.put("prop2", "value1");
        dto.setMetadata(metadata);

        return dto;
    }

    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateGetAndDelete(createPath, getByUriPath, deletePath, getCreationDto());
    }

    @Test
    public void create_with_personInCharge() throws Exception {
        PersonDAO personDAO = new PersonDAO(getSparqlService());

        PersonModel personModel = new PersonModel();
        personModel.setFirstName("test");
        personModel.setLastName("test");
        personModel.setEmail(new InternetAddress("test@test.test"));
        URI personURI = personDAO.create(personModel, new ORCIDClient()).getUri();

        DeviceCreationDTO deviceDTO = getCreationDto();
        deviceDTO.setPersonInChargeURI(personURI);

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), deviceDTO);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void create_with_personInCharge_doesnt_exist() throws Exception {
        DeviceCreationDTO deviceDTO = getCreationDto();
        deviceDTO.setPersonInChargeURI(new URI("http://fake.uri/personInCharge"));

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), deviceDTO);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void create_with_personInCharge_exist_but_is_not_a_person() throws Exception {
        AccountDAO accountDAO = new AccountDAO(getSparqlService());
        URI accountURI = accountDAO.create(null, new InternetAddress("test@test.test"), false, "password", "fr").getUri();

        DeviceCreationDTO deviceDTO = getCreationDto();
        deviceDTO.setPersonInChargeURI(accountURI);

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), deviceDTO);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void testGetByUriWithUnknownUri() throws Exception {
        Response getResult = getJsonGetByUriResponseAsAdmin(target(getByUriPath), Oeso.Device + "/unknown_uri");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testGetByUri() throws Exception {

        // Try to insert an Entity, to fetch it and to get fields
        DeviceCreationDTO creationDTO = getCreationDto();

        Response postResult = getJsonPostResponseAsAdmin(target(createPath), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Response getResult = getJsonGetByUriResponseAsAdmin(target(getByUriPath), uri.toString());

        // try to deserialize object and check if the fields value are the same
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<DeviceGetDetailsDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<DeviceGetDetailsDTO>>() {
        });
        DeviceGetDetailsDTO dtoFromDb = getResponse.getResult();
        assertNotNull(dtoFromDb);

        assertEquals(creationDTO.getName(), dtoFromDb.getName());
        assertEquals(creationDTO.getDescription(), dtoFromDb.getDescription());
        assertEquals(creationDTO.getBrand(), dtoFromDb.getBrand());
        assertEquals(creationDTO.getSerialNumber(), dtoFromDb.getSerialNumber());
        assertEquals(creationDTO.getConstructorModel(), dtoFromDb.getConstructorModel());
        assertEquals(creationDTO.getStartUp(), dtoFromDb.getStartUp());
        assertEquals(creationDTO.getRemoval(), dtoFromDb.getRemoval());
        assertEquals(creationDTO.getMetadata(), dtoFromDb.getMetadata());

        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getType(), dtoFromDb.getType()));
    }

    private VariableModel getVariable(String name) throws Exception {
        EntityModel entity = new EntityModel();
        entity.setName("entity");

        CharacteristicModel characteristic = new CharacteristicModel();
        characteristic.setName("characteristic");

        MethodModel method = new MethodModel();
        method.setName("method");

        UnitModel unit = new UnitModel();
        unit.setName("unit");

        SPARQLService sparql = getSparqlService();
        sparql.create(entity);
        sparql.create(characteristic);
        sparql.create(method);
        sparql.create(unit);

        VariableModel model = new VariableModel();
        model.setEntity(entity);
        model.setCharacteristic(characteristic);
        model.setMethod(method);
        model.setUnit(unit);
        model.setDataType(new URI(XSD.integer.getURI()));
        model.setName(name);
        return model;
    }

    @Test
    public void testDeleteFailWithLinkedData() throws Exception {

        // create device
        DeviceCreationDTO deviceDto = getCreationDto();
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), deviceDto);
        deviceDto.setUri(extractUriFromResponse(postResult));

        // create provenance
        ProvenanceCreationDTO provDto = new ProvenanceCreationDTO();
        provDto.setName("prov");
        provDto.setDescription("prov");
        Response postProvResult = getJsonPostResponseAsAdmin(target(provenancePath),provDto);
        provDto.setUri(extractUriFromResponse(postProvResult));

        // create data with a DataProvenance associated to device
        DataProvenanceModel dataProv = new DataProvenanceModel();
        dataProv.setUri(provDto.getUri());

        // link data to device
        ProvEntityModel provEntity = new ProvEntityModel();
        provEntity.setType(sensingDeviceType);
        provEntity.setUri(deviceDto.getUri());
        dataProv.setProvWasAssociatedWith(Collections.singletonList(provEntity));

        // create Variable
        VariableModel variable = getVariable("variable");
        getSparqlService().create(variable);

        DataCreationDTO dataDto = new DataCreationDTO();
        dataDto.setDate(Instant.now().toString());
        dataDto.setValue(8611);
        dataDto.setProvenance(dataProv);
        dataDto.setVariable(variable.getUri());
        dataDto.setUri(URI.create("dev:data1"));

        Response postDataProvResult = getJsonPostResponseAsAdmin(target(dataPath), Collections.singletonList(dataDto));
        assertEquals(Response.Status.CREATED.getStatusCode(),postDataProvResult.getStatus());

        // try to delete device -> expect error 404 with result.title = LINKED_DEVICE_ERROR
        final Response delResult = getDeleteByUriResponse(target(deletePath), deviceDto.getUri().toString());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),delResult.getStatus());

        JsonNode node = delResult.readEntity(JsonNode.class);
        ErrorResponse getResponse = mapper.convertValue(node, new TypeReference<ErrorResponse>() {});
        assertEquals(DeviceAPI.LINKED_DEVICE_ERROR,getResponse.getResult().title);
        assertTrue(getResponse.getResult().message.contains("data"));
    }

    @Test
    public void testDeleteFailWithLinkedProvenance() throws Exception {

        // create device
        DeviceCreationDTO deviceDto = getCreationDto();
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), deviceDto);
        deviceDto.setUri(extractUriFromResponse(postResult));

        // create provenance associated to device
        ProvenanceCreationDTO provDto = new ProvenanceCreationDTO();
        provDto.setName("prov");
        provDto.setDescription("prov");

        AgentModel agent = new AgentModel();
        agent.setRdfType(sensingDeviceType);
        agent.setUri(deviceDto.getUri());
        provDto.setAgents(Collections.singletonList(agent));

        Response postProvResult = getJsonPostResponseAsAdmin(target(provenancePath),provDto);
        assertEquals(Response.Status.CREATED.getStatusCode(),postProvResult.getStatus());
        provDto.setUri(extractUriFromResponse(postProvResult));

        // try to delete device -> expect error 404 with result.title = LINKED_DEVICE_ERROR
        final Response delResult = getDeleteByUriResponse(target(deletePath), deviceDto.getUri().toString());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),delResult.getStatus());

        JsonNode node = delResult.readEntity(JsonNode.class);
        ErrorResponse getResponse = mapper.convertValue(node, new TypeReference<ErrorResponse>() {});
        assertEquals(DeviceAPI.LINKED_DEVICE_ERROR,getResponse.getResult().title);
        assertTrue(getResponse.getResult().message.contains("provenance"));
    }

    @Test
    public void testLinkWithVariablesOK() throws Exception {

        // create variable and relation
        VariableApiTest variableApiTest = new VariableApiTest();
        VariableCreationDTO linkedVariable = variableApiTest.getCreationDto();
        Response postVariableResponse = getJsonPostResponseAsAdmin(target(variableApiTest.createPath), variableApiTest.getCreationDto());
        linkedVariable.setUri(extractUriFromResponse(postVariableResponse));

        RDFObjectRelationDTO measuresRelation = new RDFObjectRelationDTO();
        measuresRelation.setProperty(URI.create(Oeso.measures.toString()));
        measuresRelation.setValue(linkedVariable.getUri().toString());

        // create sensing device and link with variable
        DeviceCreationDTO sensingDeviceDto = getCreationDto();
        sensingDeviceDto.setName("sensing_device");
        sensingDeviceDto.setType(URI.create(Oeso.SensingDevice.getURI()));
        sensingDeviceDto.setRelations(Collections.singletonList(measuresRelation));

        Response postSensingDeviceResponse = getJsonPostResponseAsAdmin(target(createPath),sensingDeviceDto);
        assertEquals(Response.Status.CREATED.getStatusCode(), postSensingDeviceResponse.getStatus());
        sensingDeviceDto.setUri(extractUriFromResponse(postSensingDeviceResponse));

        // create software and link with variable
        DeviceCreationDTO softwareDto = getCreationDto();
        softwareDto.setType(URI.create(Oeso.Software.getURI()));
        softwareDto.setName("software");
        softwareDto.setRelations(Collections.singletonList(measuresRelation));

        Response postSoftwareResponse = getJsonPostResponseAsAdmin(target(createPath),softwareDto);
        assertEquals(Response.Status.CREATED.getStatusCode(), postSoftwareResponse.getStatus());
        softwareDto.setUri(extractUriFromResponse(postSoftwareResponse));

        // test retrieval with relations
        BiPredicate<RDFObjectRelationDTO, DeviceGetDetailsDTO> findRelationPredicate = (relation, device) ->
                SPARQLDeserializers.compareURIs(relation.getProperty().toString(), Oeso.measures.toString()) &&
                SPARQLDeserializers.compareURIs(relation.getValue(), linkedVariable.getUri().toString()
        );

        // test on sensing device
        Response getSensingDeviceResponse = getJsonGetByUriResponseAsAdmin(target(getByUriPath), sensingDeviceDto.getUri().toString());
        SingleObjectResponse<DeviceGetDetailsDTO> getResponse = mapper.convertValue(
                getSensingDeviceResponse.readEntity(JsonNode.class),
                new TypeReference<SingleObjectResponse<DeviceGetDetailsDTO>>() {}
        );
        DeviceGetDetailsDTO sensingDeviceGetDTO = getResponse.getResult();
        Assert.assertTrue(sensingDeviceGetDTO.getRelations().stream().anyMatch(relation -> findRelationPredicate.test(relation, sensingDeviceGetDTO)));

        // test on software
        Response getSoftwareDeviceResponse = getJsonGetByUriResponseAsAdmin(target(getByUriPath), softwareDto.getUri().toString());
        getResponse = mapper.convertValue(
                getSoftwareDeviceResponse.readEntity(JsonNode.class),
                new TypeReference<SingleObjectResponse<DeviceGetDetailsDTO>>() {}
        );
        DeviceGetDetailsDTO softwareGetDTO = getResponse.getResult();
        Assert.assertTrue(softwareGetDTO.getRelations().stream().anyMatch(relation -> findRelationPredicate.test(relation, softwareGetDTO)));
    }

    private FacilityModel createFacility(URI uri, String name) throws Exception {
        FacilityModel facilityModel = new FacilityModel();
        facilityModel.setUri(uri);
        facilityModel.setName(name);
        getSparqlService().create(facilityModel);
        return facilityModel;
    }

    private MoveModel createMove(URI device, FacilityModel fromFacility, FacilityModel toFacility, String end) throws Exception {
        MoveModel moveModel = new MoveModel();
        moveModel.setType(URI.create(Oeev.Move.getURI()));
        moveModel.setTargets(Arrays.asList(device));
        moveModel.setFrom(fromFacility);
        moveModel.setTo(toFacility);
        moveModel.setIsInstant(true);
        InstantModel endInstant = new InstantModel();
        endInstant.setDateTimeStamp(OffsetDateTime.parse(end));
        moveModel.setEnd(endInstant);
        getSparqlService().create(moveModel);

        return moveModel;
    }

    @Test
    public void testGetAssociatedFacilityOK() throws Exception {
        // create device
        DeviceCreationDTO deviceDto = getCreationDto();
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), deviceDto);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());
        deviceDto.setUri(extractUriFromResponse(postResult));

        // create facilities
        FacilityModel facilityA = createFacility(new URI("test:id/facilityA"), "facility A");
        FacilityModel facilityB = createFacility(new URI("test:id/facilityB"), "facility B");

        // create move event
        MoveModel moveModel = createMove(deviceDto.getUri(), facilityA, facilityB, OffsetDateTime.now().toString());

        // test get associated facility
        Response getResult = getJsonGetByUriResponseAsAdmin(target(facilityPath), facilityB.getUri().toString());
        JsonNode node = getResult.readEntity(JsonNode.class);

        PaginatedListResponse<DeviceGetDTO> getResponse = mapper.convertValue(
                node,
                new TypeReference<PaginatedListResponse<DeviceGetDTO>>() {}
        );
        List<DeviceGetDTO> deviceDtoList = getResponse.getResult();
        assertEquals(deviceDtoList.size(), 1);
        assertEquals(URIDeserializer.getExpandedURI(deviceDtoList.get(0).getUri().toString()), deviceDto.getUri().toString());

        // create new move
        moveModel = createMove(deviceDto.getUri(), facilityB, facilityA, OffsetDateTime.now().plusDays(1).toString());

        // test get associated facility for new move
        getResult = getJsonGetByUriResponseAsAdmin(target(facilityPath), facilityA.getUri().toString());
        node = getResult.readEntity(JsonNode.class);

        getResponse = mapper.convertValue(
                node,
                new TypeReference<PaginatedListResponse<DeviceGetDTO>>() {}
        );
        deviceDtoList = getResponse.getResult();
        assertEquals(deviceDtoList.size(), 1);
        assertEquals(URIDeserializer.getExpandedURI(deviceDtoList.get(0).getUri().toString()), deviceDto.getUri().toString());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Arrays.asList(VariableModel.class,DeviceModel.class);
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return Arrays.asList(
                DeviceDAO.ATTRIBUTES_COLLECTION_NAME,
                ProvenanceDAO.PROVENANCE_COLLECTION_NAME,
                DataDAO.DATA_COLLECTION_NAME,
                DataDAO.FILE_COLLECTION_NAME
        );
    }

    @Test
    public void testExportDevicesAsShpandGeoJson() throws Exception {

        //Create one Device Model
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDto());
        URI uri = extractUriFromResponse(postResult);

        DeviceModel deviceModel = new DeviceModel();

        deviceModel.setName("DeviceExported");
        deviceModel.setUri(uri);
        deviceModel.setType(sensingDeviceType);
        deviceModel.setTypeLabel(new SPARQLLabel("Device","en"));

        deviceModel.setBrand("Roald_Dalh");
        deviceModel.setModel("Willy_Wonka");
        deviceModel.setStartUp(LocalDate.now());

        //build geometry
        Geometry geometry = new Point(new Position(3.97167246, 43.61328981));

        GeometryDTO objToExport=new GeometryDTO();
        objToExport.setGeometry(geometryToGeoJson(geometry));
        objToExport.setUri(deviceModel.getUri());

        ArrayList<GeometryDTO> objectsList= new ArrayList<>();
        objectsList.add(objToExport);

        //build params
        ArrayList<URI> propsList= new ArrayList<>();
        propsList.add(new URI(SPARQLDeserializers.getShortURI(Oeso.hasGeometry.getURI())));
        propsList.add(new URI("vocabulary:hasModel"));
        propsList.add(new URI("vocabulary:startUp"));
        propsList.add(new URI("vocabulary:hasBrand"));

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