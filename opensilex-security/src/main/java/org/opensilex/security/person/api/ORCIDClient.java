package org.opensilex.security.person.api;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.opensilex.server.exceptions.BadGatewayException;
import org.opensilex.server.exceptions.NotFoundException;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.UnknownHostException;

/**
 * web client used to get data from the ORCID API. It only uses the public API without credentials.
 */
public class ORCIDClient {

    /**
     * this method check if the connection with ORCID API is possible. It checks 3 cases :
     * - Server does not have a connection to internet.
     * - DNS address (domain) https://orcid.org is not attributed.
     * - ORCID servers has internal problem, and so return a 5XX response.
     */
    public void assertOrcidConnexionIsOk() {
       try(Response response = sendGetRequest("https://orcid.org/0000-0000-0000-0000")){

            if (Response.Status.Family.familyOf(response.getStatus()).equals(Response.Status.Family.SERVER_ERROR)) {
                throw new BadGatewayException("An error occurred on the ORCID API server when tried to reach the service. Please contact your administrator for further information");
            }
        }catch (ProcessingException e) {
            if (e.getCause() instanceof UnknownHostException) {
                throw new BadGatewayException("An error occurred when tried to reach the ORCID API. The ORCID API may not function or your server connection is lost");
            }
        }
    }

    /**
     * @param orcid is the ORCID ID without the website prefix ex : 0000-0000-0000-0000
     * @return data found on the ORCIDAPI
     * @throws NotFoundException            if the ORCID doesn't exist on their API. We don't use {@link #orcidExists(String)} method to avoid sending multiple request.
     * @throws InternalServerErrorException if the request fail for another reason
     */
    public OrcidRecordDTO getRecord(String orcid) throws NotFoundException, InternalServerErrorException {

        Response response = this.sendGetRequest("https://pub.orcid.org/v3.0/" + orcid + "/record");


        if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            throw new NotFoundException("orcid not found by ORCID API : " + orcid);
        }
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new BadGatewayException("GET request to ORCID API failed");
        }

        String objectString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(objectString));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return new OrcidRecordDTO(object);
    }

    /**
     * @param orcid is the ORCID ID without the website prefix ex : 0000-0000-0000-0000
     * @return true if the ID exists in the ORCID API, false otherwise
     */
    public boolean orcidExists(String orcid) {
        try(Response response = sendGetRequest("https://orcid.org/" + orcid)) {
            if (response.getStatus() != 200 && response.getStatus() != 404){
                throw new BadGatewayException("GET request to ORCID API failed");
            }
            return response.getStatus() == 200;
        }
    }

    /**
     * send a GET HTTP request with the defined url and add a {accept: application/json} header
     * @param url you want to request for
     * @return the response of the HTTP request
     */
    protected Response sendGetRequest(String url) {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(url);

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON_TYPE);
        return invocationBuilder.get();
    }

}
