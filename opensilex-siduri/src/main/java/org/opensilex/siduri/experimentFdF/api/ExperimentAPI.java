// //******************************************************************************
// //                          ExperimentAPI.java
// // OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// // Copyright © INRAE 2020
// // Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
// //******************************************************************************
// package org.opensilex.core.experiment.api;

// import com.mongodb.MongoBulkWriteException;
// import com.mongodb.MongoCommandException;
// import com.mongodb.bulk.BulkWriteError;
// import com.opencsv.CSVWriter;
// import com.univocity.parsers.csv.CsvParser;
// import com.univocity.parsers.csv.CsvParserSettings;
// import io.swagger.annotations.*;
// import org.apache.commons.lang3.StringUtils;
// import org.bson.Document;
// import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
// import org.glassfish.jersey.media.multipart.FormDataParam;
// import org.opensilex.core.data.api.DataAPI;
// import org.opensilex.core.data.api.DataCSVValidationDTO;
// import org.opensilex.core.data.api.DataGetDTO;
// import org.opensilex.core.data.api.DataGetSearchDTO;
// import org.opensilex.core.data.bll.DataLogic;
// import org.opensilex.core.data.dal.*;
// import org.opensilex.core.data.utils.DataValidateUtils;
// import org.opensilex.core.data.utils.ParsedDateTimeMongo;
// import org.opensilex.core.exception.*;
// import org.opensilex.core.experiment.dal.ExperimentDAO;
// import org.opensilex.core.experiment.dal.ExperimentModel;
// import org.opensilex.core.experiment.dal.ExperimentSearchFilter;
// import org.opensilex.core.experiment.factor.api.FactorDetailsGetDTO;
// import org.opensilex.core.experiment.factor.dal.FactorDAO;
// import org.opensilex.core.experiment.factor.dal.FactorModel;
// import org.opensilex.core.experiment.utils.ImportDataIndex;
// import org.opensilex.core.organisation.api.facility.FacilityGetDTO;
// import org.opensilex.core.organisation.dal.facility.FacilityModel;
// import org.opensilex.core.provenance.api.ProvenanceAPI;
// import org.opensilex.core.provenance.api.ProvenanceGetDTO;
// import org.opensilex.core.provenance.dal.ProvenanceDAO;
// import org.opensilex.core.provenance.dal.ProvenanceModel;
// import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
// import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
// import org.opensilex.core.species.api.SpeciesDTO;
// import org.opensilex.core.species.dal.SpeciesDAO;
// import org.opensilex.core.species.dal.SpeciesModel;
// import org.opensilex.core.variable.dal.VariableDAO;
// import org.opensilex.core.variable.dal.VariableModel;
// import org.opensilex.fs.service.FileStorageService;
// import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
// import org.opensilex.nosql.exceptions.NoSQLTooLargeSetException;
// import org.opensilex.nosql.mongodb.MongoDBService;
// import org.opensilex.security.account.dal.AccountDAO;
// import org.opensilex.security.account.dal.AccountModel;
// import org.opensilex.security.authentication.ApiCredential;
// import org.opensilex.security.authentication.ApiCredentialGroup;
// import org.opensilex.security.authentication.ApiProtected;
// import org.opensilex.server.exceptions.NotFoundURIException;
// import org.opensilex.security.authentication.injection.CurrentUser;
// import org.opensilex.security.user.api.UserGetDTO;
// import org.opensilex.server.response.*;
// import org.opensilex.server.rest.validation.ValidURI;
// import org.opensilex.sparql.csv.CSVCell;
// import org.opensilex.sparql.deserializer.URIDeserializer;
// import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
// import org.opensilex.sparql.response.CreatedUriResponse;
// import org.opensilex.sparql.response.NamedResourceDTO;
// import org.opensilex.sparql.service.SPARQLService;
// import org.opensilex.utils.ClassUtils;
// import org.opensilex.utils.ListWithPagination;
// import org.opensilex.utils.OrderBy;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import javax.inject.Inject;
// import javax.validation.Valid;
// import javax.validation.constraints.Max;
// import javax.validation.constraints.Min;
// import javax.validation.constraints.NotNull;
// import javax.ws.rs.*;
// import javax.ws.rs.core.MediaType;
// import javax.ws.rs.core.Response;
// import java.io.InputStream;
// import java.io.InputStreamReader;
// import java.io.Reader;
// import java.io.StringWriter;
// import java.net.URI;
// import java.net.URISyntaxException;
// import java.nio.charset.StandardCharsets;
// import java.time.Duration;
// import java.time.Instant;
// import java.time.LocalDate;
// import java.time.format.DateTimeFormatter;
// import java.time.zone.ZoneRulesException;
// import java.util.*;
// import java.util.stream.Collectors;

