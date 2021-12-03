//******************************************************************************
//                          MoblieModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.api;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoCommandException;
import com.mongodb.bulk.BulkWriteError;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.exception.DateMappingExceptionResponse;
import org.opensilex.core.exception.DateValidationException;
import org.opensilex.core.ontology.dal.CSVCell;
import org.opensilex.mobile.dal.CodeLotCSVValidationModel;
import org.opensilex.mobile.dal.CodeLotDAO;
import org.opensilex.mobile.dal.CodeLotModel;
import org.opensilex.mobile.dal.FormDAO;
import org.opensilex.mobile.dal.SectionModel;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.exceptions.NoSQLTooLargeSetException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.utils.ClassUtils;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * Mobile API which provides:
 *
 * - Create form
 * - Delete form
 * - Get all forms, or get forms corresponding to a list of uri's
 * - Update form
 * - Import code-lots by csv
 * </pre>
 *
 * @author Maximilian Hart
 */
@Api(MobileAPI.CREDENTIAL_MOBILE_GROUP_ID)
@Path("/mobile")
public class MobileAPI {
    
    public static final String CREDENTIAL_MOBILE_GROUP_ID = "Mobile";
    public static final String CREDENTIAL_MOBILE_GROUP_LABEL_KEY = "credential-groups.mobile";
    public static final String CREDENTIAL_MOBILE_MODIFICATION_ID = "mobile-modification";
    public static final String CREDENTIAL_MOBILE_MODIFICATION_LABEL_KEY = "credential.mobile.modification";
    
    protected final static Logger LOGGER = LoggerFactory.getLogger(MobileAPI.class);
    
    @Inject
    private MongoDBService nosql;

    /**
     * Create a form
     *
     * @param dto the Form to create
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the created Form {@link URI}
     * @throws java.lang.Exception if creation failed
     */
    @POST
    @Path("forms")
    @ApiOperation("Add a form")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Add a form", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),})
    public Response createForm(
            @ApiParam("Form to save") @NotNull @Valid FormCreationDTO dto
    ) throws Exception {
        FormDAO dao = new FormDAO(nosql);
        SectionModel createdForm = dao.create(dto.newModel());
        return new ObjectUriResponse(Response.Status.CREATED, createdForm.getUri()).getResponse();
    }

