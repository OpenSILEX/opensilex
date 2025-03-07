//******************************************************************************
//                          DataFileAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.vocabulary.XSD;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.annotation.api.AnnotationGetDTO;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.data.api.DataCSVValidationDTO;
import org.opensilex.core.data.api.DataCreationDTO;
import org.opensilex.core.data.api.DataFileCreationDTO;
import org.opensilex.core.data.api.DataGetDTO;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.dal.ProvEntityModel;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.api.VariableApiTest;
import org.opensilex.core.variable.api.VariableCreationDTO;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.DateFormatters;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.mail.internet.InternetAddress;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static junit.framework.TestCase.*;

/**
 *
 * @author Alice Boizet
 * @author rcolin
 */
public class DataAPITest extends AbstractMongoIntegrationTest {
    public static final String PATH = "/core/data";
    public static final String ANNOTATION_PATH = "/core/annotations";

    protected static final String URI_PATH = PATH + "/{uri}";
    protected static final String SEARCH_PATH = PATH;
    protected static final String IMPORT_PATH = PATH + "/import";

    public static final String CREATE_PATH = PATH;
    protected static final String UPDATE_PATH = PATH;
    protected static final String DELETE_PATH = PATH + "/{uri}";

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private static URI variable;

    // Variables with specific types and template strings for CSV files
    private static URI dateVariable;
    private static final String FILE_TEMPLATE_DATE_VARIABLE_URI = "__DATE_VARIABLE__";
    private static URI datetimeVariable;
    private static final String FILE_TEMPLATE_DATETIME_VARIABLE_URI = "__DATETIME_VARIABLE__";
    private static URI integerVariable;
    private static final String FILE_TEMPLATE_INTEGER_VARIABLE_URI = "__INTEGER_VARIABLE__";
    private static URI decimalVariable;
    private static final String FILE_TEMPLATE_DECIMAL_VARIABLE_URI = "__DECIMAL_VARIABLE__";
    private static URI booleanVariable;
    private static final String FILE_TEMPLATE_BOOLEAN_VARIABLE_URI = "__BOOLEAN_VARIABLE__";
    private static URI stringVariable;
    private static final String FILE_TEMPLATE_STRING_VARIABLE_URI = "__STRING_VARIABLE__";

    // Target template string for CSV files
    private static final String FILE_TEMPLATE_TARGET_URI = "__TARGET__";

    // CSV files for import tests
    private static final Path FILE_PATH_IMPORT_INTEGER = Paths.get("data", "importInteger.csv");
    private static final Path FILE_PATH_IMPORT_ANNOTATION_NO_TARGET_COL = Paths.get("data", "importAnnotationsNoTargetColumnError.csv");
    private static final Path FILE_PATH_IMPORT_ANNOTATION = Paths.get("data", "importAnnotation.csv");
    private static final Path FILE_PATH_IMPORT_ANNOTATION_NO_TARGET = Paths.get("data", "importAnnotationsNoTargetError.csv");
    private static final Path FILE_PATH_IMPORT_ANNOTATION_NOT_ON_EVERY_LINE = Paths.get("data", "importSomeAnnotations.csv");
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
    private static URI provenanceImportInteger;
    private static URI provenanceImportDecimal;
    private static URI provenanceImportBoolean;
    private static URI provenanceImportString;
    private static URI provenanceImportDate;
    private static URI provenanceImportDatetime;
    private static URI provenanceImportIntegerDatatypeError;
    private static URI provenanceImportDateDatatypeError;
    private static URI provenanceImportDatetimeDatatypeError;
    private static URI provenanceImportAnnotation;

    private static URI globalProvenanceURI;
    private static DataProvenanceModel provenanceWithXP,  provenanceWithoutXP;
    private static ScientificObjectModel os, osWithXp;
    private static DeviceModel device;
    private static AccountModel account;
    private static FacilityModel facility;

    @BeforeClass
    public static void beforeTest() throws Exception {

        SPARQLService sparql = newSparqlService();

        //create experiment
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, getMongoDBService());
        ExperimentModel xp =  ExperimentAPITest.getCreationDTO().newModel();
        experimentDAO.create(xp);
        List<URI> experiments = Collections.singletonList(xp.getUri());