// import static org.opensilex.core.data.api.DataAPI.*;

// /**
//  * @author Vincent MIGOT
//  * @author Renaud COLIN
//  * @author Julien BONNEFONT
//  */
// @Api(ExperimentAPI.CREDENTIAL_EXPERIMENT_GROUP_ID)
// @Path(ExperimentAPI.PATH)
// @ApiCredentialGroup(
//         groupId = ExperimentAPI.CREDENTIAL_EXPERIMENT_GROUP_ID,
//         groupLabelKey = ExperimentAPI.CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY
// )
// public class ExperimentAPI {

//     Logger LOGGER = LoggerFactory.getLogger(ExperimentAPI.class);

//     public static final String PATH = "/core/experiments";
//     public static final String CREDENTIAL_EXPERIMENT_GROUP_ID = "Experiments";
//     public static final String CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY = "credential-groups.experiments";

//     public static final String CREDENTIAL_EXPERIMENT_MODIFICATION_ID = "experiment-modification";
//     public static final String CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY = "credential.default.modification";

//     public static final String CREDENTIAL_EXPERIMENT_DELETE_ID = "experiment-delete";
//     public static final String CREDENTIAL_EXPERIMENT_DELETE_LABEL_KEY = "credential.default.delete";

//     public static final String EXPERIMENT_EXAMPLE_URI = "http://opensilex/experiment/id/ZA17";
//     public static final String EXPERIMENT_API_VALUE = "Experiment URI";



//     /**
//      * Search experiments
//      *
//      * @param name
//      * @param year
//      * @param isEnded
//      * @param species
//      * @param factorCategories
//      * @param projects
//      * @param isPublic
//      * @param isFdF
//      * @param facilities
//      * @param orderByList
//      * @param page
//      * @param pageSize
//      * @return filtered, ordered and paginated list
//      * @throws java.lang.Exception
//      * @see ExperimentDAO
//      */
//     @GET
//     @ApiOperation("Search experiments")
//     @ApiProtected
//     @Consumes(MediaType.APPLICATION_JSON)
//     @Produces(MediaType.APPLICATION_JSON)
//     @ApiResponses(value = {
//         @ApiResponse(code = 200, message = "Return experiments", response = ExperimentGetListDTO.class, responseContainer = "List")
//     })
//     public Response searchExperiments(
//             @ApiParam(value = "Regex pattern for filtering by name", example = "ZA17") @QueryParam("name") String name,
//             @ApiParam(value = "Search by year", example = "2017") @QueryParam("year") Integer year,
//             @ApiParam(value = "Search ended(false) or active experiments(true)") @QueryParam("is_ended") Boolean isEnded,
//             @ApiParam(value = "Search by involved species", example = "http://www.phenome-fppn.fr/id/species/zeamays") @QueryParam("species") List<URI> species,
//             @ApiParam(value = "Search by studied effect", example = "http://purl.obolibrary.org/obo/CHEBI_25555") @QueryParam("factors") List<URI> factorCategories,
//             @ApiParam(value = "Search by related project uri", example = "http://www.phenome-fppn.fr/projects/ZA17\nhttp://www.phenome-fppn.fr/id/projects/ZA18") @QueryParam("projects") List<URI> projects,
//             @ApiParam(value = "Search private(false) or public experiments(true)") @QueryParam("is_public") Boolean isPublic,
//             @ApiParam(value = "Search only Ferments du Futur experiments (true)") @QueryParam("is_FdF") Boolean isFdF,
//             @ApiParam(value = "Search by involved facilities") @QueryParam("facilities") List<URI> facilities,
//             @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
//             @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
//             @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
//     ) throws Exception {
//         ExperimentDAO xpDao = new ExperimentDAO(sparql, nosql);

//         ExperimentSearchFilter filter = new ExperimentSearchFilter()
//                 .setYear(year)
//                 .setName(name)
//                 .setSpecies(species)
//                 .setFactorCategories(factorCategories)
//                 .setEnded(isEnded)
//                 .setProjects(projects)
//                 .setPublic(isPublic)
//                 .setFdF(isFdF)
//                 .setFacilities(facilities)
//                 .setUser(currentUser);

//         filter.setOrderByList(orderByList)
//                 .setPage(page)
//                 .setPageSize(pageSize);

//         ListWithPagination<ExperimentModel> resultList = xpDao.search(filter);

//         // Convert paginated list to DTO
//         ListWithPagination<ExperimentGetListDTO> resultDTOList = resultList.convert(ExperimentGetListDTO.class, ExperimentGetListDTO::fromModel);
//         return new PaginatedListResponse<>(resultDTOList).getResponse();
//     }
// }