    /**
     * Get forms
     *
     * @param uris List of form uris to get, leave null to get all
     * @param orderByList How to order the returned forms
     * @param page Which page to get (see pagination system)
     * @param pageSize How many forms per page
     * @return a {@link Response} with a paginated list if successful
     * @throws java.lang.Exception if fail
     */
    @GET
    @ApiOperation("Search forms")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return form list", response = FormGetDTO.class, responseContainer = "List")
    })
    public Response searchFormList(
            @ApiParam(value = "Search by uris") @QueryParam("uris") List<URI> uris,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        FormDAO dao = new FormDAO(nosql);
        
        ListWithPagination<SectionModel> resultList = dao.search(
                uris,
                orderByList,
                page,
                pageSize
        );
        
        ListWithPagination<FormGetDTO> resultDTOList = resultList.convert(FormGetDTO.class, FormGetDTO::fromModel);
        
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     * Delete form
     *
     * @param uri , uri of the form to delete
     * @return a {@link Response}
     * @throws java.lang.Exception if fail
     */
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete form")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Form deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown Form URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteForm(
            @ApiParam(value = "Form URI", required = true) @PathParam("uri") @NotNull URI uri)
            throws Exception {
        try {
            FormDAO dao = new FormDAO(nosql);
            dao.delete(uri);
            return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown data URI ", uri);
        }
    }

    /**
     * Update form
     *
     * @param dto , form containing modifications to update
     * @return a {@link Response}
     * @throws java.lang.Exception if fail
     */
    @PUT
    @ApiProtected
    @ApiOperation("Update form")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Confidence update", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    
    public Response update(
            @ApiParam("Form description") @Valid FormUpdateDTO dto
    ) throws Exception {
        
        FormDAO dao = new FormDAO(nosql);
        
        try {
            SectionModel model = dto.newModel();
            dao.update(model);
            return new SingleObjectResponse<>(FormGetDTO.fromModel(model)).getResponse();
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown data URI ", dto.getUri());
            
        } catch (DateValidationException e) {
            return new DateMappingExceptionResponse().toResponse(e);
        }
    }

    /**
     * Get forms
     *
     * @param uris List of codeLot uris to get, leave null to get all
     * @param orderByList How to order the returned forms
     * @param page Which page to get (see pagination system)
     * @param pageSize How many forms per page
     * @return a {@link Response} with a paginated list if successful
     * @throws java.lang.Exception if fail
     */
    @GET
    @Path("/codelot")
    @ApiOperation("Search codeLots")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return codelot list", response = CodeLotGetDTO.class, responseContainer = "List")
    })
    public Response searchCodeLots(
            @ApiParam(value = "Search by uris") @QueryParam("uris") List<URI> uris,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        CodeLotDAO dao = new CodeLotDAO(nosql);

        ListWithPagination<CodeLotModel> resultList = dao.search(
                uris,
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<CodeLotGetDTO> resultDTOList = resultList.convert(CodeLotGetDTO.class, CodeLotGetDTO::fromModel);

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     * Delete codelot
     *
     * @param uri , uri of the codelot to delete
     * @return a {@link Response}
     * @throws java.lang.Exception if fail
     */
    @DELETE
    @Path("/codelot/{uri}")
    @ApiOperation("Delete codelot")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Codelot deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown Codelot URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteCodeLot(
            @ApiParam(value = "CodeLot URI", required = true) @PathParam("uri") @NotNull URI uri)
            throws Exception {
        try {
            CodeLotDAO dao = new CodeLotDAO(nosql);
            dao.delete(uri);
            return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid URI ", uri);
        }
    }


    @POST
    @Path("import")
    @ApiOperation(value = "Import a CSV file containing parent and child code-lots")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Codes were imported", response = CodeLotCSVValidationDTO.class)})
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_MOBILE_GROUP_ID,
            groupLabelKey = CREDENTIAL_MOBILE_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_MOBILE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_MOBILE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importCSVCodes(
            @ApiParam(value = "File", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {
        CodeLotDAO dao = new CodeLotDAO(nosql);

        CodeLotCSVValidationModel validation;

        validation = validateWholeCSV(file);

        CodeLotCSVValidationDTO csvValidation = new CodeLotCSVValidationDTO();

        validation.setInsertionStep(true);
        validation.setValidCSV(!validation.hasErrors());
        validation.setNbLinesToImport(validation.getData().size());

        if (validation.isValidCSV()) {
            Instant start = Instant.now();
            List<CodeLotModel> data = new ArrayList<>(validation.getData().keySet());
            try {
                dao.createAll(data);
                validation.setNbLinesImported(data.size());
            } catch (NoSQLTooLargeSetException ex) {
                validation.setTooLargeDataset(true);

            } catch (MongoBulkWriteException duplicateError) {
                List<BulkWriteError> bulkErrors = duplicateError.getWriteErrors();
                for (int i = 0; i < bulkErrors.size(); i++) {
                    int index = bulkErrors.get(i).getIndex();
                    CodeLotGetDTO fromModel = CodeLotGetDTO.fromModel(data.get(index));
                    CSVCell csvCell = new CSVCell(validation.getData().get(data.get(index)), 0, fromModel.getHead(), "cell");
                    validation.addDuplicatedDataError(csvCell);
                }
            } catch (MongoCommandException e) {
                CSVCell csvCell = new CSVCell(-1, -1, "Unknown value", "Unknown variable");
                validation.addDuplicatedDataError(csvCell);
            }
            Instant finish = Instant.now();
            long timeElapsed = Duration.between(start, finish).toMillis();
            LOGGER.debug("Insertion " + Long.toString(timeElapsed) + " milliseconds elapsed");

            validation.setValidCSV(!validation.hasErrors());
        }
        csvValidation.setCodelotErrors(validation);
        return new SingleObjectResponse<>(csvValidation).getResponse();
    }

    private final String parentHeader = "parent";
    private final String childHeader = "enfant";

    public static void main(String[] args) {
        MobileAPI api = new MobileAPI();
        try {
            File file = new File("/home/hart/opensilex-dev/opensilex-mobile/src/main/java/org/opensilex/mobile/api/csvexample.csv");
            InputStream inputStream = new FileInputStream(file);
            api.validateWholeCSV(inputStream);
            System.out.println("ddd");
        }catch(Exception e){
            System.out.println("something went wrong :/");
            System.out.println(e.getMessage());

        }

    }

    private CodeLotCSVValidationModel validateWholeCSV(InputStream file) throws Exception {
        CodeLotCSVValidationModel csvValidation = new CodeLotCSVValidationModel();
        List<CodeLotModel> allCodes = new ArrayList<>();
        Map<String, Integer> IndexByHeader = new HashMap<>();
        try (Reader inputReader = new InputStreamReader(file, StandardCharsets.UTF_8.name())) {
            CsvParserSettings csvParserSettings = ClassUtils.getCSVParserDefaultSettings();
            CsvParser csvReader = new CsvParser(csvParserSettings);
            csvReader.beginParsing(inputReader);
            LOGGER.debug("Import codelots - CSV format => \n '" + csvReader.getDetectedFormat() + "'");

            // Line 1
            String[] ids = csvReader.parseNext();
            Set<String> headers = Arrays.stream(ids).filter(Objects::nonNull).map(id -> id.toLowerCase(Locale.ENGLISH)).collect(Collectors.toSet());
            if (!headers.contains(parentHeader) && !headers.contains(childHeader)) {
                csvValidation.addMissingHeaders(Arrays.asList(parentHeader + " or " + childHeader));
            }

            // 1. check variables
            //HashMap<URI, URI> mapVariableUriDataType = new HashMap<>();
            //VariableDAO dao = new VariableDAO(sparql,nosql,fs);
            if (ids != null) {
                for (int i = 0; i < ids.length; i++) {
                    String header = ids[i];
                    if (header == null) {
                        csvValidation.addEmptyHeader(i + 1);
                    } else {

                        if (header.equalsIgnoreCase(parentHeader) || header.equalsIgnoreCase(childHeader)) {
                            IndexByHeader.put(header.toLowerCase(Locale.ENGLISH), i);

                        } else {
                            csvValidation.addInvalidHeaderURI(i, ids[i]);
                        }
                    }
                }
                // 1.1 return error variables
                if (csvValidation.hasErrors()) {
                    return csvValidation;
                }
                csvValidation.setHeadersFromArray(ids);

                int rowIndex = 0;
                String[] values;

                // Line 2 This doesnt apply to me? i have no headers labels, what are they?
//                String[] headersLabels = csvReader.parseNext();
//                csvValidation.setHeadersLabelsFromArray(headersLabels);
                // Line 3
                //csvReader.parseNext();
                // Line 4
                boolean validateCSVRow = false;
                while ((values = csvReader.parseNext()) != null) {
                    try {
                        validateCSVRow = validateCSVRow(
                                values,
                                rowIndex,
                                csvValidation,
                                IndexByHeader.get(parentHeader),
                                IndexByHeader.get(childHeader),
                                allCodes
                                );
                    } catch (Exception e) {
                        System.out.println("Something went wrong with row call");
                    }
                    rowIndex++;
                }
            }
        }
        return csvValidation;
    }

    private static class BackPropagationObject {
        final int steps;
        final boolean backPropagationPossible;

        public BackPropagationObject(int i, boolean b) {
            steps = i;
            backPropagationPossible = b;
        }
    }

    private BackPropagationObject isBackPropagationPossible(CodeLotModel currentNode, CodeLotModel endNode, int steps) {
        if (currentNode == endNode) {
            return new BackPropagationObject(steps, true);
        } else {
            List<CodeLotModel> parents = currentNode.getParents();
            int branchIndex = 0;
            BackPropagationObject currentBranch = new BackPropagationObject(steps, false);
            while (branchIndex < parents.size() && (!currentBranch.backPropagationPossible)) {
                currentBranch = isBackPropagationPossible(parents.get(branchIndex), endNode, steps+1);
                branchIndex++;
            }
            return currentBranch;
        }
    }

    private boolean validateCSVRow(
            String[] values,
            int rowIndex,
            CodeLotCSVValidationModel csvValidation,
            int parentColumnIndex,
            int childColumnIndex,
            List<CodeLotModel> allCodes
    ){
        boolean validRow = true;
        String parent = null;
        String child = null;
        for (int colIndex = 0; colIndex < values.length; colIndex++) {
            if (colIndex == parentColumnIndex) {
                parent = values[colIndex];
            } else if (colIndex == childColumnIndex) {
                child = values[colIndex];
            }
        }
        CSVCell errorCell = new CSVCell(rowIndex, childColumnIndex, child, "enfant");
        //No parent validation
        if (parent == null && child != null) {
            csvValidation.addNoParentError(errorCell);
            validRow = false;
        } else {
            if (parent == null && child == null) {
                validRow = false;
            } else {
                //parent is not null:
                //Start by testing that they are not equal if child is not null:
                boolean childNotNull = !(child == null);
                if (childNotNull && parent.equals(child)) {
                    csvValidation.addBoucleError(errorCell);
                    return false;
                }
                //Fetch the code corresponding to parent and child if not null:
                CodeLotModel parentCode = new CodeLotModel(parent);
                CodeLotModel childCode = new CodeLotModel(child);
                boolean foundParent = false;
                boolean foundChild = false;
                int i = 0;
                while ((!(foundParent && (!childNotNull || foundChild))) && i < allCodes.size()) {
                    CodeLotModel currentCode = allCodes.get(i);
                    if (currentCode.getHead().equals(parent)) {
                        foundParent = true;
                        parentCode = currentCode;
                    }
                    if (childNotNull && currentCode.getHead().equals(child)) {
                        foundChild = true;
                        childCode = currentCode;
                    }
                    i++;
                }

                if (!foundParent) {
                    if (foundChild) {
                        parentCode.addChild(childCode);
                        childCode.addParent(parentCode);
                    }
                    allCodes.add(parentCode);
                }
                if (childNotNull && !foundChild) {
                    childCode.addParent(parentCode);
                    allCodes.add(childCode);
                    parentCode.addChild(childCode);
                }
                if (foundParent && foundChild) {
                    //Run tests as this is the only case where problems can occur:
                    //Linked sibbling test:
                    for (CodeLotModel parentOfParent : parentCode.getParents()) {
                        if (parentOfParent.containsChild(child)) {
                            csvValidation.addLinkedSibblingError(errorCell);
                            return false;
                        }
                    }
                    //Shortcircuit test:
                    BackPropagationObject shortcircuitTest = isBackPropagationPossible(childCode, parentCode, 0);
                    if (shortcircuitTest.backPropagationPossible && shortcircuitTest.steps > 1) {
                        csvValidation.addShortCircuitError(errorCell);
                        return false;
                    } else {
                        //Boucle test:
                        BackPropagationObject boucleTest = isBackPropagationPossible(parentCode, childCode, 0);
                        if (boucleTest.backPropagationPossible) {
                            csvValidation.addBoucleError(errorCell);
                            return false;
                        } else {
                            parentCode.addChild(childCode);
                            childCode.addParent(parentCode);
                        }
                    }
                }
                if (!(foundParent && (!foundChild) && child == null)) {
                    csvValidation.addData(childCode, rowIndex);
                    csvValidation.addData(parentCode, rowIndex);
                }
            }
        }
        return validRow;
    }
}