        createProvenances(experiments);
        createVariables(sparql);

        //create scientific object
        // Don't use API in order to reduce tests dependencies and make test faster (no API call wrapping)
        os = new ScientificObjectModel();
        os.setName("DataAPITest");
        sparql.create(os);

        osWithXp = new ScientificObjectModel();
        osWithXp.setName("DataAPITest-xp");
        sparql.create(SPARQLDeserializers.nodeURI(experiments.get(0)), osWithXp);

        device = new DeviceModel();
        device.setName("DataAPITest-xp");
        sparql.create(device);

        account = new AccountModel();
        account.setEmail(new InternetAddress("DataAPITest-xp@opensilex.com"));
        sparql.create(account);

        facility = new FacilityModel();
        facility.setName("DataAPITest-facility");
        sparql.create(facility);
    }

    private static void createVariables(SPARQLService sparql) throws Exception {

        // Get variable DTO with already created components (entity, method, etc.). Only call it once in order to avoid recreation of linked objects
        VariableCreationDTO varDTO = new VariableApiTest().getCreationDto();
        VariableDAO variableDAO = new VariableDAO(sparql, getMongoDBService(), null, null);

        // create a variable of each type
        variable = variableDAO.create(varDTO.newModel()).getUri();

        varDTO.setDataType(new URI(XSD.date.getURI()));
        dateVariable = variableDAO.create(varDTO.newModel()).getUri();

        varDTO.setDataType(new URI(XSD.dateTime.getURI()));
        datetimeVariable = variableDAO.create(varDTO.newModel()).getUri();

        varDTO.setDataType(new URI(XSD.decimal.getURI()));
        decimalVariable = variableDAO.create(varDTO.newModel()).getUri();

        varDTO.setDataType(new URI(XSD.integer.getURI()));
        integerVariable = variableDAO.create(varDTO.newModel()).getUri();

        varDTO.setDataType(new URI(XSD.xboolean.getURI()));
        booleanVariable = variableDAO.create(varDTO.newModel()).getUri();

        varDTO.setDataType(new URI(XSD.xstring.getURI()));
        stringVariable = variableDAO.create(varDTO.newModel()).getUri();
    }

    private static URI createOneProvenance(String name) throws Exception {
        ProvenanceDaoV2 provenanceDaoV2 = new ProvenanceDaoV2(getMongoDBService().getServiceV2());
        ProvenanceModel model = new ProvenanceModel();
        model.setName(name);
        provenanceDaoV2.create(model);
        return model.getUri();
    }

    private static void createProvenances(List<URI> experiments) throws Exception {

        // Provenance for JSON CRUD tests
        globalProvenanceURI = createOneProvenance("DataAPITest");
        provenanceWithXP = new DataProvenanceModel();
        provenanceWithXP.setUri(globalProvenanceURI);
        provenanceWithXP.setExperiments(experiments);

        provenanceWithoutXP = new DataProvenanceModel();
        provenanceWithoutXP.setUri(globalProvenanceURI);

        // Provenances for import tests
        provenanceImportInteger = createOneProvenance("Import test : integer");
        provenanceImportAnnotation = createOneProvenance("Import test : annotation");
        provenanceImportDecimal = createOneProvenance("Import test : decimal");
        provenanceImportBoolean = createOneProvenance("Import test : boolean");
        provenanceImportString = createOneProvenance("Import test : string");
        provenanceImportDate = createOneProvenance("Import test : date");
        provenanceImportDatetime = createOneProvenance("Import test : datetime");
        provenanceImportIntegerDatatypeError = createOneProvenance("Import test : integer with datatype errors");
        provenanceImportDateDatatypeError = createOneProvenance("Import test : date with datatype errors");
        provenanceImportDatetimeDatatypeError = createOneProvenance("Import test : date with datatype errors");
    }

    public DataFileCreationDTO getCreationFileDTO() throws URISyntaxException, Exception {
        DataFileCreationDTO dataDTO = new DataFileCreationDTO();

        dataDTO.setProvenance(new DataProvenanceModel());
        dataDTO.getProvenance().setUri(globalProvenanceURI);

        dataDTO.setRdfType(new URI(Oeso.Image.toString()));
        dataDTO.setDate("2025-03-06");

        return dataDTO;
    }

    protected DataFileCreationDTO createDatafile() throws Exception{
        File file = tmpFolder.newFile("testFile.txt");
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("test".getBytes());
        }

        final FormDataMultiPart mp = new FormDataMultiPart();
        DataFileCreationDTO dto = getCreationFileDTO();

        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
        MultiPart multipart = new FormDataMultiPart().field("description", dto, MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);


        final Response postResult = getJsonPostResponseMultipart(target(DataFileAPITest.createPath), multipart);

        dto.setUri(extractUriFromResponse(postResult));
        return dto;
    }

    public DataCreationDTO getCreationDataDTO(String date) {

        DataCreationDTO dataDTO = new DataCreationDTO();

        dataDTO.setProvenance(provenanceWithXP);
        dataDTO.setVariable(variable);
        dataDTO.setTarget(osWithXp.getUri());
        dataDTO.setValue(5.56);
        dataDTO.setDate(date);

        return dataDTO;
    }

    public DataCreationDTO getCreationDataDTO(String date, URI variable, URI target, Object value, DataProvenanceModel provenance) throws URISyntaxException {
        DataCreationDTO dataDTO = new DataCreationDTO();
        dataDTO.setProvenance(provenance);
        dataDTO.setVariable(variable);
        dataDTO.setTarget(target);
        dataDTO.setValue(value);
        dataDTO.setDate(date);

        return dataDTO;
    }

    private FileDataBodyPart getImportFileBodyPart(Path filePath) throws IOException {
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
                .replaceAll(FILE_TEMPLATE_TARGET_URI, os.getUri().toString());

        FileUtils.writeStringToFile(file, fileContent, StandardCharsets.UTF_8);

        return new FileDataBodyPart(IMPORT_FILE_MULTIPART_PARAMETER_NAME, file, APPLICATION_OCTET_STREAM_TYPE);
    }

    private DataCSVValidationDTO getImportResponseAsDTO(Path fileToImport, URI provenanceUri) throws Exception {
        FileDataBodyPart bodyPart = getImportFileBodyPart(fileToImport);
        try(MultiPart multiPart = new FormDataMultiPart().bodyPart(bodyPart)){
            final Response postResult = getJsonPostResponseMultipart(
                    target(IMPORT_PATH).queryParam(IMPORT_PROVENANCE_QUERY_PARAMETER_NAME, provenanceUri.toString()),
                    multiPart);

            assertEquals(Response.Status.OK.getStatusCode(), postResult.getStatus());

            JsonNode node = postResult.readEntity(JsonNode.class);
            SingleObjectResponse<DataCSVValidationDTO> postResponse = mapper.convertValue(node, new TypeReference<>() {});
            return postResponse.getResult();
        }
    }

    private List<DataGetDTO> getSearchResponseAsDTOList(URI provenance) throws Exception {
        return getSearchResultsAsAdmin(SEARCH_PATH,
                0,
                20,
                new HashMap<>() {{
                    put(SEARCH_PROVENANCES_QUERY_PARAMETER_NAME, Collections.singletonList(provenance));
                }},
                new TypeReference<>() {
                }
        );
        /*return getSearchResultsAsAdmin(searchPath,
                0,
                20,
                new HashMap<String, Object>() {{
                    put(SEARCH_PROVENANCES_QUERY_PARAMETER_NAME, Collections.singletonList(provenance));
                }},
                new TypeReference<PaginatedListResponse<DataGetDTO>>() {
                }
        );*/
    }

    private List<AnnotationGetDTO> getSearchAnnotationsResponseAsDTOList() throws Exception {

        return getSearchResultsAsAdmin(ANNOTATION_PATH,
                0,
                20,
                new HashMap<>(),
                new TypeReference<>() {
                }
        );
    }

    @Test
    public void testCreate() throws Exception {
        // test with global OS inside XP
        var dtoWithOsTarget = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        Response postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(dtoWithOsTarget));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());

        // test with global OS
        var dtoWithGlobalOsTarget = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        dtoWithGlobalOsTarget.setTarget(os.getUri());
        dtoWithGlobalOsTarget.setProvenance(provenanceWithoutXP);
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(dtoWithGlobalOsTarget));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());

        // test with both of target
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), List.of(dtoWithOsTarget, dtoWithGlobalOsTarget));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());

        // test with facility
        var dtoWithFacilityTarget = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        dtoWithFacilityTarget.setTarget(facility.getUri());
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), List.of(dtoWithOsTarget, dtoWithGlobalOsTarget));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());

        // test with facility and experiment OS
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), List.of(dtoWithOsTarget, dtoWithFacilityTarget));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());

        // test with facility and global OS
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), List.of(dtoWithGlobalOsTarget, dtoWithFacilityTarget));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());

        // test without OS and without experiment
        var dtoWithNoTargetAndXp = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        dtoWithNoTargetAndXp.setTarget(null);
        dtoWithNoTargetAndXp.setProvenance(provenanceWithoutXP);
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), List.of(dtoWithNoTargetAndXp));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());

        // test without OS inside an experiment
        var dtoWithNoTarget = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        dtoWithNoTarget.setTarget(null);
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), List.of(dtoWithNoTarget));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());
    }

    @Test
    public void testCreateWithUnknownTargetFail() throws Exception {

        // test with unknown os from xp
        var dtoWithBadOsTarget = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        dtoWithBadOsTarget.setTarget(URI.create("os:unknown_os"));
        Response postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(dtoWithBadOsTarget));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResultData.getStatus());

        // test with unknown os from global
        var dtoWithBadGlobalOsTarget = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        dtoWithBadGlobalOsTarget.setTarget(URI.create("os:unknown_os"));
        dtoWithBadGlobalOsTarget.setProvenance(provenanceWithoutXP);
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(dtoWithBadOsTarget));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResultData.getStatus());

        // test with unknown facility
        var dtoWithBadFacilityTarget = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        dtoWithBadFacilityTarget.setTarget(URI.create("os:unknown_facility"));
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(dtoWithBadFacilityTarget));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResultData.getStatus());

        // test with unknown facility and a known OS
        var dtoWithOsTarget = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), List.of(dtoWithBadFacilityTarget, dtoWithOsTarget));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResultData.getStatus());

        // test with a known facility and an unknown OS
        var dtoWithFacilityTarget = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        dtoWithFacilityTarget.setTarget(facility.getUri());
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), List.of(dtoWithFacilityTarget, dtoWithBadOsTarget));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResultData.getStatus());

        // test with an unknown facility and an unknown OS
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), List.of(dtoWithBadFacilityTarget, dtoWithBadOsTarget));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResultData.getStatus());
    }

    @Test
    public void testCreateWithProvWithBadAgentFail() throws Exception {
        DataProvenanceModel provenanceModel = new DataProvenanceModel();
        provenanceModel.setUri(provenanceWithXP.getUri());
        ProvEntityModel provAgent = new ProvEntityModel();
        provAgent.setUri(os.getUri());
        provenanceModel.setProvWasAssociatedWith(Collections.singletonList(provAgent));

        DataCreationDTO creationDTO = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        creationDTO.setProvenance(provenanceModel);

        // Test with an OS : should fail since the OS exists but is not an agent (a device/account)
        Response postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(creationDTO));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResultData.getStatus());

        // Test with unknown device or agent
        ProvEntityModel badProvUsedDevice = new ProvEntityModel();
        badProvUsedDevice.setUri(URI.create(device.getUri().toString()+"_unknown"));
        ProvEntityModel badProvUsedAccount = new ProvEntityModel();
        badProvUsedAccount.setUri(URI.create(account.getUri().toString()+"_unknown"));
        provenanceModel.setProvWasAssociatedWith(Arrays.asList(badProvUsedDevice, badProvUsedAccount));

        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(creationDTO));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResultData.getStatus());
    }

    @Test
    public void testCreateWithInvalidVariableOrDatatypeFail() throws Exception {
        DataProvenanceModel provenanceModel = new DataProvenanceModel();
        provenanceModel.setUri(provenanceWithXP.getUri());

        // DataCreationDTO uses an unknown variable
        DataCreationDTO creationDTO = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        creationDTO.setProvenance(provenanceModel);
        creationDTO.setVariable(URI.create("os:unknown_variable"));

        Response postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(creationDTO));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResultData.getStatus());

        // Data with incompatible value type
        creationDTO = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        creationDTO.setVariable(datetimeVariable);
        creationDTO.setValue(5);
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(creationDTO));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResultData.getStatus());

        creationDTO = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        creationDTO.setVariable(stringVariable);
        creationDTO.setValue(563F);
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(creationDTO));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResultData.getStatus());

        creationDTO = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        creationDTO.setVariable(decimalVariable);
        creationDTO.setValue(563F);
        postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(creationDTO));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());
    }


    @Test
    public void testCreateWithProvAgentOK() throws Exception {

        ProvEntityModel provUsedDevice = new ProvEntityModel();
        provUsedDevice.setUri(device.getUri());
        ProvEntityModel provUsedAccount = new ProvEntityModel();
        provUsedAccount.setUri(account.getUri());

        // Create provenance with unknown agent (here an OS, not a device or account)
        DataProvenanceModel provenanceModel = new DataProvenanceModel();
        provenanceModel.setUri(provenanceWithXP.getUri());
        provenanceModel.setExperiments(provenanceWithXP.getExperiments());
        provenanceModel.setProvWasAssociatedWith(Arrays.asList(provUsedDevice, provUsedAccount));

        // DataCreationDTO uses this provenance
        DataCreationDTO creationDTO = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        creationDTO.setProvenance(provenanceModel);

        final Response postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(creationDTO));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());
    }

    @Test
    public void testCreateWithProvActivityOK() throws Exception {

        ProvEntityModel provActivity1 = new ProvEntityModel();
        provActivity1.setUri(device.getUri());
        ProvEntityModel provActivity2 = new ProvEntityModel();
        provActivity2.setUri(account.getUri());

        // Create provenance with activities
        DataProvenanceModel provenanceModel = new DataProvenanceModel();
        provenanceModel.setUri(provenanceWithXP.getUri());
        provenanceModel.setExperiments(provenanceWithXP.getExperiments());
        provenanceModel.setProvUsed(List.of(provActivity1, provActivity2));

        // DataCreationDTO uses this provenance
        DataCreationDTO creationDTO = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        creationDTO.setProvenance(provenanceModel);
        final Response postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(creationDTO));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());
    }

    @Test
    public void testCreateWithProvActivityFail() throws Exception {

        ProvEntityModel provActivity1 = new ProvEntityModel();
        provActivity1.setUri(URI.create("test:unknown-activity"));

        // Create provenance with activities
        DataProvenanceModel provenanceModel = new DataProvenanceModel();
        provenanceModel.setUri(provenanceWithXP.getUri());
        provenanceModel.setExperiments(provenanceWithXP.getExperiments());
        provenanceModel.setProvUsed(List.of(provActivity1));

        // DataCreationDTO uses this provenance
        DataCreationDTO creationDTO = getCreationDataDTO("2020-10-11T10:29:06.402+0200");
        creationDTO.setProvenance(provenanceModel);
        final Response postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(creationDTO));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResultData.getStatus());
    }

    @Test
    public void testCreateWithWrongDatafileAsProvActivityShouldFail() throws Exception {
        var datafile = createDatafile();

        var provActivityDevice = new ProvEntityModel();
        provActivityDevice.setUri(device.getUri());
        provActivityDevice.setType(device.getType());
        var provActivityDatafile = new ProvEntityModel();
        provActivityDatafile.setUri(URI.create("http://opensilex.org/wrong/datafile"));
        provActivityDatafile.setType(datafile.getRdfType());

        DataProvenanceModel provenanceModel = new DataProvenanceModel();
        provenanceModel.setUri(provenanceWithXP.getUri());
        provenanceModel.setExperiments(provenanceWithXP.getExperiments());
        provenanceModel.setProvUsed(List.of(provActivityDevice, provActivityDatafile));

        DataCreationDTO creationDTO = getCreationDataDTO("2025-03-06");
        creationDTO.setProvenance(provenanceModel);
        final Response postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(creationDTO));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResultData.getStatus());
    }

    @Test
    public void testCreateWithWrongDeviceAsProvActivityShouldFail() throws Exception {
        var datafile = createDatafile();

        var provActivityDevice = new ProvEntityModel();
        provActivityDevice.setUri(URI.create("http://opensilex.org/wrong/device"));
        provActivityDevice.setType(device.getType());
        var provActivityDatafile = new ProvEntityModel();
        provActivityDatafile.setUri(datafile.getUri());
        provActivityDatafile.setType(datafile.getRdfType());

        DataProvenanceModel provenanceModel = new DataProvenanceModel();
        provenanceModel.setUri(provenanceWithXP.getUri());
        provenanceModel.setExperiments(provenanceWithXP.getExperiments());
        provenanceModel.setProvUsed(List.of(provActivityDevice, provActivityDatafile));

        DataCreationDTO creationDTO = getCreationDataDTO("2025-03-06");
        creationDTO.setProvenance(provenanceModel);
        final Response postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(creationDTO));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResultData.getStatus());
    }

    @Test
    public void testCreateWithDatafileAndDeviceAsProvActivityOK() throws Exception {
        var datafile = createDatafile();

        var provActivityDevice = new ProvEntityModel();
        provActivityDevice.setUri(device.getUri());
        provActivityDevice.setType(device.getType());
        var provActivityDatafile = new ProvEntityModel();
        provActivityDatafile.setUri(datafile.getUri());
        provActivityDatafile.setType(datafile.getRdfType());

        DataProvenanceModel provenanceModel = new DataProvenanceModel();
        provenanceModel.setUri(provenanceWithXP.getUri());
        provenanceModel.setExperiments(provenanceWithXP.getExperiments());
        provenanceModel.setProvUsed(List.of(provActivityDevice, provActivityDatafile));

        DataCreationDTO creationDTO = getCreationDataDTO("2025-03-06");
        creationDTO.setProvenance(provenanceModel);
        final Response postResultData = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(creationDTO));
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
    public void testImportAnnotationNoTargetColumnError() throws Exception {
        DataCSVValidationDTO csvValidationDTO = getImportResponseAsDTO(FILE_PATH_IMPORT_ANNOTATION_NO_TARGET_COL, provenanceImportAnnotation);
        assertFalse(csvValidationDTO.getDataErrors().getMissingHeaders().isEmpty());

        List<DataGetDTO> importedDataList = getSearchResponseAsDTOList(provenanceImportAnnotation);
        assertEquals(0, importedDataList.size());

        List<AnnotationGetDTO> importedAnnotationsList = getSearchAnnotationsResponseAsDTOList();
        assertEquals(0, importedAnnotationsList.size());
    }
    @Test
    public void testImportAnnotationNoTargetError() throws Exception {
        DataCSVValidationDTO csvValidationDTO = getImportResponseAsDTO(FILE_PATH_IMPORT_ANNOTATION_NO_TARGET, provenanceImportAnnotation);
        assertTrue(csvValidationDTO.getDataErrors().hasErrors());

        List<DataGetDTO> importedDataList = getSearchResponseAsDTOList(provenanceImportAnnotation);
        assertEquals(0, importedDataList.size());

        List<AnnotationGetDTO> importedAnnotationsList = getSearchAnnotationsResponseAsDTOList();
        assertEquals(0, importedAnnotationsList.size());
    }

    @Test
    public void testImportAnnotationsEveryLine() throws Exception {
        DataCSVValidationDTO csvValidationDTO = getImportResponseAsDTO(FILE_PATH_IMPORT_ANNOTATION, provenanceImportAnnotation);

        assertFalse(csvValidationDTO.getDataErrors().hasErrors());
        assertEquals(3, csvValidationDTO.getDataErrors().getNbLinesImported().intValue());

        List<DataGetDTO> importedDataList = getSearchResponseAsDTOList(provenanceImportAnnotation);
        assertEquals(3, importedDataList.size());

        List<AnnotationGetDTO> importedAnnotationsList = getSearchAnnotationsResponseAsDTOList();
        assertEquals(3, importedAnnotationsList.size());
    }

    @Test
    public void testImportAnnotationsOnSomeLines() throws Exception {
        DataCSVValidationDTO csvValidationDTO = getImportResponseAsDTO(FILE_PATH_IMPORT_ANNOTATION_NOT_ON_EVERY_LINE, provenanceImportAnnotation);

        assertFalse(csvValidationDTO.getDataErrors().hasErrors());
        assertEquals(3, csvValidationDTO.getDataErrors().getNbLinesImported().intValue());

        List<DataGetDTO> importedDataList = getSearchResponseAsDTOList(provenanceImportAnnotation);
        assertEquals(3, importedDataList.size());

        List<AnnotationGetDTO> importedAnnotationsList = getSearchAnnotationsResponseAsDTOList();
        assertEquals(2, importedAnnotationsList.size());
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
        final Response postResult = getJsonPostResponseAsAdmin(target(CREATE_PATH), dtoList);

        dto.setUri(extractUriListFromPaginatedListResponse(postResult).get(0));
        dto.setValue(10.2);

        // check update ok
        final Response updateResult = getJsonPutResponse(target(UPDATE_PATH), dto);
        assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());

        final Response getResult = getJsonGetByUriResponseAsAdmin(target(URI_PATH), dto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<DataGetDTO> getResponse = mapper.convertValue(node, new TypeReference<>() {
        });
        DataGetDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(dto.getValue(), dtoFromApi.getValue());
    }

    @Test
    public void testDelete() throws Exception {

        // create object and check if URI exists
        var dto = getCreationDataDTO("2020-10-13T10:29:06.402+0200");
        Response postResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(dto));
        String uri = extractUriListFromPaginatedListResponse(postResponse).get(0).toString();

        // delete object and check if URI no longer exists
        Response delResult = getDeleteByUriResponse(target(DELETE_PATH), uri);
        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());

        Response getResult = getJsonGetByUriResponseAsAdmin(target(URI_PATH), uri);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testGetByUri() throws Exception {
        var data = getCreationDataDTO("2020-10-14T10:29:06.402+0200");

        final Response postResult = getJsonPostResponseAsAdmin(target(CREATE_PATH), Collections.singletonList(data));
        URI uri = extractUriListFromPaginatedListResponse(postResult).get(0);

        final Response getResult = getJsonGetByUriResponseAsAdmin(target(URI_PATH), uri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<DataGetDTO> getResponse = mapper.convertValue(node, new TypeReference<>() {});
        DataGetDTO dataGetDto = getResponse.getResult();
        assertNotNull(dataGetDto);
    }

    @Test
    public void testSearch() throws Exception {
        ArrayList<DataCreationDTO> dtoList = new ArrayList<>();
        DataCreationDTO creationDTO = getCreationDataDTO("2020-06-15T10:29:06.402+0200");
        dtoList.add(creationDTO);
        getJsonPostResponseAsAdmin(target(CREATE_PATH), dtoList);

        List<URI> provenances = new ArrayList<>();
        provenances.add(creationDTO.getProvenance().getUri());
        List<URI> variables = new ArrayList<>();
        variables.add(creationDTO.getVariable());

        Map<String, Object> params = new HashMap<>() {
            {
                put("start_date", "2020-06-01");
                put("end_date", "2020-06-30");
                put("provenances", provenances);
                put("variables", variables);
                put("order_by", "date=desc");
            }
        };

        WebTarget searchTarget = appendSearchParams(target(SEARCH_PATH), 0, 20, params);
        final Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        List<DataGetDTO> datas = getSearchResultsAsAdmin(SEARCH_PATH,params, new TypeReference<>() {
        });

        assertFalse(datas.isEmpty());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(AnnotationModel.class);
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(
                DataDAO.DATA_COLLECTION_NAME
        );
    }

}