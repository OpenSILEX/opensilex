//******************************************************************************
//                          ExperimentAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.scientificObject.api;

import org.opensilex.core.infrastructure.api.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import org.junit.Test;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.response.ResourceTreeResponse;

/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */
public class ScientificObjectAPITest extends AbstractSecurityIntegrationTest {

    protected String path = "/core/scientific-object";

    protected String uriPath = path + "/get/{uri}";
    protected String searchPath = path + "/search";
    protected String createPath = path + "/create";
    protected String updatePath = path + "/update";
    protected String deletePath = path + "/delete/{uri}";

//    protected ExperimentCreationDTO getCreationDTO() {
//       // TODO
//    }
//
//    @Test
//    public void testCreate() throws Exception {
//
//        final Response postResult = getJsonPostResponse(target(createPath), getCreationDTO());
//        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
//
//        // ensure that the result is a well formed URI, else throw exception
//        URI createdUri = extractUriFromResponse(postResult);
//        final Response getResult = getJsonGetByUriResponse(target(uriPath), createdUri.toString());
//        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
//    }
}
