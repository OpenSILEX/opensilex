//******************************************************************************
//                              Server.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server;

import org.opensilex.server.admin.ServerAdmin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.IOTools;
import org.apache.catalina.valves.StuckThreadDetectionValve;
import org.apache.catalina.valves.rewrite.RewriteValve;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.commons.io.FileUtils;
import org.apache.jasper.servlet.JasperInitializer;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.opensilex.OpenSilex;
import org.opensilex.server.extensions.ServerExtension;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensilex.OpenSilexModule;
import org.opensilex.OpenSilexModuleNotFoundException;

/**
 * <pre>
 * This class extends Tomcat server to embbeded.
 * - Set configuration in constructor
 * - Load swagger "root" application
 * - Call initServer method for all modules implementing {@code org.opensilex.module.extensions.ServerExtension}
 * - Enable GZIP compression
 * - Enable admin thread to remotly manage server
 * - Call shutDownServer method for all modules implementing {@code org.opensilex.module.extensions.ServerExtension}
 * </pre>
 *
 * @author Vincent Migot
 */
public class Server extends Tomcat {

    /**
     * Class Logger.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(Server.class);

    /**
     * Base directory for tomcat server.
     */
    private final String baseDir;

    /**
     * OpenSilex application instance.
     */
    private final OpenSilex instance;

    /**
     * Server configuration reference.
     */
    private final ServerConfig config;

    /**
     * Host name.
     */
    private final String host;

    /**
     * Tomcat port.
     */
    private final int port;

    /**
     * Administration port for server to be remotly managed.
     */
    private final int adminPort;

    /**
     * Threshold for thread lock check.
     */
    private final static int THREAD_LOCK_THRESHOLD = 120;

    /**
     * Inactivity threshold for thread lock interuption.
     */
    private final static int THREAD_LOCK_INTERRUPT_THRESHOLD = 30;

    /**
     * Construct OpenSilex server with host, port and adminPort adminPort is used to communicate with the running server by the cli.
     *
     * @param instance OpenSilex application instance
     * @param host Server hostname or IP
     * @param port Server port
     * @param adminPort Server administration port
     * @param tomcatDirectory Tomcat server base directory
     * @throws OpenSilexModuleNotFoundException If server module is not found (which should never happend)
     */
    public Server(OpenSilex instance, String host, int port, int adminPort, Path tomcatDirectory) throws OpenSilexModuleNotFoundException {
        super();

        // Set system properties
        System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");

        config = instance.getModuleConfig(ServerModule.class, ServerConfig.class);
        config.tomcatSystemProperties().forEach((key, value) -> {
            System.setProperty(key, value);
        });

        this.instance = instance;
        this.baseDir = tomcatDirectory.toFile().getAbsolutePath();
        this.host = host;
        this.port = port;
        this.adminPort = adminPort;
    }

