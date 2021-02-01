//******************************************************************************
//                          DocumentAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.document.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.core.ontology.Oeso;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.net.URI;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import java.net.URL;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;

/**
 * @author Emilie FERNANDEZ
 */
public class DocumentAPITest extends AbstractSecurityIntegrationTest {

    protected String path = "/core/documents";

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    protected String uriPath = path + "/{uri}/description";
    protected String deletePath = path + "/{uri}";

    protected DocumentCreationDTO getCreationDTO() {

        DocumentCreationDTO docDto = new DocumentCreationDTO();
        docDto.setTitle("test document");
        LocalDate currentDate = LocalDate.now();
        docDto.setDate(currentDate.toString());
        docDto.setDescription("description");
        return docDto;
    }

    @Test
    public void testCreate() throws Exception {

        File file = tmpFolder.newFile("testFile.txt"); 
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("test".getBytes());
        }
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
        MultiPart multipart = new FormDataMultiPart().field("description", getCreationDTO(), MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);

        final Response postResult = getJsonPostResponseMultipart(target(path), multipart);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        final Response getResult = getJsonGetByUriResponse(target(uriPath), createdUri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {

        DocumentCreationDTO docDto = getCreationDTO();
        File file = tmpFolder.newFile("testFile.txt"); 
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("test".getBytes());
        }
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
        MultiPart multipart = new FormDataMultiPart().field("description", docDto, MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);
        final Response postResult = getJsonPostResponseMultipart(target(path), multipart);

        // update the doc
        docDto.setUri(extractUriFromResponse(postResult));
        docDto.setTitle("new title test document");

        final Response updateResult = getJsonPutResponseMultipart(target(path), multipart);
        assertEquals(Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new doc and compare to the expected doc
        final Response getResult = getJsonGetByUriResponse(target(uriPath), docDto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<DocumentGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<DocumentGetDTO>>() {
        });
        DocumentGetDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(docDto.getTitle(), dtoFromApi.getTitle());
    }

    @Test
    public void testDelete() throws Exception {

        // create object and check if URI exists

        File file = tmpFolder.newFile("testFile.txt"); 
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("test".getBytes());
        }
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
        MultiPart multipart = new FormDataMultiPart().field("description", getCreationDTO(), MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);

        Response postResponse = getJsonPostResponseMultipart(target(path), multipart);
        String uri = extractUriFromResponse(postResponse).toString();

        // delete object and check if URI no longer exists
        Response delResult = getDeleteByUriResponse(target(deletePath), uri);
        assertEquals(Status.OK.getStatusCode(), delResult.getStatus());

        Response getResult = getJsonGetByUriResponse(target(uriPath), uri);
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testGetByUri() throws Exception {

        File file = tmpFolder.newFile("testFile.txt"); 
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("test".getBytes());
        }
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
        MultiPart multipart = new FormDataMultiPart().field("description", getCreationDTO(), MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);

        final Response postResult = getJsonPostResponseMultipart(target(path), multipart);
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponse(target(uriPath), uri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<DocumentGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<DocumentGetDTO>>() {
        });
        DocumentGetDTO docDto = getResponse.getResult();
        assertNotNull(docDto);
    }

    @Test
    public void testGetByUriFail() throws Exception {

        File file = tmpFolder.newFile("testFile.txt"); 
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("test".getBytes());
        }
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
        MultiPart multipart = new FormDataMultiPart().field("description", getCreationDTO(), MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);

        final Response postResult = getJsonPostResponseMultipart(target(path), multipart);
        JsonNode node = postResult.readEntity(JsonNode.class);
        ObjectUriResponse postResponse = mapper.convertValue(node, ObjectUriResponse.class);
        String uri = postResponse.getResult();

        // call the service with a non existing pseudo random URI
        final Response getResult = getJsonGetByUriResponse(target(uriPath), uri + "7FG4FG89FG4GH4GH57");
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testSearch() throws Exception {

        File file = tmpFolder.newFile("testFile.txt"); 
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("test".getBytes());
        }
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
        MultiPart multipart = new FormDataMultiPart().field("description", getCreationDTO(), MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);

        final Response postResult = getJsonPostResponseMultipart(target(path), multipart);
        URI uri = extractUriFromResponse(postResult);

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("uri", uri);
            }
        };

        WebTarget searchTarget = appendSearchParams(target(path), 0, 50, params);
        final Response getResult = appendToken(searchTarget).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<DocumentGetDTO> docListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<DocumentGetDTO>>() {
        });
        List<DocumentGetDTO> docDto = docListResponse.getResult();

        assertFalse(docDto.isEmpty());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(DocumentModel.class);
    }
}
