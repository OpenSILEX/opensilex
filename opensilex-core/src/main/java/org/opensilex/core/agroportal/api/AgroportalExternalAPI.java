//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.agroportal.api;

import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.external.agroportal.AgroportalService;
import org.opensilex.core.external.agroportal.AgroportalTermModel;
import org.opensilex.core.external.agroportal.OntologyAgroportalModel;
import org.opensilex.core.variable.api.VariableAPI;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.SingleObjectResponse;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.opensilex.core.agroportal.api.AgroportalExternalAPI.PATH;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID;


/**
 * Provide communication with the AgroPortal external API
 *
 * @author Brice Maussang
 */
@Api(AgroportalExternalAPI.CREDENTIAL_AGROPORTAL_GROUP_ID)
@Path(PATH)
@ApiCredentialGroup(
        groupId = CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class AgroportalExternalAPI {

    public static final String CREDENTIAL_AGROPORTAL_GROUP_ID = "Agroportal API";

    public static final String PATH = "/core/agroportal";

    @Inject
    private AgroportalService agroportalService;

    /**
     * Ping AgroPortal website
     *
     * @param timeout The maximum waiting time for response
     * @return True if the service has been reached
     */
    @GET
    @Path("/ping")
    @ApiOperation("Ping agroportal server")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Agroportal status", response = Boolean.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response pingAgroportal(
            @ApiParam(value = "Timeout", example = "1000") @QueryParam("timeout") Long timeout
    ) throws Exception {
        return new SingleObjectResponse<>(agroportalService.ping(timeout)).getResponse();
    }

    /**
     * Search a term in AgroPortal
     *
     * @param namePattern A regex to filter by name
     * @param ontologies  A list of ontologies acronym to filter in
     * @return A list of {@link AgroportalTermDTO}
     */
    @GET
    @Path("/search")
    @ApiOperation("Search through agroportal")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return entities", response = AgroportalTermDTO.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchThroughAgroportal(
            @ApiParam(value = "Name (regex)", example = "plant") @QueryParam("name") String namePattern,
            @ApiParam(value = "List of ontologies (acronym)", example = "AGROVOC") @QueryParam("ontologies") List<String> ontologies
    ) throws Exception {
        List<AgroportalTermModel> terms = agroportalService.search(namePattern, ontologies)
                .getCollection();

        return new SingleObjectResponse<>(terms.stream().map(AgroportalTermDTO::fromModel).collect(Collectors.toList()))
                .getResponse();
    }

    /**
     * Retrieve ontologies present in AgroPortal.
     *
     * @param namePattern A regex to filter by ontologies acronym
     * @param ontologies  A list of ontologies acronym to retrieve only these
     * @return A list of {@link OntologyAgroportalDTO}
     * @throws Exception
     */
    @GET
    @Path("/ontologies")
    @ApiOperation("Get ontologies from agroportal")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return ontologies", response = OntologyAgroportalDTO.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAgroportalOntologies(
            @ApiParam(value = "Name (regex)", example = ".*") @QueryParam("name") @DefaultValue(".*") String namePattern,
            @ApiParam(value = "List of ontologies to get (acronyms)", example = "AGROVOC") @DefaultValue("") @QueryParam("ontologies") List<String> ontologies
    ) throws Exception {
        List<OntologyAgroportalModel> ontologiesModel = agroportalService.getOntologies();

        boolean isValidNamePattern = StringUtils.isNotEmpty(namePattern);
        boolean isValidOntologiesList = ontologies != null && !(String.join(",", ontologies).isEmpty());

        Pattern pattern = Pattern.compile(namePattern);

        ontologiesModel = ontologiesModel
                .stream()
                .filter(isValidOntologiesList ? model -> ontologies.contains(model.getAcronym()) : model -> true)
                .filter(isValidNamePattern ? model -> pattern.matcher(model.getAcronym().toLowerCase()).find() : model -> true)
                .collect(Collectors.toList());

        return new SingleObjectResponse<>(
                ontologiesModel
                        .stream()
                        .map(OntologyAgroportalDTO::fromModel)
                        .sorted(Comparator.comparing(OntologyAgroportalDTO::getAcronym))
                        .collect(Collectors.toList())
        ).getResponse();
    }
}
