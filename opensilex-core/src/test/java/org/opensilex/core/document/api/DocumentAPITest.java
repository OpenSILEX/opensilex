//******************************************************************************
//                          DocumentAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.document.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;
import static org.opensilex.core.document.dal.DocumentDAO.FS_DOCUMENT_PREFIX;

/**
 * @author Emilie FERNANDEZ
 */
public class DocumentAPITest extends AbstractSecurityIntegrationTest {

    protected String path = "/core/documents";

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    protected FileStorageService fs;

    protected String uriPath = path + "/{uri}/description";
    protected String getFilePath = path + "/{uri}";
    protected String deletePath = path + "/{uri}";

    FileStorageService getFs(){

        if(fs == null){
            fs = getOpensilex().getServiceInstance(FileStorageService.DEFAULT_FS_SERVICE, FileStorageService.class);
        }
        return fs;
    }

    protected DocumentCreationDTO getCreationDTO() throws URISyntaxException {

        DocumentCreationDTO docDto = new DocumentCreationDTO();
        docDto.setType(new URI(Oeso.Document.toString()));
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
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), createdUri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testCreateWithSource() throws Exception {
        URI sourceUri = new URI("https://example.org");

        DocumentCreationDTO creationDTO = getCreationDTO();
        creationDTO.setUri(new URI("http://opensilex.org/testCreateWithSource"));
        creationDTO.setSource(sourceUri);
        MultiPart multiPart = new FormDataMultiPart().field("description", creationDTO, MediaType.APPLICATION_JSON_TYPE);

        final Response postResult = getJsonPostResponseMultipart(target(path), multiPart);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), createdUri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<DocumentGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<DocumentGetDTO>>() {
        });
        DocumentGetDTO dtoFromApi = getResponse.getResult();

        assertEquals(sourceUri, dtoFromApi.getSource());
    }

    @Test
    public void testCreateWithoutFileOrSourceFail() throws Exception {
        DocumentCreationDTO creationDTO = getCreationDTO();
        creationDTO.setUri(new URI("http://opensilex.org/testCreateWithoutFileOrSourceFail"));
        MultiPart multiPart = new FormDataMultiPart().field("description", creationDTO, MediaType.APPLICATION_JSON_TYPE);

        final Response postResult = getJsonPostResponseMultipart(target(path), multiPart);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void testCreateFsFail() throws Exception{

        File file = tmpFolder.newFile("testCreateFsFail.txt");
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);

        DocumentCreationDTO creationDTO = getCreationDTO();
        creationDTO.setUri(new URI("http://opensilex.org/testCreateFsFail"));

        MultiPart multipart = new FormDataMultiPart().field("description", creationDTO, MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);

        // try to insert a empty file
        final Response postResult = getJsonPostResponseMultipart(target(path), multipart);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());

        // ensure that the document model has not been inserted
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), creationDTO.getUri().toString());
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());

        // ensure that the file has not been inserted
        final Response getFileResult = getOctetStreamByUriResponse(target(getFilePath),creationDTO.getUri().toString());
        assertEquals(Status.NOT_FOUND.getStatusCode(), getFileResult.getStatus());
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
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), docDto.getUri().toString());

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

        Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri);
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testDeleteFail() throws Exception{

        // create object and check if URI exists

        File file = tmpFolder.newFile("testFile.txt");
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("test".getBytes());
        }
        DocumentCreationDTO creationDTO = getCreationDTO();
        creationDTO.setUri(new URI("http://opensilex.org/testDeleteFail"));

        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
        MultiPart multipart = new FormDataMultiPart().field("description", creationDTO, MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);

        Response postResponse = getJsonPostResponseMultipart(target(path), multipart);

        Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), creationDTO.getUri().toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to get the file path and to delete file, outside of the document API
        getFs().delete(FS_DOCUMENT_PREFIX,creationDTO.getUri());

        // try to delete document : delete must fail because the file has been deleted
        final Response delResult = getDeleteByUriResponse(target(deletePath), creationDTO.getUri().toString());
        assertEquals(Status.NOT_FOUND.getStatusCode(), delResult.getStatus());

        // check that file download failed
        final Response getFileResult = getOctetStreamByUriResponse(target(getFilePath), creationDTO.getUri().toString());
        assertEquals(Status.NOT_FOUND.getStatusCode(), getFileResult.getStatus());

        // check that the document model is still present
        getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), creationDTO.getUri().toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

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

        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<DocumentGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<DocumentGetDTO>>() {
        });
        DocumentGetDTO docDto = getResponse.getResult();
        assertNotNull(docDto);
    }

    @Test
    public void testGetFileByUri() throws Exception{

        File file = tmpFolder.newFile("testFile.txt");
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("test".getBytes());
        }
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
        MultiPart multipart = new FormDataMultiPart().field("description", getCreationDTO(), MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);

        final Response postResult = getJsonPostResponseMultipart(target(path), multipart);
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getOctetStreamByUriResponse(target(getFilePath), uri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        byte[] fileBytes = IOUtils.toByteArray(getResult.readEntity(InputStream.class));
        assertNotNull(fileBytes);
        assertEquals(4,fileBytes.length);

        byte[] initialFileBytes = Files.readAllBytes(Paths.get(file.getPath()));
        assertArrayEquals(fileBytes, initialFileBytes);
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
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri + "7FG4FG89FG4GH4GH57");
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testGetFileByUriFail() throws Exception{
        File file = tmpFolder.newFile("testFile.txt");
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("test".getBytes());
        }
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
        DocumentCreationDTO creationDTO = getCreationDTO();
        creationDTO.setUri(new URI("http://opensilex.org/testGetFileFail"));
        MultiPart multipart = new FormDataMultiPart().field("description",creationDTO, MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);

        final Response postResponse = getJsonPostResponseMultipart(target(path), multipart);

        // try to get the file path and to delete file, outside of the document API
        getFs().delete(FS_DOCUMENT_PREFIX,creationDTO.getUri());

        Response getResult = getOctetStreamByUriResponse(target(getFilePath), creationDTO.getUri().toString());
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
        final Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<DocumentGetDTO> docListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<DocumentGetDTO>>() {
        });
        List<DocumentGetDTO> docDto = docListResponse.getResult();

        assertFalse(docDto.isEmpty());
    }

    /**
     * Tests the search with a "multiple" parameter (which should filter on the title OR keyword)
     *
     * @throws Exception
     */
    @Test
    public void testSearchMultiple() throws Exception {
        URI exampleSource = new URI("https://example.org");

        // Creates a document with "test" as keyword
        DocumentCreationDTO document1 = getCreationDTO();
        document1.setTitle("document1");
        document1.setSubject(new ArrayList<String>() {{
            add("test");
        }});
        document1.setSource(exampleSource);
        // Creates a document with "test" as title
        DocumentCreationDTO document2 = getCreationDTO();
        document2.setTitle("test");
        document2.setSubject(new ArrayList<>());
        document2.setSource(exampleSource);
        // Creates a document without "test" in title nor keyword
        DocumentCreationDTO document3 = getCreationDTO();
        document3.setTitle("document3");
        document3.setSubject(new ArrayList<>());
        document3.setSource(exampleSource);

        // Creates the multiparts to send in the request
        MultiPart multiPart1 = new FormDataMultiPart().field("description", document1, MediaType.APPLICATION_JSON_TYPE);
        MultiPart multiPart2 = new FormDataMultiPart().field("description", document2, MediaType.APPLICATION_JSON_TYPE);
        MultiPart multiPart3 = new FormDataMultiPart().field("description", document3, MediaType.APPLICATION_JSON_TYPE);

        // Request to create the documents
        final Response postResult1 = getJsonPostResponseMultipart(target(path), multiPart1);
        URI documentUri1 = SPARQLDeserializers.formatURI(extractUriFromResponse(postResult1));
        final Response postResult2 = getJsonPostResponseMultipart(target(path), multiPart2);
        URI documentUri2 = SPARQLDeserializers.formatURI(extractUriFromResponse(postResult2));
        final Response postResult3 = getJsonPostResponseMultipart(target(path), multiPart3);
        URI documentUri3 = SPARQLDeserializers.formatURI(extractUriFromResponse(postResult3));

        // Search params
        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("multiple", "test");
            }
        };

        // Perform the search
        WebTarget searchTarget = appendSearchParams(target(path), 0, 50, params);
        final Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // Get the result and assert that they are correct
        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<DocumentGetDTO> docListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<DocumentGetDTO>>() {
        });
        List<DocumentGetDTO> documentSearchList = docListResponse.getResult();

        // We should get the document 1 and the document 2, but not the 3
        assertEquals(2, documentSearchList.size());
        assertTrue(documentSearchList.stream().anyMatch(document ->
                Objects.equals(SPARQLDeserializers.formatURI(document.getUri()), documentUri1)));
        assertTrue(documentSearchList.stream().anyMatch(document ->
                Objects.equals(SPARQLDeserializers.formatURI(document.getUri()), documentUri2)));
        assertTrue(documentSearchList.stream().noneMatch(document ->
                Objects.equals(SPARQLDeserializers.formatURI(document.getUri()), documentUri3)));
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(DocumentModel.class);
    }
}