    /**
     * Tomcat start method override.
     *
     * @throws LifecycleException
     */
    @Override
    public void start() throws LifecycleException {
        // Define properties
        setBaseDir(baseDir);
        setPort(port);
        setHostname(host);
        getHost().setAppBase(baseDir);
        getServer().setParentClassLoader(OpenSilex.getClassLoader());

        String pathPrefix = "";
        try {
            pathPrefix = instance.getModuleConfig(ServerModule.class, ServerConfig.class).pathPrefix();
        } catch (Exception ex) {
            LOGGER.error("Error while loading path prefix", ex);
        }

        if (!pathPrefix.equals("")) {
            try {
                Context redirectContext = addWebapp("defaultRedirect", new File(".").getAbsolutePath());
                RewriteValve valve = new RewriteValve();
                redirectContext.getPipeline().addValve(valve);
                String rewriteRules = "RewriteCond  %{REQUEST_URI} ^/$\n"
                        + "RewriteRule . " + pathPrefix + " [R=301,L,NE]\n";
                valve.setConfiguration(rewriteRules);
            } catch (Exception ex) {
                LOGGER.error("Error while setting default rewrite rules", ex);
            }
        }

        // Load Swagger root application
        Context appContext = initApp("", pathPrefix.equals("") ? "/" : pathPrefix, "/webapp", getClass());

        RewriteValve valve = new RewriteValve();
        appContext.getPipeline().addValve(valve);

        String rewritePrefix = pathPrefix + "/app/";
        try {
            String rewriteRules = "RewriteCond  %{REQUEST_URI} ^.*/webjars/.+$\n"
                    + "RewriteRule .*/webjars/(.+)$ /webjars/$1 [L,NE]\n"
                    + "RewriteMap uc org.opensilex.server.rest.RestRewriteMap\n"
                    + "RewriteRule .*/rest/.* ${uc:%{REQUEST_URI}}\n"
                    + "RewriteCond %{REQUEST_URI} ^/$\n"
                    + "RewriteRule . " + rewritePrefix + " [R=301,L,NE]\n"
                    + "RewriteCond %{REQUEST_URI} ^" + pathPrefix + "/?$\n"
                    + "RewriteRule .* " + rewritePrefix + " [R=301,L,NE]\n"
                    + "RewriteCond %{REQUEST_URI} ^" + rewritePrefix + ".+\n"
                    + "RewriteRule ^" + rewritePrefix + "(.*)$ " + rewritePrefix + "$1 [L,NE]\n";
            LOGGER.debug("Rewrite rules:\n" + rewriteRules);
            valve.setConfiguration(rewriteRules);
        } catch (Exception ex) {
            LOGGER.error("Error while setting rewrite rules", ex);
        }

        if (this.config.enableAntiThreadLock()) {
            // Prevent any thread to be stuck to long
            StuckThreadDetectionValve threadTimeoutValve = new StuckThreadDetectionValve();
            threadTimeoutValve.setThreshold(THREAD_LOCK_THRESHOLD);
            threadTimeoutValve.setInterruptThreadThreshold(THREAD_LOCK_INTERRUPT_THRESHOLD);
            appContext.getPipeline().addValve(threadTimeoutValve);
        }

        try {
            ClassUtils.listFilesByExtension(instance.getBaseDirectory() + "/webapps", "war", (File warfile) -> {
                String filename = warfile.getName();
                String context = "/" + filename.substring(0, filename.length() - ".war".length());
                addWarApp(context, warfile);
            });

        } catch (Throwable t) {
            LOGGER.error("Errow while loading war applications", t);
        }

        // Call initServer method for all modules implementing ServerExtension
        instance.getModulesImplementingInterface(ServerExtension.class).forEach((ServerExtension extension) -> {
            try {
                extension.initServer(this);
            } catch (Exception ex) {
                OpenSilexModule extensionModule = (OpenSilexModule) extension;
                LOGGER.error("Error while initilizing server extension for: " + extensionModule.getClass().getCanonicalName(), ex);
            }
        });

        Connector connector = getConnector();

        //Limit file size 100MB
        connector.setMaxPostSize(104857600);

        // Allow tomcat to accept encoded slash
        connector.setEncodedSolidusHandling(EncodedSolidusHandling.DECODE.getValue());

        // Enable GZIP compression
        enableGzip(connector);

        // enable UTF-8
        enableUTF8(connector);

        // Enable admin thread to manage server
        initAdminThread(adminPort);

        super.start();
    }

    /**
     * Tomcat stop method override.
     *
     * @throws LifecycleException
     */
    @Override
    public void stop() throws LifecycleException {
        // Call shutDownServer method for all modules implementing ServerExtension
        instance.getModulesImplementingInterface(ServerExtension.class).forEach((ServerExtension extension) -> {
            try {
                extension.shutDownServer(this);
            } catch (Exception ex) {
                OpenSilexModule extensionModule = (OpenSilexModule) extension;
                LOGGER.error("Error while stopping server extension for: " + extensionModule.getClass().getCanonicalName(), ex);
            }
        });

        super.stop();
    }

