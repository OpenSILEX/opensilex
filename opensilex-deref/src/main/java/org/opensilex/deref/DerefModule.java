package org.opensilex.deref;

import org.apache.catalina.startup.Tomcat;
import org.opensilex.OpenSilexModule;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.server.Server;
import org.opensilex.server.ServerConfig;
import org.opensilex.server.ServerModule;
import org.opensilex.server.extensions.ServerExtension;
import org.opensilex.server.scanner.IgnoreJarScanner;

public class DerefModule extends OpenSilexModule implements ServerExtension {
    public String getApplicationPathPrefix() {
        try {
            var cfg = getOpenSilex().getModuleConfig(ServerModule.class, ServerConfig.class);
            return cfg.pathPrefix();
        } catch (OpenSilexModuleNotFoundException ex) {
            return "";
        }
    }

    @Override
    public void initServer(Server server) throws Exception {
        ServerExtension.super.initServer(server);

        var context = server.initApp(getApplicationPathPrefix() + "/id", "/", "/", DerefModule.class);
        context.setJarScanner(new IgnoreJarScanner());

        var valve = new DerefRewriteValve();
        context.getPipeline().addValve(valve);
        valve.initRules();

        Tomcat.addServlet(context, "DerefServlet", new DerefServlet());
        context.addServletMappingDecoded("/deref", "DerefServlet");
    }
}
