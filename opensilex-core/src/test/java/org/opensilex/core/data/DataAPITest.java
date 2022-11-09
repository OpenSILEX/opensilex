//******************************************************************************
//                          DataFileAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static junit.framework.TestCase.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.vocabulary.XSD;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.data.api.DataCSVValidationDTO;
import org.opensilex.core.data.api.DataCreationDTO;
import org.opensilex.core.data.api.DataGetDTO;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.provenance.api.ProvenanceAPITest;
import org.opensilex.core.provenance.api.ProvenanceCreationDTO;
import org.opensilex.core.scientificObject.api.ScientificObjectAPITest;
import org.opensilex.core.variable.api.VariableApiTest;
import org.opensilex.core.variable.api.VariableCreationDTO;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.DateFormatters;

/**
 *
 * @author Alice Boizet
 */
public class DataAPITest extends AbstractMongoIntegrationTest {
    public static final String path = "/core/data";

    protected String uriPath = path + "/{uri}";
    protected String searchPath = path;
    protected String createPath = path;
    protected String importPath = path + "/import";

    public static final String createListPath = path;
    protected String updatePath = path;
    protected String deletePath = path + "/{uri}";

    private ProvenanceAPITest provAPI = new ProvenanceAPITest();

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private URI variable;

    // Variables with specific types and template strings for CSV files
    private URI dateVariable;
    private static final String FILE_TEMPLATE_DATE_VARIABLE_URI = "__DATE_VARIABLE__";
    private URI datetimeVariable;
    private static final String FILE_TEMPLATE_DATETIME_VARIABLE_URI = "__DATETIME_VARIABLE__";
    private URI integerVariable;
    private static final String FILE_TEMPLATE_INTEGER_VARIABLE_URI = "__INTEGER_VARIABLE__";
    private URI decimalVariable;
    private static final String FILE_TEMPLATE_DECIMAL_VARIABLE_URI = "__DECIMAL_VARIABLE__";
    private URI booleanVariable;
    private static final String FILE_TEMPLATE_BOOLEAN_VARIABLE_URI = "__BOOLEAN_VARIABLE__";
    private URI stringVariable;
    private static final String FILE_TEMPLATE_STRING_VARIABLE_URI = "__STRING_VARIABLE__";

    // Target template string for CSV files
    private static final String FILE_TEMPLATE_TARGET_URI = "__TARGET__";

    // CSV files for import tests
    private static final Path FILE_PATH_IMPORT_INTEGER = Paths.get("data", "importInteger.csv");
    private static final Path FILE_PATH_IMPORT_DECIMAL = Paths.get("data", "importDecimal.csv");
    private static final Path FILE_PATH_IMPORT_BOOLEAN = Paths.get("data", "importBoolean.csv");
    private static final Path FILE_PATH_IMPORT_STRING = Paths.get("data", "importString.csv");
    private static final Path FILE_PATH_IMPORT_DATE = Paths.get("data", "importDate.csv");
    private static final Path FILE_PATH_IMPORT_DATETIME = Paths.get("data", "importDatetime.csv");
    private static final Path FILE_PATH_IMPORT_INTEGER_DATATYPE_ERROR = Paths.get("data", "importIntegerDatatypeError.csv");
    private static final Path FILE_PATH_IMPORT_DATE_DATATYPE_ERROR = Paths.get("data", "importDateDatatypeError.csv");
    private static final Path FILE_PATH_IMPORT_DATETIME_DATATYPE_ERROR = Paths.get("data", "importDatetimeDatatypeError.csv");

    // Service parameter names
    private static final String IMPORT_FILE_MULTIPART_PARAMETER_NAME = "file";
    private static final String IMPORT_PROVENANCE_QUERY_PARAMETER_NAME = "provenance";
    private static final String SEARCH_PROVENANCES_QUERY_PARAMETER_NAME = "provenances";

