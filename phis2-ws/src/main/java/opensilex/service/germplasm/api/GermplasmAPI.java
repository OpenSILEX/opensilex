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
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.germplasm.dal.GermplasmDAO;
import opensilex.service.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.rest.authentication.ApiCredential;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.sparql.service.SPARQLService;

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
        if (!sparql.isSubClassOf(germplasmDTO.getRdfType(), new URI(Oeso.Germplasm.getURI()))) {
            // Return error response 409 - CONFLICT if rdfType doesn't exist in the ontology
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "rdfType doesn't exist in the ontology",
                    "wrong rdfType: "+ germplasmDTO.getRdfType().toString()
            ).getResponse();
        }
        
        // check that fromAccession, fromVariety or fromSpecies are given and exist
        boolean missingLink = true;
        String message = new String();
        if (germplasmDTO.getRdfType() == new URI(Oeso.PlantMaterialLot.getURI())) {
            message = "fromAccession, fromVariety or fromSpecies";

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
                }                
            }
            
        } else if (germplasmDTO.getRdfType() == new URI(Oeso.Accession.getURI())) {
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
                }   
            }
            
        } else {
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
}
