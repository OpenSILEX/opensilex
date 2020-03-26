/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.germplasm.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.germplasm.dal.GermplasmDAO;
import opensilex.service.germplasm.dal.GermplasmModel;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.Triple;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.rest.authentication.ApiCredential;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.validation.ValidURI;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author boizetal
 */
@Api(GermplasmAPI.CREDENTIAL_GERMPLASM_GROUP_ID)
@Path("/phis/germplasm")
public class GermplasmAPI {
    
    public static final String CREDENTIAL_GERMPLASM_GROUP_ID = "Germplasm";
    public static final String CREDENTIAL_GERMPLASM_GROUP_LABEL_KEY = "credential-groups.germplasm";
    
    public static final String CREDENTIAL_GERMPLASM_MODIFICATION_ID = "germplasm-modification";
    public static final String CREDENTIAL_GERMPLASM_MODIFICATION_LABEL_KEY = "credential.germplasm.modification";

    public static final String CREDENTIAL_GERMPLASM_READ_ID = "germplasm-read";
    public static final String CREDENTIAL_GERMPLASM_READ_LABEL_KEY = "credential.germplasm.read";

    public static final String CREDENTIAL_GERMPLASM_DELETE_ID = "experiment-delete";
    public static final String CREDENTIAL_GERMPLASM_DELETE_LABEL_KEY = "credential.germplasm.delete";

    protected static final String GERMPLASM_EXAMPLE_URI = "http://opensilex/set/experiments/ZA17";
    protected static final String GERMPLASM_EXAMPLE_TYPE = "http://www.opensilex.org/vocabulary/oeso#variety";
    protected static final String GERMPLASM_EXAMPLE_SPECIES = "http://www.phenome-fppn.fr/id/species/zeamays";
    protected static final String GERMPLASM_EXAMPLE_VARIETY = "";
    protected static final String GERMPLASM_EXAMPLE_ACCESSION = "";
    
    @Inject
    public GermplasmAPI(SPARQLService sparql) {
        this.sparql = sparql;
    }
    
    private final SPARQLService sparql;
    
    @POST
    @Path("create")
    @ApiOperation("Create a germplasm")
    @ApiProtected
    @ApiCredential(
        groupId = CREDENTIAL_GERMPLASM_GROUP_ID,
        groupLabelKey = CREDENTIAL_GERMPLASM_GROUP_LABEL_KEY,
        credentialId = CREDENTIAL_GERMPLASM_MODIFICATION_ID,
        credentialLabelKey = CREDENTIAL_GERMPLASM_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create a germplasm (variety, accession, plantMaterialLot)", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 409, message = "A germplasm with the same URI already exists", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    
    public Response createGermplasm(
        @ApiParam("Germplasm description") @Valid GermplasmCreationDTO germplasmDTO
    ) throws Exception {
 
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql);

        // check if germplasm URI already exists
        if (sparql.uriExists(GermplasmModel.class, germplasmDTO.getUri())) {
            // Return error response 409 - CONFLICT if URI already exists
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Germplasm URI already exists",
                    "Duplicated URI: " + germplasmDTO.getUri()
            ).getResponse();
        }
        
