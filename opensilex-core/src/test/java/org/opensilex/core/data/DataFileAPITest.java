//******************************************************************************
//                          DataFileAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import javax.imageio.ImageIO;
import javax.ws.rs.core.MediaType;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.data.api.DataFileCreationDTO;
import org.opensilex.core.data.api.DataFileGetDTO;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.api.ProvenanceAPITest;
import org.opensilex.core.provenance.api.ProvenanceCreationDTO;
import org.opensilex.core.variable.api.VariableApiTest;
import org.opensilex.server.response.SingleObjectResponse;

/**
 *
 * @author Alice Boizet
 */
public class DataFileAPITest extends AbstractMongoIntegrationTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    
    protected String path = "/core/datafiles";

    protected String uriPath = path + "/{uri}";
    protected String searchPath = path;
    protected String createPath = path;
    protected String deletePath = path + "/{uri}";    
    protected String getDescriptionPath = path + "/{uri}/description";
        
    private DataProvenanceModel provenance;
    
    @Before
    public void beforeTest() throws Exception {
        //create provenance
        ProvenanceAPITest provAPI = new ProvenanceAPITest();
        ProvenanceCreationDTO prov = new ProvenanceCreationDTO();
        prov.setName("name");
        Response postResultProv = getJsonPostResponse(target(provAPI.createPath), prov);
        provenance = new DataProvenanceModel();
        provenance.setUri(extractUriFromResponse(postResultProv)); 

    }
    
    public DataFileCreationDTO getCreationFileDTO(String date) throws URISyntaxException, Exception {
        DataFileCreationDTO dataDTO = new DataFileCreationDTO();
        
        dataDTO.setProvenance(provenance);
        //dataDTO.setScientificObjects(scientificObjects);
        
        dataDTO.setRdfType(new URI(Oeso.Image.toString()));
        dataDTO.setDate(date);
                
        return dataDTO;        
    }    
    
    protected DataFileCreationDTO create(String date) throws Exception{
        File file = tmpFolder.newFile("testFile.txt"); 
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("test".getBytes());
        }
        
        final FormDataMultiPart mp = new FormDataMultiPart();
        DataFileCreationDTO dto = getCreationFileDTO(date);
        
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
        MultiPart multipart = new FormDataMultiPart().field("description", dto, MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);


        final Response postResult = getJsonPostResponseMultipart(target(createPath), multipart);
        
        dto.setUri(extractUriFromResponse(postResult));
        return dto;
    }

    //@Test
    public void testCreate() throws Exception {
        File file = tmpFolder.newFile("testFile.txt"); 
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("test".getBytes());
        }        
        
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
        MultiPart multipart = new FormDataMultiPart().field("description", getCreationFileDTO("2020-02-09T10:29:06.402+0200"), MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);
            
        final Response postResult = getJsonPostResponseMultipart(target(createPath), multipart);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());    
    }
    
    //@Test
    public void testGetDescriptionByUri() throws Exception {

        File file = tmpFolder.newFile("testFile.txt"); 
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("test".getBytes());
        }
        
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
        MultiPart multipart = new FormDataMultiPart().field("description", getCreationFileDTO("2020-02-10T10:29:06.402+0200"), MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);
            
        final Response postResult = getJsonPostResponseMultipart(target(createPath), multipart);
        URI uri = extractUriFromResponse(postResult);
    
        final Response getResult = getJsonGetByUriResponse(target(getDescriptionPath), uri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        //try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<DataFileGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<DataFileGetDTO>>() {
        });
        DataFileGetDTO fileGetDto = getResponse.getResult();
        assertNotNull(fileGetDto);
    }
    
    private File createTestImage() throws IOException {
        // Constructs a BufferedImage of one of the predefined image types.
        BufferedImage bufferedImage = new BufferedImage(250, 250, BufferedImage.TYPE_INT_RGB);

        // Create a graphics which can be used to draw into the buffered image
        Graphics2D g2d = bufferedImage.createGraphics();

        // fill all the image with white
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, 250, 250);

        // create a circle with black
        g2d.setColor(Color.black);
        g2d.fillOval(0, 0, 250, 250);

        // create a string with yellow
        g2d.setColor(Color.yellow);
        g2d.drawString("Testing image", 50, 120);

        // Disposes of this graphics context and releases any system resources that it is using. 
        g2d.dispose();

        // Save as PNG
        File file = tmpFolder.newFile("image.png"); 
        ImageIO.write(bufferedImage, "png", file);
        
        return file;
    }

    
//    @Test
//    public void testGetFileByUri() throws Exception {
//
//        File file = createTestImage();
//        
//        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
//        MultiPart multipart = new FormDataMultiPart().field("description", getCreationFileDTO("2020-02-11T10:29:06.402+0200"), MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);
//            
//        final Response postResult = getJsonPostResponseMultipart(target(createPath), multipart);
//        URI uri = extractUriFromResponse(postResult); 
//        
//        final Response getResult = getJsonGetByUriResponse(target(uriPath), uri.toString());
//        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
//
//        // try to read the image
//        InputStream input = (InputStream)getResult.getEntity();
//        BufferedImage image = ImageIO.read(input);
//        assertNotNull(image);
//    }


}
