package org.opensilex.core;


import org.apache.jena.shared.PrefixMapping;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.opensilex.OpenSilex;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.service.ServiceManager;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.rdf4j.RDF4JConnection;
import org.opensilex.sparql.service.SPARQLService;

import javax.mail.internet.InternetAddress;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

/**
 * @author Renaud COLIN
 *
 *  An utility class used in order to init an {@link Repository} with an {@link MemoryStore}, for unit and integration tests.
 *  This repository will then be used by the {@link OpenSilex} {@link SPARQLService}.
 */
public class OpenSilexTestContext {

    protected Repository repository;
    protected SPARQLService sparqlService;

    public OpenSilexTestContext() throws Exception {


        // create a SPARQLService based on an in-memory rdf4j connection
        repository = new SailRepository(new ShaclSail(new MemoryStore()));
        repository.init();
        sparqlService = new SPARQLService(new RDF4JConnection(repository.getConnection()));
        sparqlService.startup();

        // register the new SPARQLService
        OpenSilex.setup(new HashMap<String,String>() {{
            put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.TEST_PROFILE_ID);
        }});

        OpenSilex openSilex = OpenSilex.getInstance();
        ServiceManager serviceManager = openSilex.getServiceManager();
        serviceManager.register(SPARQLService.class, SPARQLService.DEFAULT_SPARQL_SERVICE, sparqlService);

        // only use SPARQLService default prefix mapping
        PrefixMapping prefixMapping = PrefixMapping.Factory.create().setNsPrefixes(sparqlService.getPrefixes());
        URIDeserializer.setPrefixes(prefixMapping);

        // add the admin user
        AuthenticationService authentication = openSilex.getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);
        UserDAO userDAO = new UserDAO(sparqlService, authentication);
        InternetAddress email = new InternetAddress("admin@opensilex.org");
        userDAO.create(null, email, "Admin", "OpenSilex", true, "admin");

        // exclude specials test class from the SPARQL object mapper
//        SPARQLClassObjectMapper.excludeResourceClass(NoGetterClass.class);
//        SPARQLClassObjectMapper.excludeResourceClass(NoSetterClass.class);
    }

    /**
     *
     * @return the effective {@link Repository} used by the service.
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     *
     * @return the {@link SPARQLService}  used for tests
     */
    public SPARQLService getSparqlService() {
        return sparqlService;
    }

    /**
     * @throws URISyntaxException
     * @throws SPARQLQueryException
     */
    public void clearGraphs(List<String> graphsToClear) throws URISyntaxException, SPARQLQueryException {
        for (String graphName : graphsToClear) {
            sparqlService.clearGraph(SPARQLModule.getPlatformDomainGraphURI(graphName));
        }
        sparqlService.clearGraph(SPARQLModule.getPlatformURI());
    }

    /**
     * @throws Exception
     */
    public void shutdown() throws Exception {
        sparqlService.shutdown();
    }
}