    // Provenances for CSV import tests
    private URI provenanceImportInteger;
    private URI provenanceImportDecimal;
    private URI provenanceImportBoolean;
    private URI provenanceImportString;
    private URI provenanceImportDate;
    private URI provenanceImportDatetime;
    private URI provenanceImportIntegerDatatypeError;
    private URI provenanceImportDateDatatypeError;
    private URI provenanceImportDatetimeDatatypeError;

    private DataProvenanceModel provenance;
    private List<URI> scientificObjects;    
    
    @Before
    public void beforeTest() throws Exception {
        //create experiment
        ExperimentAPITest expAPI = new ExperimentAPITest();
        Response postResultExp = getJsonPostResponseAsAdmin(target(expAPI.createPath), expAPI.getCreationDTO());
        List<URI> experiments = new ArrayList<>();
        experiments.add(extractUriFromResponse(postResultExp));
        
        createProvenances(experiments);

        createVariables();
        
        //create scientific object
        ScientificObjectAPITest soAPI = new ScientificObjectAPITest();
        Response postResultSO = getJsonPostResponseAsAdmin(target(ScientificObjectAPITest.createPath), soAPI.getCreationDTO(false));
        scientificObjects = new ArrayList<>();
        scientificObjects.add(extractUriFromResponse(postResultSO)); 
        
    }

    private void createVariables() throws Exception {
        VariableApiTest varAPI = new VariableApiTest();
        Response postResultVar = getJsonPostResponseAsAdmin(target(varAPI.createPath), varAPI.getCreationDto());
        variable = extractUriFromResponse(postResultVar);

        VariableCreationDTO variableDateDTO = varAPI.getCreationDto();
        variableDateDTO.setDataType(new URI(XSD.date.getURI()));
        postResultVar = getJsonPostResponseAsAdmin(target(varAPI.createPath), variableDateDTO);
        dateVariable = extractUriFromResponse(postResultVar);

        VariableCreationDTO variableDatetimeDTO = varAPI.getCreationDto();
        variableDatetimeDTO.setDataType(new URI(XSD.dateTime.getURI()));
        postResultVar = getJsonPostResponseAsAdmin(target(varAPI.createPath), variableDatetimeDTO);
        datetimeVariable = extractUriFromResponse(postResultVar);

        VariableCreationDTO variableDecimalDTO = varAPI.getCreationDto();
        variableDecimalDTO.setDataType(new URI(XSD.decimal.getURI()));
        postResultVar = getJsonPostResponseAsAdmin(target(varAPI.createPath), variableDecimalDTO);
        decimalVariable = extractUriFromResponse(postResultVar);

        VariableCreationDTO variableIntegerDTO = varAPI.getCreationDto();
        variableIntegerDTO.setDataType(new URI(XSD.integer.getURI()));
        postResultVar = getJsonPostResponseAsAdmin(target(varAPI.createPath), variableIntegerDTO);
        integerVariable = extractUriFromResponse(postResultVar);

        VariableCreationDTO variableBooleanDTO = varAPI.getCreationDto();
        variableBooleanDTO.setDataType(new URI(XSD.xboolean.getURI()));
        postResultVar = getJsonPostResponseAsAdmin(target(varAPI.createPath), variableBooleanDTO);
        booleanVariable = extractUriFromResponse(postResultVar);

        VariableCreationDTO variableStringDTO = varAPI.getCreationDto();
        variableStringDTO.setDataType(new URI(XSD.xstring.getURI()));
        postResultVar = getJsonPostResponseAsAdmin(target(varAPI.createPath), variableStringDTO);
        stringVariable = extractUriFromResponse(postResultVar);
    }

    private URI createOneProvenance(String name) throws Exception {
        ProvenanceCreationDTO provenanceImportIntegerDTO = new ProvenanceCreationDTO();
        provenanceImportIntegerDTO.setName("Import test : integer");
        Response postResultProv = getJsonPostResponseAsAdmin(target(provAPI.createPath), provenanceImportIntegerDTO);
        return extractUriFromResponse(postResultProv);
    }

