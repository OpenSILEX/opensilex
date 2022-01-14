// //******************************************************************************
// //                          StepAPITest.java
// // OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// // Copyright © INRA 2019
// // Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
// //******************************************************************************
// package org.opensilex.process.process.step.api;

// import com.fasterxml.jackson.core.type.TypeReference;
// import com.fasterxml.jackson.databind.JsonNode;
// import org.junit.Test;
// import org.opensilex.server.response.ObjectUriResponse;
// import org.opensilex.server.response.PaginatedListResponse;
// import org.opensilex.server.response.SingleObjectResponse;

// import javax.ws.rs.client.WebTarget;
// import javax.ws.rs.core.Response;
// import javax.ws.rs.core.Response.Status;
// import java.net.URI;
// import java.time.LocalDate;
// import java.util.*;
// import org.junit.Before;
// import org.junit.After;
// import org.apache.jena.graph.Node;
// import org.apache.jena.graph.NodeFactory;
// import static junit.framework.TestCase.assertEquals;
// import static org.junit.Assert.assertFalse;
// import static org.junit.Assert.assertNotNull;
// import org.opensilex.process.process.dal.ProcessModel;
// import org.opensilex.process.process.dal.StepModel;
// import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
// import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
// import org.opensilex.sparql.model.SPARQLResourceModel;
// import java.time.OffsetDateTime;
// import java.net.URISyntaxException;
// import org.opensilex.sparql.exceptions.SPARQLException;
// import org.opensilex.sparql.service.SPARQLService;

// /**
//  * @author Emilie Fernandez
//  */
// public class StepAPITest extends AbstractSecurityIntegrationTest {

//     protected static String path = "/core/steps";

//     public static String uriPath = path + "/{uri}";
//     public static String searchPath = path ;
//     public static String createPath = path;
//     public static String updatePath = path ;
//     public static String deletePath = path + "/{uri}";
//     private static final Node scientificObjectGraph = NodeFactory.createURI("test:stepTest");
//     private static ScientificObjectModel input;

//     @Before
//     public void createSO() throws Exception {
//         SPARQLService sparql = getSparqlService();
//         input = new ScientificObjectModel();
//         input.setUri(new URI("test:input"));
//         input.setName("input");
//         input.setType(new URI("http://www.opensilex.org/vocabulary/oeso#Sample"));
//         sparql.create(scientificObjectGraph, input);
//     }

//     @After
//     public void after() throws URISyntaxException, SPARQLException {
//         getSparqlService().clearGraph(new URI(scientificObjectGraph.getURI()));
//     }

//     public static StepCreationDTO getCreationDTO() {

//         StepCreationDTO stepDto = new StepCreationDTO();
//         stepDto.setName("step 1");
//         stepDto.setStart(OffsetDateTime.now().toString());
//         List<URI> inputUris = new ArrayList<>();
//         inputUris.add(input.getUri());
//         stepDto.setInput(inputUris);
//         return stepDto;
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

//         StepCreationDTO stepDto = getCreationDTO();
//         final Response postResult = getJsonPostResponse(target(createPath), stepDto);

//         stepDto.setUri(extractUriFromResponse(postResult));
//         stepDto.setName("new name");
//         stepDto.setStart(OffsetDateTime.now().toString());

//         final Response updateResult = getJsonPutResponse(target(updatePath), stepDto);
//         assertEquals(Status.OK.getStatusCode(), updateResult.getStatus());

//         // retrieve the new step and compare to the expected step
//         final Response getResult = getJsonGetByUriResponse(target(uriPath), stepDto.getUri().toString());

//         // try to deserialize object
//         JsonNode node = getResult.readEntity(JsonNode.class);
//         SingleObjectResponse<StepGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<StepGetDTO>>() {
//         });
//         StepGetDTO dtoFromApi = getResponse.getResult();

//         // check that the object has been updated
//         assertEquals(stepDto.getName(), dtoFromApi.getName());
//         assertEquals(stepDto.getStart(), dtoFromApi.getStart());
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
//         SingleObjectResponse<StepGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<StepGetDTO>>() {
//         });
//         StepGetDTO stepDto = getResponse.getResult();
//         assertNotNull(stepDto);
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

//         StepCreationDTO creationDTO = getCreationDTO();
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
//         PaginatedListResponse<StepGetDTO> stepListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<StepGetDTO>>() {
//         });
//         List<StepGetDTO> stepDto = stepListResponse.getResult();

//         assertFalse(stepDto.isEmpty());
//     }
    

//     @Override
//     protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
//         return Collections.singletonList(StepModel.class);
//     }
// }