        // check if germplasm label already exists
        if (germplasmDAO.germplasmLabelExists(germplasmDTO.getLabel())) {
            // Return error response 409 - CONFLICT if label already exists
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Germplasm label already exists",
                    "Duplicated label: " + germplasmDTO.getUri()
            ).getResponse();
        }
        
        // check rdfType
        if (!germplasmDAO.isGermplasmType(germplasmDTO.getRdfType())) {
            // Return error response 409 - CONFLICT if rdfType doesn't exist in the ontology
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "rdfType doesn't exist in the ontology",
                    "wrong rdfType: "+ germplasmDTO.getRdfType().toString()
            ).getResponse();
        } else {
        }
        
        // check that fromAccession, fromVariety or fromSpecies are given and exist
        boolean missingLink = true;
        String message = new String();
        if (germplasmDAO.isPlantMaterialLot(germplasmDTO.getRdfType())) {
            message = "fromAccession, fromVariety or fromSpecies";

            if (germplasmDTO.getFromSpecies() != null) {
                missingLink = false;
                if (!sparql.uriExists(new URI(Oeso.Species.getURI()), germplasmDTO.getFromSpecies())) {
                    // Return error response 409 - CONFLICT if species doesn't exist in the DB
                    return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "The given species doesn't exist in the database",
                            "unknown species : " + germplasmDTO.getFromSpecies().toString()
                    ).getResponse();
                }   
            }
            
            if (germplasmDTO.getFromVariety() != null) {
                missingLink = false;
                if (!sparql.uriExists(new URI(Oeso.Variety.getURI()), germplasmDTO.getFromVariety())) {
                    // Return error response 409 - CONFLICT if variety doesn't exist in the DB
                    return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "The given variety doesn't exist in the database",
                            "unknown variety : " + germplasmDTO.getFromVariety().toString()
                    ).getResponse();
                } else {
                    GermplasmDAO varietyDAO = new GermplasmDAO(sparql);
                    GermplasmModel variety = varietyDAO.get(germplasmDTO.getFromVariety());
                    germplasmDTO.setFromSpecies(variety.getSpecies().getUri());
                }   
            }            
            
            if (germplasmDTO.getFromAccession() != null) {
                missingLink = false;
                if (!sparql.uriExists(new URI(Oeso.Accession.getURI()), germplasmDTO.getFromAccession())) {
                    // Return error response 409 - CONFLICT if accession doesn't exist in the DB
                    return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "The given accession doesn't exist in the database",
                            "unknown accession : " + germplasmDTO.getFromAccession().toString()
                    ).getResponse();
                } else {
                    GermplasmDAO accessionDAO = new GermplasmDAO(sparql);
                    GermplasmModel accession = accessionDAO.get(germplasmDTO.getFromAccession());
                    germplasmDTO.setFromVariety(accession.getVariety().getUri());
                    germplasmDTO.setFromSpecies(accession.getSpecies().getUri());
                }                
            }
            
        } else if (germplasmDTO.getRdfType().toString().equals(Oeso.Accession.getURI())) {
            message = "fromVariety or fromSpecies";   
            if (germplasmDTO.getFromSpecies()!= null) {
                missingLink = false;
                if (!sparql.uriExists(new URI(Oeso.Species.getURI()), germplasmDTO.getFromSpecies())) {
                    // Return error response 409 - CONFLICT if species doesn't exist in the DB
                    return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "The given species doesn't exist in the database",
                            "unknown species : " + germplasmDTO.getFromSpecies().toString()
                    ).getResponse();
                }   
            }
            if (germplasmDTO.getFromVariety() != null) {
                missingLink = false;
                if (!sparql.uriExists(new URI(Oeso.Variety.getURI()), germplasmDTO.getFromVariety())) {
                    // Return error response 409 - CONFLICT if variety doesn't exist in the DB
                    return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "The given variety doesn't exist in the database",
                            "unknown variety : " + germplasmDTO.getFromVariety().toString()
                    ).getResponse();
                } else {
                    GermplasmDAO varietyDAO = new GermplasmDAO(sparql);
                    GermplasmModel variety = varietyDAO.get(germplasmDTO.getFromVariety());
                    germplasmDTO.setFromSpecies(variety.getSpecies().getUri());                
                }   
            }
            
        } else if (germplasmDTO.getRdfType().toString().equals(Oeso.Variety.getURI())) {
            message = "fromSpecies";
            if (germplasmDTO.getFromSpecies()!= null) {
                missingLink = false;
                if (!sparql.uriExists(new URI(Oeso.Species.getURI()), germplasmDTO.getFromSpecies())) {
                    // Return error response 409 - CONFLICT if species doesn't exist in the DB
                    return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "The given species doesn't exist in the database",
                            "unknown species : " + germplasmDTO.getFromSpecies().toString()
                    ).getResponse();
                }  
            }
        } else {
            missingLink = false;
        }
        
        if (missingLink) {
            // Return error response 409 - CONFLICT if link fromSpecies, fromVariety or fromAccession is missing
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "missing attribute",
                    "you have to fill " + message
            ).getResponse();
        }               
        

        // create new germplasm
        GermplasmModel germplasm = germplasmDAO.create(
                germplasmDTO.getUri(),
                germplasmDTO.getLabel(),
                germplasmDTO.getRdfType(),
                germplasmDTO.getFromSpecies(),
                germplasmDTO.getFromVariety(),
                germplasmDTO.getFromAccession()
        );
        //return germplasm uri
        return new ObjectUriResponse(Response.Status.CREATED, germplasm.getUri()).getResponse();
        
    }
    
    @GET
    @Path("get/{uri}")
    @ApiOperation("Get a germplasm by its URI")
    @ApiProtected
    @ApiCredential(
        groupId = CREDENTIAL_GERMPLASM_GROUP_ID,
        groupLabelKey = CREDENTIAL_GERMPLASM_GROUP_LABEL_KEY,
        credentialId = CREDENTIAL_GERMPLASM_READ_ID,
        credentialLabelKey = CREDENTIAL_GERMPLASM_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return profile", response = GermplasmGetDTO.class),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Germplasm not found", response = ErrorDTO.class)
    })
    public Response getGermplasm(
            @ApiParam(value = "germplasm URI", example = "dev-users:Admin_OpenSilex", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        // Get germplasm from DAO by URI
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql);
        GermplasmModel model = germplasmDAO.get(uri);

        // Check if germplasm is found
        if (model != null) {
            // Return GermplasmGetDTO
            return new SingleObjectResponse<>(
                    GermplasmGetDTO.fromModel(model)
            ).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Germplasm not found",
                    "Unknown germplasm URI: " + uri.toString()
            ).getResponse();
        }
    }
    
    @GET
    @Path("search")
    @ApiOperation("Search germplasm")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GERMPLASM_GROUP_ID,
            groupLabelKey = CREDENTIAL_GERMPLASM_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_GERMPLASM_READ_ID,
            credentialLabelKey = CREDENTIAL_GERMPLASM_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return germplasm list", response = GermplasmGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchGermplasmList(
            @ApiParam(value = "Search by uri", example = GERMPLASM_EXAMPLE_URI) @QueryParam("uri") URI uri,
            @ApiParam(value = "Search by type", example = GERMPLASM_EXAMPLE_TYPE) @QueryParam("type") URI rdfType,
            @ApiParam(value = "Regex pattern for filtering list by name", example = ".*") @DefaultValue(".*") @QueryParam("label") String label,
            @ApiParam(value = "Search by species", example = GERMPLASM_EXAMPLE_SPECIES) @QueryParam("species") URI species,
            @ApiParam(value = "Search by variety", example = GERMPLASM_EXAMPLE_VARIETY) @QueryParam("variety") URI variety,
            @ApiParam(value = "Search by accession", example = GERMPLASM_EXAMPLE_ACCESSION) @QueryParam("accession") URI accession,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "label=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        // Search germplasm with germplasm DAO
        GermplasmDAO dao = new GermplasmDAO(sparql);
        ListWithPagination<GermplasmModel> resultList = dao.search(
                uri,
                rdfType,
                label,
                species,
                variety,
                accession,
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<GermplasmGetDTO> resultDTOList = resultList.convert(
                GermplasmGetDTO.class,
                GermplasmGetDTO::fromModel
        );

        // Return paginated list of profiles DTO
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
    
    //@DELETE
    //@Path("delete/{uri}")
    @ApiOperation("Delete a germplasm")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GERMPLASM_GROUP_ID,
            groupLabelKey = CREDENTIAL_GERMPLASM_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_GERMPLASM_DELETE_ID,
            credentialLabelKey = CREDENTIAL_GERMPLASM_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteProfile(
            @ApiParam(value = "Germplasm URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        GermplasmDAO dao = new GermplasmDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

}
