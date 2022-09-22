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
import org.apache.jena.vocabulary.XSD;
import org.junit.Assert;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.CoreModule;
import org.opensilex.core.data.api.DataAPI;
import org.opensilex.core.data.api.DataCreationDTO;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.dal.ProvEntityModel;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.provenance.api.ProvenanceAPI;
import org.opensilex.core.provenance.api.ProvenanceCreationDTO;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.variable.api.VariableApiTest;
import org.opensilex.core.variable.api.VariableCreationDTO;
import org.opensilex.core.variable.dal.*;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiPredicate;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * @author rcolin
 */
public class DeviceAPITest extends AbstractMongoIntegrationTest {

    public String path = DeviceAPI.PATH;
    public String getByUriPath = path + "/{uri}";
    public String createPath = path;
    public String updatePath = path;
    public String deletePath = path + "/{uri}";

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
    public void testGetByUriWithUnknownUri() throws Exception {
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), Oeso.Device + "/unknown_uri");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testGetByUri() throws Exception {

        // Try to insert an Entity, to fetch it and to get fields
        DeviceCreationDTO creationDTO = getCreationDto();

        Response postResult = getJsonPostResponse(target(createPath), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Response getResult = getJsonGetByUriResponse(target(getByUriPath), uri.toString());

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
        Response postResult = getJsonPostResponse(target(createPath), deviceDto);
        deviceDto.setUri(extractUriFromResponse(postResult));

        // create provenance
        ProvenanceCreationDTO provDto = new ProvenanceCreationDTO();
        provDto.setName("prov");
        provDto.setDescription("prov");
        Response postProvResult = getJsonPostResponse(target(provenancePath),provDto);
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

        Response postDataProvResult = getJsonPostResponse(target(dataPath), Collections.singletonList(dataDto));
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
        Response postResult = getJsonPostResponse(target(createPath), deviceDto);
        deviceDto.setUri(extractUriFromResponse(postResult));

        // create provenance associated to device
        ProvenanceCreationDTO provDto = new ProvenanceCreationDTO();
        provDto.setName("prov");
        provDto.setDescription("prov");

        AgentModel agent = new AgentModel();
        agent.setRdfType(sensingDeviceType);
        agent.setUri(deviceDto.getUri());
        provDto.setAgents(Collections.singletonList(agent));

        Response postProvResult = getJsonPostResponse(target(provenancePath),provDto);
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
        Response postVariableResponse = getJsonPostResponse(target(variableApiTest.createPath), variableApiTest.getCreationDto());
        linkedVariable.setUri(extractUriFromResponse(postVariableResponse));

        RDFObjectRelationDTO measuresRelation = new RDFObjectRelationDTO();
        measuresRelation.setProperty(URI.create(Oeso.measures.toString()));
        measuresRelation.setValue(linkedVariable.getUri().toString());

        // create sensing device and link with variable
        DeviceCreationDTO sensingDeviceDto = getCreationDto();
        sensingDeviceDto.setName("sensing_device");
        sensingDeviceDto.setType(URI.create(Oeso.SensingDevice.getURI()));
        sensingDeviceDto.setRelations(Collections.singletonList(measuresRelation));

        Response postSensingDeviceResponse = getJsonPostResponse(target(createPath),sensingDeviceDto);
        assertEquals(Response.Status.CREATED.getStatusCode(), postSensingDeviceResponse.getStatus());
        sensingDeviceDto.setUri(extractUriFromResponse(postSensingDeviceResponse));

        // create software and link with variable
        DeviceCreationDTO softwareDto = getCreationDto();
        softwareDto.setType(URI.create(Oeso.Software.getURI()));
        softwareDto.setName("software");
        softwareDto.setRelations(Collections.singletonList(measuresRelation));

        Response postSoftwareResponse = getJsonPostResponse(target(createPath),softwareDto);
        assertEquals(Response.Status.CREATED.getStatusCode(), postSoftwareResponse.getStatus());
        softwareDto.setUri(extractUriFromResponse(postSoftwareResponse));

        // test retrieval with relations
        BiPredicate<RDFObjectRelationDTO, DeviceGetDetailsDTO> findRelationPredicate = (relation, device) ->
                SPARQLDeserializers.compareURIs(relation.getProperty().toString(), Oeso.measures.toString()) &&
                SPARQLDeserializers.compareURIs(relation.getValue(), linkedVariable.getUri().toString()
        );

        // test on sensing device
        Response getSensingDeviceResponse = getJsonGetByUriResponse(target(getByUriPath), sensingDeviceDto.getUri().toString());
        SingleObjectResponse<DeviceGetDetailsDTO> getResponse = mapper.convertValue(
                getSensingDeviceResponse.readEntity(JsonNode.class),
                new TypeReference<SingleObjectResponse<DeviceGetDetailsDTO>>() {}
        );
        DeviceGetDetailsDTO sensingDeviceGetDTO = getResponse.getResult();
        Assert.assertTrue(sensingDeviceGetDTO.getRelations().stream().anyMatch(relation -> findRelationPredicate.test(relation, sensingDeviceGetDTO)));

        // test on software
        Response getSoftwareDeviceResponse = getJsonGetByUriResponse(target(getByUriPath), softwareDto.getUri().toString());
        getResponse = mapper.convertValue(
                getSoftwareDeviceResponse.readEntity(JsonNode.class),
                new TypeReference<SingleObjectResponse<DeviceGetDetailsDTO>>() {}
        );
        DeviceGetDetailsDTO softwareGetDTO = getResponse.getResult();
        Assert.assertTrue(softwareGetDTO.getRelations().stream().anyMatch(relation -> findRelationPredicate.test(relation, softwareGetDTO)));
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

}