    /**
     * Initialize a new application from an opensilex module accessible througth the server.
     *
     * @param name Name of this application use an empty string to fine the ROOT application
     * @param contextPath Public URL path where the application will be served
     * @param baseDirectory Resource path of the web application whithin the module
     * @param moduleClass Class of the module where the application belong
     * @return Application context
     */
    public Context initApp(String name, String contextPath, String baseDirectory, Class<?> moduleClass) {
        try {
            // Define webapp context
            Context context = addWebapp(name, new File(".").getAbsolutePath());

            // Get module JAR file from it's class
            File jarFile = ClassUtils.getJarFile(moduleClass);

            // Define application resources
            WebResourceRoot resource = new StandardRoot(context);
            if (jarFile.isFile()) {
                // Define resources as a JAR file
                resource.createWebResourceSet(
                        WebResourceRoot.ResourceSetType.RESOURCE_JAR,
                        contextPath,
                        jarFile.getCanonicalPath(),
                        null,
                        baseDirectory
                );

            } else {
                // Define resources as a folder if module is a folder (DEV MODE)
                resource.createWebResourceSet(
                        WebResourceRoot.ResourceSetType.PRE,
                        contextPath,
                        jarFile.getCanonicalPath(),
                        null,
                        baseDirectory
                );
            }

            // Speed class scanning avoiding to scan useless dependencies
            Set<String> jarModules = new HashSet<>();
            this.instance.getModules().forEach((module) -> {
                File moduleJarFile = ClassUtils.getJarFile(module.getClass());
                if (moduleJarFile.getName().endsWith(".jar")) {
                    jarModules.add(moduleJarFile.getName());
                }
            });

            // Change application scanner to avoid land and unnecessary library scan
            context.getJarScanner().setJarScanFilter((JarScanType jarScanType, String jarName) -> {
                if (jarScanType == JarScanType.TLD) {
                    if (jarName.endsWith(".jar")) {
                        boolean shouldBeScan = jarModules.contains(jarName);
                        if (shouldBeScan) {
                            LOGGER.debug("Scan for Tomcat TLD: " + jarName);
                        }
                        return shouldBeScan;
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            });

            // Add resources to context
            context.setResources(resource);

            // Allow application to get opensilex instance through servlet context injection
            context.getServletContext().setAttribute("opensilex", instance);

            return context;

        } catch (IOException ex) {
            if (name.equals("")) {
                name = "/";
            }
            LOGGER.error("Can't initialize application:" + name, ex);
        }

        return null;
    }

    /**
     * Load war application at the given contextPath (root url of the war).
     *
     * @param contextPath Public URL path where the application will be served
     * @param warFile War file to be served
     */
    public void addWarApp(String contextPath, File warFile) {
        LOGGER.debug("Load war file: " + warFile.getAbsolutePath() + " at context: " + contextPath);

        try {
            Path basePath = Paths.get(getHost().getAppBase());

            File targetWar = basePath.resolve(warFile.getName()).toFile();

            try (InputStream warSource = FileUtils.openInputStream(warFile);
                    OutputStream output = new FileOutputStream(targetWar)) {
                IOTools.flow(warSource, output);
            }

            Context context = addWebapp(contextPath, targetWar.getAbsolutePath());
            context.addServletContainerInitializer(new JasperInitializer(), null);
            context.getServletContext().setAttribute("opensilex", instance);

        } catch (IOException ex) {
            LOGGER.error("[Context: " + contextPath + "] Can't add WAR file: " + warFile, ex);
        }
    }

    /**
     * Init administration thread on the given port to listen for commands execution from cli.
     *
     * @param adminPort The administration port where server listen to
     */
    private void initAdminThread(int adminPort) {
        try {
            Thread adminThread = new Thread(new ServerAdmin(this, adminPort));
            adminThread.setDaemon(true);
            adminThread.start();
        } catch (IOException ex) {
            LOGGER.warn("Can't initialize admin thread, tomcat must be killed in another way", ex);
        }
    }

    /**
     * Enable GZIP compression for current Tomcat service connector.
     *
     * @param connector
     */
    private void enableGzip(Connector connector) {
        connector.setProperty("compression", "on");
        connector.setProperty("compressionMinSize", "1024");
        connector.setProperty("noCompressionUserAgents", "gozilla, traviata");
        connector.setProperty("compressableMimeType", "text/html,text/xml, text/css, application/json, application/javascript");
    }

    /**
     * Enable UTF-8 support.
     *
     * @param connector
     */
    private void enableUTF8(Connector connector) {
        connector.setURIEncoding("UTF-8");
    }
}
