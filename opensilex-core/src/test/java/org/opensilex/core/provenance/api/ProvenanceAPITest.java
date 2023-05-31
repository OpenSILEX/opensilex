//******************************************************************************
//                          ProvenanceAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.device.api.DeviceCreationDTO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author Alice Boizet
 */
public class ProvenanceAPITest extends AbstractMongoIntegrationTest {
    
    public String path = "/core/provenances";

    public String uriPath = path + "/{uri}";
    public String searchPath = path;
    public String createPath = path;
    public String updatePath = path;
    public String deletePath = path + "/{uri}";
    
    public String devicePath = "/core/devices";
    private static URI deviceURI;
    private static final URI sensingDeviceType = URI.create(Oeso.SensingDevice.toString());
    private static final URI activityType = URI.create(Oeso.ImageAnalysis.toString());

    public DeviceCreationDTO getCreationDeviceDTO() {
        DeviceCreationDTO device = new DeviceCreationDTO();
        device.setName("sensor01");
        device.setType(sensingDeviceType);
        return device;
    }
    
    @Before
    public void beforeTest() throws Exception {
        
        if(deviceURI == null){ // create only once ( static )
            final Response postResultXP = getJsonPostResponseAsAdmin(target(devicePath), getCreationDeviceDTO());
            deviceURI = extractUriFromResponse(postResultXP);
        }
    }
    
    
    public ProvenanceCreationDTO getCreationProvDTO(URI activityType, URI agentType) {
        ProvenanceCreationDTO provDTO = new ProvenanceCreationDTO();
        provDTO.setName("label");
        provDTO.setDescription("comment");
        
        ActivityCreationDTO activity = new ActivityCreationDTO();
        activity.setRdfType(activityType);
        ArrayList<ActivityCreationDTO> activities = new ArrayList<>();
        activities.add(activity);
        provDTO.setActivity(activities);
        
        AgentModel agent = new AgentModel();
        agent.setRdfType(agentType);
        agent.setUri(deviceURI);

        Document settings = new Document();
        settings.put("param", "value");
        agent.setSettings(settings);

        ArrayList<AgentModel> agents = new ArrayList<>();
        agents.add(agent);
        provDTO.setAgents(agents);
        
        return provDTO;        
    }    
    
    public ProvenanceCreationDTO getCreationProvDTO() {
        return getCreationProvDTO(activityType, sensingDeviceType);
    }
    
    @Test
    public void create() throws Exception {
        final Response postResultProvenance = getJsonPostResponseAsAdmin(target(createPath), getCreationProvDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultProvenance.getStatus());
    }

