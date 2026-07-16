package org.opensilex.deref;

import org.apache.catalina.startup.Tomcat;
import org.opensilex.OpenSilexModule;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.server.Server;
import org.opensilex.server.ServerConfig;
import org.opensilex.server.ServerModule;
import org.opensilex.server.extensions.ServerExtension;
import org.opensilex.server.scanner.IgnoreJarScanner;
import org.opensilex.sparql.SPARQLModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class DerefModule extends OpenSilexModule implements ServerExtension {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String getApplicationPathPrefix() {
        try {
            var cfg = getOpenSilex().getModuleConfig(ServerModule.class, ServerConfig.class);
            return cfg.pathPrefix();
        } catch (OpenSilexModuleNotFoundException ex) {
            return "";
        }
    }

    private URI getGenerationBaseUri() throws OpenSilexModuleNotFoundException {
        return getOpenSilex().getModuleByClass(SPARQLModule.class).getGenerationPrefixURI();
    }

    private String getServerPublicUri() throws OpenSilexModuleNotFoundException {
        return getOpenSilex().getModuleByClass(ServerModule.class).getBaseURL();
    }

    /**
     * @param server Unstarted server instance
     * @throws IllegalStateException if the base generation URI does not start with the instance public URI
     */
    @Override
    public void initServer(Server server) throws Exception {
        ServerExtension.super.initServer(server);

        var generationBaseUri = getGenerationBaseUri();
        var pathPrefix = getApplicationPathPrefix();
        var serverPublicUri = getServerPublicUri();

        if (!generationBaseUri.toString().startsWith(serverPublicUri)) {
            var errorMessage = String.format(
                    "Configuration error : the base generation URI must start with the instance public URI for the dereferencing module to work properly. Base generation URI <%s> does not start with instance public URI <%s>. Please change the configuration or disable the dereferencing module.",
                    generationBaseUri, serverPublicUri);
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        initRedirect(server, pathPrefix, generationBaseUri);
        initDocGen(server, pathPrefix);
    }

    private void initRedirect(Server server, String pathPrefix, URI baseGenerationUri) throws Exception {
        var context = server.initApp(pathPrefix + "/id", "/", "/", DerefModule.class);
        context.setJarScanner(new IgnoreJarScanner());

        var valve = new DerefRewriteValve(baseGenerationUri);
        context.getPipeline().addValve(valve);
        valve.initRules();
    }

    private void initDocGen(Server server, String pathPrefix) {
        var context = server.initApp(pathPrefix + "/about", "/", "/", DerefModule.class);
        context.setJarScanner(new IgnoreJarScanner());

        Tomcat.addServlet(context, "DerefServlet", new DerefServlet());
        context.addServletMappingDecoded("/*", "DerefServlet");
    }
}
