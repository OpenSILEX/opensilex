// //******************************************************************************
// //                          ProcessAPITest.java
// // OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// // Copyright © INRA 2019
// // Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
// //******************************************************************************
// package org.opensilex.process.process.api;

// import com.fasterxml.jackson.core.type.TypeReference;
// import com.fasterxml.jackson.databind.JsonNode;
// import org.junit.Test;
// import org.opensilex.server.response.ObjectUriResponse;
// import org.opensilex.server.response.PaginatedListResponse;
// import org.opensilex.server.response.SingleObjectResponse;
// import org.junit.Before;

// import javax.ws.rs.client.WebTarget;
// import javax.ws.rs.core.Response;
// import javax.ws.rs.core.Response.Status;
// import java.net.URI;
// import java.time.OffsetDateTime;
// import java.util.*;

// import static junit.framework.TestCase.assertEquals;
// import static org.junit.Assert.assertFalse;
// import static org.junit.Assert.assertNotNull;
// import org.opensilex.process.process.dal.ProcessModel;
// import org.opensilex.process.process.dal.StepModel;
// import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
// import org.opensilex.sparql.model.SPARQLResourceModel;

// /**
//  * @author Emilie Fernandez
//  */
// public class ProcessAPITest extends AbstractSecurityIntegrationTest {

//     protected static String path = "/core/process";

//     public static String uriPath = path + "/{uri}";
//     public static String searchPath = path ;
//     public static String createPath = path;
//     public static String updatePath = path ;
//     public static String deletePath = path + "/{uri}";

//     private static StepModel step;

//     @Before
//     public void createStep() throws Exception {
//         step = new StepModel();
//         step.setUri(new URI("test:step"));
//         step.setName("step");

//         getSparqlService().create(step);
//     }

//     public static ProcessCreationDTO getCreationDTO() {

//         ProcessCreationDTO processDto = new ProcessCreationDTO();
//         processDto.setName("process 1");
//         processDto.setStart(OffsetDateTime.now().toString());
//         List<URI> stepUris = new ArrayList<>();
//         stepUris.add(step.getUri());
//         processDto.setStep(stepUris);
//         return processDto;
//     }

//     @Test
//     public void testCreate() throws Exception {

//         final Response postResult = getJsonPostResponse(target(createPath), getCreationDTO());
//         assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

//         // ensure that the result is a well formed URI, else throw exception
//         URI createdUri = extractUriFromResponse(postResult);
//         final Response getResult = getJsonGetByUriResponse(target(uriPath), createdUri.toString());
//         assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
//     }

//     @Test
//     public void testUpdate() throws Exception {

//         ProcessCreationDTO processDto = getCreationDTO();
//         final Response postResult = getJsonPostResponse(target(createPath), processDto);

//         processDto.setUri(extractUriFromResponse(postResult));
//         processDto.setName("new name");
//         processDto.setStart(OffsetDateTime.now().plusDays(1).toString());

//         final Response updateResult = getJsonPutResponse(target(updatePath), processDto);
//         assertEquals(Status.OK.getStatusCode(), updateResult.getStatus());

//         // retrieve the new process and compare to the expected process
//         final Response getResult = getJsonGetByUriResponse(target(uriPath), processDto.getUri().toString());

//         // try to deserialize object
//         JsonNode node = getResult.readEntity(JsonNode.class);
//         SingleObjectResponse<ProcessGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ProcessGetDTO>>() {
//         });
//         ProcessGetDTO dtoFromApi = getResponse.getResult();

//         // check that the object has been updated
//         assertEquals(processDto.getName(), dtoFromApi.getName());
//         assertEquals(processDto.getStart(), dtoFromApi.getStart());
//     }

//     @Test
//     public void testDelete() throws Exception {

//         // create object and check if URI exists
//         Response postResponse = getJsonPostResponse(target(createPath), getCreationDTO());
//         String uri = extractUriFromResponse(postResponse).toString();

//         // delete object and check if URI no longer exists
//         Response delResult = getDeleteByUriResponse(target(deletePath), uri);
//         assertEquals(Status.OK.getStatusCode(), delResult.getStatus());

//         Response getResult = getJsonGetByUriResponse(target(uriPath), uri);
//         assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
//     }

//     @Test
//     public void testGetByUri() throws Exception {

//         final Response postResult = getJsonPostResponse(target(createPath), getCreationDTO());
//         URI uri = extractUriFromResponse(postResult);

//         final Response getResult = getJsonGetByUriResponse(target(uriPath), uri.toString());
//         assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

//         // try to deserialize object
//         JsonNode node = getResult.readEntity(JsonNode.class);
//         SingleObjectResponse<ProcessGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ProcessGetDTO>>() {
//         });
//         ProcessGetDTO processDto = getResponse.getResult();
//         assertNotNull(processDto);
//     }

//     @Test
//     public void testGetByUriFail() throws Exception {

//         final Response postResult = getJsonPostResponse(target(createPath), getCreationDTO());
//         JsonNode node = postResult.readEntity(JsonNode.class);
//         ObjectUriResponse postResponse = mapper.convertValue(node, ObjectUriResponse.class);
//         String uri = postResponse.getResult();

//         // call the service with a non existing pseudo random URI
//         final Response getResult = getJsonGetByUriResponse(target(uriPath), uri + "testFail");
//         assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
//     }

    

//     @Test
//     public void testSearch() throws Exception {

//         ProcessCreationDTO creationDTO = getCreationDTO();
//         final Response postResult = getJsonPostResponse(target(createPath), creationDTO);
//         URI uri = extractUriFromResponse(postResult);

//         Map<String, Object> params = new HashMap<String, Object>() {
//             {
//                 put("start", creationDTO.getStart());
//                 put("name", creationDTO.getName());
//                 put("uri", uri);
//             }
//         };

//         WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 50, params);
//         final Response getResult = appendToken(searchTarget).get();
//         assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

//         JsonNode node = getResult.readEntity(JsonNode.class);
//         PaginatedListResponse<ProcessGetDTO> processListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ProcessGetDTO>>() {
//         });
//         List<ProcessGetDTO> processDto = processListResponse.getResult();

//         assertFalse(processDto.isEmpty());
//     }
    

//     @Override
//     protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
//         return Collections.singletonList(ProcessModel.class);
//     }
// }
