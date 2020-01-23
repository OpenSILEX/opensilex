package integration.opensilex.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.opensilex.OpenSilex;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.security.api.AuthenticationDTO;
import org.opensilex.rest.security.api.TokenGetDTO;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.service.ServiceManager;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.rdf4j.RDF4JConnection;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;

import javax.mail.internet.InternetAddress;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Renaud COLIN
 */
public abstract class AbstractServiceTest extends RestApplicationTest {

    protected static SPARQLService service;

    @Override
    protected ResourceConfig configure() {

        ResourceConfig config = super.configure();
        try {

            // create a new SPARQLService based on a in-memory Rdf4j connection
            Repository repository = new SailRepository(new MemoryStore());
            service = new SPARQLService(new RDF4JConnection(repository.getConnection()));
            service.startup();

            // register the new SPARQLService
            OpenSilex openSilex = OpenSilex.getInstance();
            ServiceManager serviceManager = openSilex.getServiceManager();
            serviceManager.register(SPARQLService.class, "sparql", service);

            // add the admin user
            AuthenticationService authentication = openSilex.getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);
            UserDAO userDAO = new UserDAO(service, authentication);
            InternetAddress email = new InternetAddress("admin@opensilex.org");
            userDAO.create(null, email, "Admin", "OpenSilex", true, "admin");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    @AfterClass
    public static void end() throws Exception {
        service.shutdown();
    }

    /**
     *
     * @throws URISyntaxException
     * @throws SPARQLQueryException
     */
    @After
    public void clearGraph() throws URISyntaxException, SPARQLQueryException {

        for (String graphName : getGraphsToCleanNames()) {
            service.clearGraph(SPARQLModule.getPlatformDomainGraphURI(graphName));
        }
        service.clearGraph(SPARQLModule.getPlatformURI());
    }

    /**
     *
     * @return
     */
    protected abstract List<String> getGraphsToCleanNames();


    protected static ObjectMapper mapper = new ObjectMapper();

    /**
     * Call the security service and return a new Token
     *
     * @return a new Token
     */
    protected TokenGetDTO getToken() {

        AuthenticationDTO authDto = new AuthenticationDTO();
        authDto.setIdentifier("admin@opensilex.org");
        authDto.setPassword("admin");

        final Response callResult = target("/security/authenticate")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(authDto, MediaType.APPLICATION_JSON_TYPE));

        JsonNode node = callResult.readEntity(JsonNode.class);

        // need to convert according a TypeReference, because the expected SingleObjectResponse is a generic object
        SingleObjectResponse<TokenGetDTO> res = mapper.convertValue(node, new TypeReference<>() {
        });

        assertEquals(Response.Status.OK.getStatusCode(), callResult.getStatus());
        assertEquals(Response.Status.OK, res.getStatus());
        return res.getResult();
    }


    protected Response getJsonPostResponse(WebTarget target, Object entity) {
        return appendToken(target).post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    protected Response getJsonPutResponse(WebTarget target, Object entity) {
        return appendToken(target).put(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    protected Response getJsonGetByUriResponse(WebTarget target, String uri) {
        return appendToken(target.resolveTemplate("uri", uri)).get();
    }

    protected Response getDeleteByUriResponse(WebTarget target, String uri) {
        return appendToken(target.resolveTemplate("uri", uri)).delete();
    }

    protected WebTarget appendSearchParams(WebTarget target, int page, int pageSize, Map<String, Object> params) {
        return appendSearchParams(target, page, pageSize, Collections.emptyList(), params);
    }

    protected WebTarget appendSearchParams(WebTarget target, int page, int pageSize, List<OrderBy> orderByList, Map<String, Object> params) {

        target.queryParam("page", page)
                .queryParam("pageSize", pageSize)
                .queryParam("orderBy", orderByList);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            target = target.queryParam(entry.getKey(), entry.getValue());
        }
        return target;
    }

    protected Invocation.Builder appendToken(WebTarget target) {
        TokenGetDTO token = getToken();
        return target.request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + token.getToken());
    }

    protected URI extractUriFromResponse(final Response response) throws URISyntaxException {
        JsonNode node = response.readEntity(JsonNode.class);
        ObjectUriResponse postResponse = mapper.convertValue(node, new TypeReference<>() {
        });
        return new URI(postResponse.getResult());
    }

    protected List<URI> extractUriListFromResponse(final Response response) {
        JsonNode node = response.readEntity(JsonNode.class);
        PaginatedListResponse<URI> listResponse = mapper.convertValue(node, new TypeReference<>() {
        });
        return listResponse.getResult();
    }

}
