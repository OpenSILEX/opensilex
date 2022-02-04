//******************************************************************************
//                          DataFileAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.data.api.DataCreationDTO;
import org.opensilex.core.data.api.DataGetDTO;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.provenance.api.ProvenanceAPITest;
import org.opensilex.core.provenance.api.ProvenanceCreationDTO;
import org.opensilex.core.scientificObject.api.ScientificObjectAPITest;
import org.opensilex.core.variable.api.VariableApiTest;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;

/**
 *
 * @author Alice Boizet
 */
public class DataAPITest extends AbstractMongoIntegrationTest {
    protected String path = "/core/data";

    protected String uriPath = path + "/{uri}";
    protected String searchPath = path;
    protected String createPath = path;
    protected String createListPath = path;
    protected String updatePath = path;
    protected String deletePath = path + "/{uri}";
    
    private URI variable;
    private DataProvenanceModel provenance;
    private List<URI> scientificObjects;    
    
    @Before
    public void beforeTest() throws Exception {
        //create experiment
        ExperimentAPITest expAPI = new ExperimentAPITest();
        Response postResultExp = getJsonPostResponse(target(expAPI.createPath), expAPI.getCreationDTO());
        List<URI> experiments = new ArrayList<>();
        experiments.add(extractUriFromResponse(postResultExp));
        
        //create provenance
        ProvenanceAPITest provAPI = new ProvenanceAPITest();
        ProvenanceCreationDTO prov = new ProvenanceCreationDTO();
        prov.setName("name");
        Response postResultProv = getJsonPostResponse(target(provAPI.createPath), prov);
        
        provenance = new DataProvenanceModel();
        provenance.setUri(extractUriFromResponse(postResultProv));   
        provenance.setExperiments(experiments);
        
        //create Variable
        VariableApiTest varAPI = new VariableApiTest();
        Response postResultVar = getJsonPostResponse(target(varAPI.createPath), varAPI.getCreationDto());
        variable = extractUriFromResponse(postResultVar);
        
        //create scientific object
        ScientificObjectAPITest soAPI = new ScientificObjectAPITest();
        Response postResultSO = getJsonPostResponse(target(ScientificObjectAPITest.createPath), soAPI.getCreationDTO(false));
        scientificObjects = new ArrayList<>();
        scientificObjects.add(extractUriFromResponse(postResultSO)); 
        
    }
    
    public DataCreationDTO getCreationDataDTO(String date) throws URISyntaxException, Exception {

        DataCreationDTO dataDTO = new DataCreationDTO();
        
        dataDTO.setProvenance(provenance);
        dataDTO.setVariable(variable);
        dataDTO.setTarget(scientificObjects.get(0));
        dataDTO.setValue(5.56);
        dataDTO.setDate(date);
                
        return dataDTO;        
    }    
       
    @Test
    public void testCreate() throws Exception {        
        ArrayList<DataCreationDTO> dtoList = new ArrayList<>();
        dtoList.add(getCreationDataDTO("2020-10-11T10:29:06.402+0200"));
        final Response postResultData = getJsonPostResponse(target(createListPath), dtoList);
        LOGGER.info(postResultData.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());        
    }
    
    @Test
    public void testUpdate() throws Exception {
        ArrayList<DataCreationDTO> dtoList = new ArrayList<>();
        DataCreationDTO dto = getCreationDataDTO("2020-10-12T10:29:06.402+0200");
        dtoList.add(dto);
        final Response postResult = getJsonPostResponse(target(createPath), dtoList);

        dto.setUri(extractUriFromResponse(postResult));
        dto.setValue(10.2);
        
        // check update ok
        final Response updateResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());

        final Response getResult = getJsonGetByUriResponse(target(uriPath), dto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<DataGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<DataGetDTO>>() {
        });
        DataGetDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(dto.getValue(), dtoFromApi.getValue());
    }
    
    @Test
    public void testDelete() throws Exception {

        // create object and check if URI exists
        ArrayList<DataCreationDTO> dtoList = new ArrayList<>();
        dtoList.add(getCreationDataDTO("2020-10-13T10:29:06.402+0200"));
        Response postResponse = getJsonPostResponse(target(createPath), dtoList);
        String uri = extractUriFromResponse(postResponse).toString();

        // delete object and check if URI no longer exists
        Response delResult = getDeleteByUriResponse(target(deletePath), uri);
        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());

        Response getResult = getJsonGetByUriResponse(target(uriPath), uri);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testGetByUri() throws Exception {
        ArrayList<DataCreationDTO> dtoList = new ArrayList<>();
        dtoList.add(getCreationDataDTO("2020-10-14T10:29:06.402+0200"));
        final Response postResult = getJsonPostResponse(target(createPath), dtoList);
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponse(target(uriPath), uri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<DataGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<DataGetDTO>>() {
        });
        DataGetDTO dataGetDto = getResponse.getResult();
        assertNotNull(dataGetDto);
    }
    
    @Test
    public void testSearch() throws Exception {
        ArrayList<DataCreationDTO> dtoList = new ArrayList<>();
        DataCreationDTO creationDTO = getCreationDataDTO("2020-06-15T10:29:06.402+0200");
        dtoList.add(creationDTO);
        final Response postResult = getJsonPostResponse(target(createPath), dtoList);
        URI uri = extractUriFromResponse(postResult);
        
        List<URI> provenances = new ArrayList();
        provenances.add(creationDTO.getProvenance().getUri());
        List<URI> variables = new ArrayList();
        variables.add(creationDTO.getVariable());

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("start_date", "2020-06-01");
                put("end_date", "2020-06-30");
                put("provenances", provenances);
                put("variables", variables);
                put("order_by", "date=desc");
            }
        };

        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 20, params);
        final Response getResult = appendToken(searchTarget).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<DataGetDTO> dataListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<DataGetDTO>>() {
        });
        List<DataGetDTO> datas = dataListResponse.getResult();

        assertFalse(datas.isEmpty());
    }
    
}