    private void createProvenances(List<URI> experiments) throws Exception {
        ProvenanceCreationDTO prov = new ProvenanceCreationDTO();
        prov.setName("name");
        Response postResultProv = getJsonPostResponseAsAdmin(target(provAPI.createPath), prov);

        provenance = new DataProvenanceModel();
        provenance.setUri(extractUriFromResponse(postResultProv));
        provenance.setExperiments(experiments);

        // Provenances for import tests
        provenanceImportInteger = createOneProvenance("Import test : integer");
        provenanceImportDecimal = createOneProvenance("Import test : decimal");
        provenanceImportBoolean = createOneProvenance("Import test : boolean");
        provenanceImportString = createOneProvenance("Import test : string");
        provenanceImportDate = createOneProvenance("Import test : date");
        provenanceImportDatetime = createOneProvenance("Import test : datetime");
        provenanceImportIntegerDatatypeError = createOneProvenance("Import test : integer with datatype errors");
        provenanceImportDateDatatypeError = createOneProvenance("Import test : date with datatype errors");
        provenanceImportDatetimeDatatypeError = createOneProvenance("Import test : date with datatype errors");
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

    private MultiPart getImportFileAsMultipart(Path filePath) throws IOException {
        File file = tmpFolder.newFile("import.csv");
        InputStream sourceFileStream = OpenSilex.getResourceAsStream(filePath.toString());

        String fileContent = IOUtils.toString(sourceFileStream, StandardCharsets.UTF_8);

        fileContent = fileContent
                .replaceAll(FILE_TEMPLATE_DATE_VARIABLE_URI, dateVariable.toString())
                .replaceAll(FILE_TEMPLATE_DATETIME_VARIABLE_URI, datetimeVariable.toString())
                .replaceAll(FILE_TEMPLATE_INTEGER_VARIABLE_URI, integerVariable.toString())
                .replaceAll(FILE_TEMPLATE_DECIMAL_VARIABLE_URI, decimalVariable.toString())
                .replaceAll(FILE_TEMPLATE_BOOLEAN_VARIABLE_URI, booleanVariable.toString())
                .replaceAll(FILE_TEMPLATE_STRING_VARIABLE_URI, stringVariable.toString())
                .replaceAll(FILE_TEMPLATE_TARGET_URI, scientificObjects.get(0).toString());

        FileUtils.writeStringToFile(file, fileContent, StandardCharsets.UTF_8);

        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart(IMPORT_FILE_MULTIPART_PARAMETER_NAME, file, APPLICATION_OCTET_STREAM_TYPE);

        return new FormDataMultiPart()
                .bodyPart(fileDataBodyPart);
    }

    private DataCSVValidationDTO getImportResponseAsDTO(Path fileToImport, URI provenanceUri) throws Exception {
        final Response postResult = getJsonPostResponseMultipart(
                target(importPath).queryParam(IMPORT_PROVENANCE_QUERY_PARAMETER_NAME, provenanceUri.toString()),
                getImportFileAsMultipart(fileToImport));

        assertEquals(Response.Status.OK.getStatusCode(), postResult.getStatus());

        JsonNode node = postResult.readEntity(JsonNode.class);
        SingleObjectResponse<DataCSVValidationDTO> postResponse = mapper.convertValue(node,
                new TypeReference<SingleObjectResponse<DataCSVValidationDTO>>() {});
        return postResponse.getResult();
    }

    private List<DataGetDTO> getSearchResponseAsDTOList(URI provenance) throws Exception {
        final Response searchResult = getJsonGetResponseAsAdmin(appendSearchParams(target(searchPath), 0, 20, new HashMap<String, Object>() {{
            put(SEARCH_PROVENANCES_QUERY_PARAMETER_NAME, Collections.singletonList(provenance));
        }}));
        assertEquals(Response.Status.OK.getStatusCode(), searchResult.getStatus());
        JsonNode node = searchResult.readEntity(JsonNode.class);
        PaginatedListResponse<DataGetDTO> searchResponse = mapper.convertValue(node,
                new TypeReference<PaginatedListResponse<DataGetDTO>>() {});
        return searchResponse.getResult();
    }
       
    @Test
    public void testCreate() throws Exception {        
        ArrayList<DataCreationDTO> dtoList = new ArrayList<>();
        dtoList.add(getCreationDataDTO("2020-10-11T10:29:06.402+0200"));
        final Response postResultData = getJsonPostResponseAsAdmin(target(createListPath), dtoList);
        LOGGER.info(postResultData.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());        
    }

    @Test
    public void testImportInteger() throws Exception {
        DataCSVValidationDTO csvValidationDTO = getImportResponseAsDTO(FILE_PATH_IMPORT_INTEGER, provenanceImportInteger);

        assertFalse(csvValidationDTO.getDataErrors().hasErrors());
        assertEquals(1, csvValidationDTO.getDataErrors().getNbLinesImported().intValue());

        List<DataGetDTO> importedDataList = getSearchResponseAsDTOList(provenanceImportInteger);

        assertEquals(1, importedDataList.size());
        assertEquals(13, importedDataList.get(0).getValue());
    }

    @Test
    public void testImportDecimal() throws Exception {
        DataCSVValidationDTO csvValidationDTO = getImportResponseAsDTO(FILE_PATH_IMPORT_DECIMAL, provenanceImportDecimal);

        assertFalse(csvValidationDTO.getDataErrors().hasErrors());
        assertEquals(2, csvValidationDTO.getDataErrors().getNbLinesImported().intValue());

        List<DataGetDTO> importedDataList = getSearchResponseAsDTOList(provenanceImportDecimal);

        assertEquals(2, importedDataList.size());
        assertTrue(importedDataList.stream().anyMatch(dto -> Objects.equals(dto.getValue(), 3.14d)));
        assertTrue(importedDataList.stream().anyMatch(dto -> Objects.equals(dto.getValue(), 3d)));
    }

    @Test
    public void testImportBoolean() throws Exception {
        DataCSVValidationDTO csvValidationDTO = getImportResponseAsDTO(FILE_PATH_IMPORT_BOOLEAN, provenanceImportBoolean);

        assertFalse(csvValidationDTO.getDataErrors().hasErrors());
        assertEquals(10, csvValidationDTO.getDataErrors().getNbLinesImported().intValue());

        List<DataGetDTO> importedDataList = getSearchResponseAsDTOList(provenanceImportBoolean);

        assertEquals(10, importedDataList.size());
        assertEquals(5, importedDataList.stream().filter(dto -> Objects.equals(dto.getValue(), true)).count());
        assertEquals(5, importedDataList.stream().filter(dto -> Objects.equals(dto.getValue(), false)).count());
    }

    @Test
    public void testImportString() throws Exception {
        DataCSVValidationDTO csvValidationDTO = getImportResponseAsDTO(FILE_PATH_IMPORT_STRING, provenanceImportString);

        assertFalse(csvValidationDTO.getDataErrors().hasErrors());
        assertEquals(1, csvValidationDTO.getDataErrors().getNbLinesImported().intValue());

        List<DataGetDTO> importedDataList = getSearchResponseAsDTOList(provenanceImportString);

        assertEquals(1, importedDataList.size());
        assertEquals("Hello world !", importedDataList.get(0).getValue());
    }

    @Test
    public void testImportDate() throws Exception {
        DataCSVValidationDTO csvValidationDTO = getImportResponseAsDTO(FILE_PATH_IMPORT_DATE, provenanceImportDate);

        assertFalse(csvValidationDTO.getDataErrors().hasErrors());
        assertEquals(1, csvValidationDTO.getDataErrors().getNbLinesImported().intValue());

        List<DataGetDTO> importedDataList = getSearchResponseAsDTOList(provenanceImportDate);

        assertEquals(1, importedDataList.size());
        LocalDate importedDate = LocalDate.parse((String) importedDataList.get(0).getValue(), DateFormatters.fromFormat(DateFormat.YMD));
        assertEquals(LocalDate.of(2022, 7, 18), importedDate);
    }

    @Test
    public void testImportDatetime() throws Exception {
        DataCSVValidationDTO csvValidationDTO = getImportResponseAsDTO(FILE_PATH_IMPORT_DATETIME, provenanceImportDatetime);

        assertFalse(csvValidationDTO.getDataErrors().hasErrors());
        assertEquals(11, csvValidationDTO.getDataErrors().getNbLinesImported().intValue());

        List<DataGetDTO> importedDataList = getSearchResponseAsDTOList(provenanceImportDatetime);

        assertEquals(11, importedDataList.size());
        for (DataGetDTO data : importedDataList) {
            ZonedDateTime.parse((String) data.getValue(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }

    @Test
    public void testImportIntegerDatatypeError() throws Exception {
        DataCSVValidationDTO csvValidationDTO = getImportResponseAsDTO(FILE_PATH_IMPORT_INTEGER_DATATYPE_ERROR, provenanceImportIntegerDatatypeError);

        assertTrue(csvValidationDTO.getDataErrors().hasErrors());
        assertEquals(2, csvValidationDTO.getDataErrors().getInvalidDataTypeErrors().size());
        assertEquals(1, csvValidationDTO.getDataErrors().getNbLinesToImport().intValue());

        // Rows 1 and 2 are invalid
        assertTrue(csvValidationDTO.getDataErrors().getInvalidDataTypeErrors().containsKey(1));
        assertTrue(csvValidationDTO.getDataErrors().getInvalidDataTypeErrors().containsKey(2));
    }

    @Test
    public void testImportDateDatatypeError() throws Exception {
        DataCSVValidationDTO csvValidationDTO = getImportResponseAsDTO(FILE_PATH_IMPORT_DATE_DATATYPE_ERROR, provenanceImportDateDatatypeError);

        assertTrue(csvValidationDTO.getDataErrors().hasErrors());
        assertEquals(2, csvValidationDTO.getDataErrors().getInvalidDataTypeErrors().size());
        assertEquals(1, csvValidationDTO.getDataErrors().getNbLinesToImport().intValue());

        // Rows 0 and 1 are invalid
        assertTrue(csvValidationDTO.getDataErrors().getInvalidDataTypeErrors().containsKey(0));
        assertTrue(csvValidationDTO.getDataErrors().getInvalidDataTypeErrors().containsKey(1));
    }

    @Test
    public void testImportDatetimeDatatypeError() throws Exception {
        DataCSVValidationDTO csvValidationDTO = getImportResponseAsDTO(FILE_PATH_IMPORT_DATETIME_DATATYPE_ERROR, provenanceImportDatetimeDatatypeError);

        assertTrue(csvValidationDTO.getDataErrors().hasErrors());
        assertEquals(1, csvValidationDTO.getDataErrors().getInvalidDataTypeErrors().size());
        assertEquals(1, csvValidationDTO.getDataErrors().getNbLinesToImport().intValue());

        // Row 0 is invalid
        assertTrue(csvValidationDTO.getDataErrors().getInvalidDataTypeErrors().containsKey(0));
    }
    
    @Test
    public void testUpdate() throws Exception {
        ArrayList<DataCreationDTO> dtoList = new ArrayList<>();
        DataCreationDTO dto = getCreationDataDTO("2020-10-12T10:29:06.402+0200");
        dtoList.add(dto);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), dtoList);

        dto.setUri(extractUriFromResponse(postResult));
        dto.setValue(10.2);
        
        // check update ok
        final Response updateResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());

        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), dto.getUri().toString());

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
        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), dtoList);
        String uri = extractUriFromResponse(postResponse).toString();

        // delete object and check if URI no longer exists
        Response delResult = getDeleteByUriResponse(target(deletePath), uri);
        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());

        Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testGetByUri() throws Exception {
        ArrayList<DataCreationDTO> dtoList = new ArrayList<>();
        dtoList.add(getCreationDataDTO("2020-10-14T10:29:06.402+0200"));
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), dtoList);
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri.toString());
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
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), dtoList);
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
        final Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<DataGetDTO> dataListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<DataGetDTO>>() {
        });
        List<DataGetDTO> datas = dataListResponse.getResult();

        assertFalse(datas.isEmpty());
    }
    
}
