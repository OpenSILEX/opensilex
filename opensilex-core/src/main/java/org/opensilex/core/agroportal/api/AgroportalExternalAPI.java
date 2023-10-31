//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.agroportal.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.CoreModule;
import org.opensilex.core.agroportal.config.AgroportalAPIConfigDTO;
import org.opensilex.core.agroportal.dal.EntityAgroportalModel;
import org.opensilex.core.agroportal.dal.OntologyAgroportalModel;
import org.opensilex.core.variable.api.VariableAPI;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.opensilex.core.agroportal.api.AgroportalExternalAPI.PATH;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID;


/**
 * Provide communication with the AgroPortal external API
 * @author brice
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
    private CoreModule coreModule;

    @Context
    protected HttpServletRequest httpRequest;


    static final ObjectMapper mapper = new ObjectMapper();
    static final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    /**
     * Ping AgroPortal website
     * @param timeout The maximum waiting time for response
     * @return True if the service has been reached
     * @throws Exception
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
            @ApiParam(value = "Timeout", example = "1000") @QueryParam("timeout") Integer timeout
    ) throws Exception {

        AgroportalAPIConfigDTO agroportalConfig = coreModule.getAgroportalAPIConfiguration();

        boolean isReachable = getStatus(agroportalConfig.getServerPath(), timeout);
        return new SingleObjectResponse<>(isReachable).getResponse();
    }

    /**
     * Search a term in AgroPortal
     * @param namePattern A regex to filter by name
     * @param ontologies A list of ontologies acronym to filter in
     * @param orderByList (unused) A list of fields to sort
     * @param page (unused) The page index
     * @param pageSize (unused) The maximum page size
     * @return A list of {@link EntityAgroportalDTO}
     * @throws Exception
     */
    @GET
    @Path("/search")
    @ApiOperation("Search through agroportal")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return entities", response = EntityAgroportalDTO.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchThroughAgroportal(
            @ApiParam(value = "Name (regex)", example = "plant") @QueryParam("name") String namePattern ,
            @ApiParam(value = "List of ontologies (acronym)", example = "AGROVOC") @QueryParam("ontologies") String ontologies ,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @Min(0) int pageSize
    ) throws Exception {

        AgroportalAPIConfigDTO agroportalConfig = coreModule.getAgroportalAPIConfiguration();

        String url = agroportalConfig.getApiUrl() + "/search?q=" + URLEncoder.encode(namePattern, StandardCharsets.UTF_8.toString());

        if(StringUtils.isNotEmpty(ontologies)) {
            url += "&ontologies=" + ontologies;
        }

        JsonNode searchResults = jsonToNode(get(url, agroportalConfig.getApiKey())).get("collection");

        List<EntityAgroportalModel> entities = mapper.readValue(searchResults.traverse(), new TypeReference<List<EntityAgroportalModel>>(){});

        return new SingleObjectResponse<>(entities.stream().map(EntityAgroportalDTO::fromModel).collect(Collectors.toList()))
                .getResponse();
    }

    /**
     * Retrieve ontologies present in AgroPortal.
     * @param namePattern A regex to filter by ontologies acronym
     * @param ontologies A list of ontologies acronym to retrieve only these
     * @param orderByList (unused) A list of fields to sort
     * @param page (unused) The page index
     * @param pageSize (unused) The maximum page size
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
            @ApiParam(value = "Name (regex)", example = ".*") @QueryParam("name") @DefaultValue(".*") String namePattern ,
            @ApiParam(value = "List of ontologies to get (aconyms)", example = "AGROVOC, PO") @DefaultValue("") @QueryParam("ontologies") List<String> ontologies,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @Min(0) int pageSize
    ) throws Exception {

        AgroportalAPIConfigDTO agroportalConfig = coreModule.getAgroportalAPIConfiguration();

        String url = agroportalConfig.getApiUrl() + "/ontologies";

        String json = get(url, agroportalConfig.getApiKey());
        JsonNode searchResults = jsonToNode(json);

        List<OntologyAgroportalModel> ontologiesModel = mapper.readValue(searchResults.traverse(), new TypeReference<List<OntologyAgroportalModel>>(){});

        boolean isValidNamePattern = StringUtils.isNotEmpty(namePattern);
        boolean isValidOntologiesList = ontologies != null && !(String.join(",", ontologies).isEmpty());

        Pattern pattern = Pattern.compile(namePattern);

        ontologiesModel = ontologiesModel
                .stream()
                .filter(isValidOntologiesList? model -> ontologies.contains(model.getAcronym()) : model -> true)
                .filter(isValidNamePattern? model -> pattern.matcher(model.getAcronym().toLowerCase()).find() : model -> true)
                .collect(Collectors.toList());

        return new SingleObjectResponse<>(
                    ontologiesModel
                            .stream()
                            .map(OntologyAgroportalDTO::fromModel)
                            .sorted(Comparator.comparing(OntologyAgroportalDTO::getAcronym))
                            .collect(Collectors.toList())
                ).getResponse();
    }

    /**
     * Check if a URL is reachable. Return true if the connection status code is '200'
     * @param url The connection's URL
     * @param timeout The maximum waiting time for response
     * @return True if the connection has been done properly
     * @throws IOException
     */
    private static boolean getStatus(String url, int timeout) throws IOException {

        boolean result = false;
        try {
            URL urlObj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(timeout);
            con.connect();

            int code = con.getResponseCode();
            if (code == 200) {
                result = true;
            }
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    /**
     * Convert a string to a JSON node
     * @param json JSON object represented as string
     * @return A JSON node
     */
    private static JsonNode jsonToNode(String json) {
        JsonNode root = null;
        try {
            root = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return root;
    }

    /**
     * Perform a http GET method
     * @param urlToGet The url
     * @param apiKey The API key
     * @return The result message as a string
     */
    private static String get(String urlToGet, String apiKey) {
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL(urlToGet);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "apikey token=" + apiKey);
            conn.setRequestProperty("Accept", "application/json");
            rd = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