    @Test
    public void Create_activity_type_doesnt_exists() throws Exception{
        ProvenanceCreationDTO prov = getCreationProvDTO(new URI(Oeso.Accession.toString()), sensingDeviceType);
        final Response postResult1 = getJsonPostResponseAsAdmin(target(createPath), prov);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult1.getStatus());
    }

    @Test
    public void create_with_sensor_bad_RDFType() throws Exception {
        ProvenanceCreationDTO provenanceDTO = getCreationProvDTO();
        provenanceDTO.getAgents().get(0).setRdfType( new URI(Oeso.Project.getURI()) );
        final Response postResultProvenance = getJsonPostResponseAsAdmin(target(createPath), provenanceDTO);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResultProvenance.getStatus());
    }

    @Test
    public void create_with_sensor_RDF_type_does_not_exists() throws Exception{
        ProvenanceCreationDTO prov2 = getCreationProvDTO(activityType, new URI(Oeso.Accession.toString()));
        final Response postResult2 = getJsonPostResponseAsAdmin(target(createPath), prov2);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult2.getStatus());
    }

    @Test
    public void create_with_sensor_which_does_not_exists_in_database() throws Exception {
        ProvenanceCreationDTO provenanceDTO = getCreationProvDTO();
        AgentModel operatorAgentModel = new AgentModel();
        URI fakeUri = new URI(deviceURI.toString()+"/this/uri/doesnt/exists");
        operatorAgentModel.setUri(fakeUri);
        operatorAgentModel.setRdfType( new URI(Oeso.SensingDevice.getURI()) );
        List<AgentModel> agents = new ArrayList<>();
        agents.add(operatorAgentModel);
        provenanceDTO.setAgents(agents);

        final Response postResultProvenance = getJsonPostResponseAsAdmin(target(createPath), provenanceDTO);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResultProvenance.getStatus());
    }

    @Test
    public void create_with_sensor_but_it_is_the_uri_of_a_person() throws Exception {
        PersonDAO personDAO = new PersonDAO(getSparqlService());
        PersonModel personModel = personDAO.create(null, "personne", "personne", null, null);

        ProvenanceCreationDTO provenanceDTO = getCreationProvDTO();
        AgentModel operatorAgentModel = new AgentModel();
        operatorAgentModel.setUri(personModel.getUri());
        operatorAgentModel.setRdfType( new URI(Oeso.SensingDevice.getURI()) );
        List<AgentModel> agents = new ArrayList<>();
        agents.add(operatorAgentModel);
        provenanceDTO.setAgents(agents);

        final Response postResultProvenance = getJsonPostResponseAsAdmin(target(createPath), provenanceDTO);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResultProvenance.getStatus());
    }

    @Test
    public void createWithOperator_butOperatorIsNotAPerson() throws Exception {
        final Response postResultProvenance = getJsonPostResponseAsAdmin(target(createPath), getCreationProvDTO(activityType, new URI(Oeso.Operator.getURI())));
        assertEquals("When inserting a provanance with an operator who is not a PersonModel, we are expecting to receive a BAD REQUEST response",
                Response.Status.BAD_REQUEST.getStatusCode(), postResultProvenance.getStatus());
    }

    @Test
    public void create_with_operator_which_does_not_exists_in_database() throws Exception {
        ProvenanceCreationDTO provenanceDTO = getCreationProvDTO();
        AgentModel operatorAgentModel = new AgentModel();
        URI fakeUri = new URI(deviceURI.toString()+"/this/uri/doesnt/exists");
        operatorAgentModel.setUri(fakeUri);
        operatorAgentModel.setRdfType( new URI(Oeso.Operator.getURI()) );
        List<AgentModel> agents = new ArrayList<>();
        agents.add(operatorAgentModel);
        provenanceDTO.setAgents(agents);

        final Response postResultProvenance = getJsonPostResponseAsAdmin(target(createPath), provenanceDTO);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResultProvenance.getStatus());
    }

    @Test
    public void createWithOperator() throws Exception {
        PersonDAO personDAO = new PersonDAO(getSparqlService());
        PersonModel operator = personDAO.create(null, "operator", "operator", null, null);

        ProvenanceCreationDTO provenanceDTO = getCreationProvDTO();
        AgentModel operatorAgentModel = new AgentModel();
        operatorAgentModel.setUri(operator.getUri());
        operatorAgentModel.setRdfType(new URI(Oeso.Operator.getURI()));
        List<AgentModel> agents = new ArrayList<>();
        agents.add(operatorAgentModel);
        provenanceDTO.setAgents(agents);

        final Response postResultProvenance = getJsonPostResponseAsAdmin(target(createPath), provenanceDTO);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultProvenance.getStatus());
    }

    @Test
    public void createWithOperator_badRDFType() throws Exception{
        PersonDAO personDAO = new PersonDAO(getSparqlService());
        PersonModel operator = personDAO.create(null, "operator", "operator", null, null);

        ProvenanceCreationDTO provenanceDTO = getCreationProvDTO();
        AgentModel operatorAgentModel = new AgentModel();
        operatorAgentModel.setUri(operator.getUri());
        operatorAgentModel.setRdfType(new URI(Oeso.Project.getURI()));
        List<AgentModel> agents = new ArrayList<>();
        agents.add(operatorAgentModel);
        provenanceDTO.setAgents(agents);

        final Response postResultProvenance = getJsonPostResponseAsAdmin(target(createPath), provenanceDTO);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResultProvenance.getStatus());
    }
    
    @Test
    public void testUpdate() throws Exception {

        // create the provenance
        ProvenanceCreationDTO dto = getCreationProvDTO();
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), dto);

        // update the provenance
        dto.setUri(extractUriFromResponse(postResult));
        dto.setDescription("new comment");
        
        // check update ok
        final Response updateResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new provenance and compare to the expected provenance
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), dto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ProvenanceGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ProvenanceGetDTO>>() {
        });
        ProvenanceGetDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(dto.getDescription(), dtoFromApi.getDescription());
    }

    @Test
    public void testUpdate_OperatorIsNotAPerson() throws Exception {

        ProvenanceCreationDTO provenanceDTO = getCreationProvDTO();
        final Response postResultProvenance = getJsonPostResponseAsAdmin(target(createPath), provenanceDTO);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultProvenance.getStatus());

        ProvenanceCreationDTO dtoWithBadOperator = getCreationProvDTO(activityType, new URI(Oeso.Operator.getURI()));
        dtoWithBadOperator.setUri(extractUriFromResponse(postResultProvenance));

        final Response updateResult = getJsonPutResponse(target(updatePath), dtoWithBadOperator);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateResult.getStatus());
    }
    
    @Test
    public void testDelete() throws Exception {

        // create object and check if URI exists
        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationProvDTO());
        String uri = extractUriFromResponse(postResponse).toString();

        // delete object and check if URI no longer exists
        Response delResult = getDeleteByUriResponse(target(deletePath), uri);
        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());

        Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testGetByUri() throws Exception {

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationProvDTO());
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ProvenanceGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ProvenanceGetDTO>>() {
        });
        ProvenanceGetDTO provGetDto = getResponse.getResult();
        assertNotNull(provGetDto);
    }
    
    @Test
    public void testSearch() throws Exception {

        ProvenanceCreationDTO creationDTO = getCreationProvDTO();
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), creationDTO);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("name", creationDTO.getName());
                put("description", creationDTO.getDescription());
                put("activity_type", creationDTO.getActivity().get(0).getRdfType());
                put("agent", creationDTO.getAgents().get(0).getUri());
            }
        };

        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 50, params);
        final Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<ProvenanceGetDTO> provListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ProvenanceGetDTO>>() {
        });
        List<ProvenanceGetDTO> provenances = provListResponse.getResult();

        assertFalse(provenances.isEmpty());
    }
}
