/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.provenance.api;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;
import static junit.framework.TestCase.assertEquals;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.integration.test.IntegrationTestCategory;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.nosql.DataNucleusServiceTest;

/**
 *
 * @author boizetal
 */
@Category(IntegrationTestCategory.class)
public class ProvenanceAPITest extends AbstractSecurityIntegrationTest {
    
    protected String path = "/core/provenance";

    protected String uriPath = path + "/get/{uri}";
    protected String searchPath = path + "/search";
    protected String createPath = path + "/create";
    protected String updatePath = path + "/update";
    protected String deletePath = path + "/delete/{uri}";
    
    protected ProvenanceCreationDTO getCreationProvDTO() {
        ProvenanceCreationDTO provDTO = new ProvenanceCreationDTO();
        provDTO.setLabel("label");
        provDTO.setComment("comment");
        AgentModel agent = new AgentModel();
        Map sensorMap = new HashMap();
//        sensorMap.put("http://www.phenome-fppn.fr/m3p/arch/2018/ac180011","settings");
//        agent.setSensingDevice(sensorMap);
//        provDTO.setAgent(agent);
        
        return provDTO;
        
    }
    
    
    @Test
    public void testCreate() throws Exception {
        
        // create provenance
        final Response postResultProvenance = getJsonPostResponse(target(createPath), getCreationProvDTO());
        LOGGER.info(postResultProvenance.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultProvenance.getStatus());        
    }
    
}
