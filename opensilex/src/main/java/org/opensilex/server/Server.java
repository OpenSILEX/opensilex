//******************************************************************************
//                            Server.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 15 March 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server;

import org.opensilex.module.extensions.ServerExtension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.IOTools;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.commons.io.FileUtils;
import org.apache.jasper.servlet.JasperInitializer;
import org.apache.tomcat.JarScanType;
import org.opensilex.OpenSilex;
import org.opensilex.fs.FileStorageService;
import org.opensilex.utils.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends Tomcat server to embbeded it and load OpenSilex services,
 * rdf4j server and workbench
 */
public class Server extends Tomcat {

    private final static Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private final String baseDir;
    private final OpenSilex instance;
    private final String host;
    private final int port;
    private final int adminPort;

    /**
     * Construct OpenSilex server with host, port and adminPort adminPort is
     * used to communicate with the running server by the cli
     * 
     * @param instance OpenSilex application instance
     * @param host Server hostname or IP
     * @param port Server port
     * @param adminPort Server administration port
     * @param tomcatDirectory Tomcat server base directory
     */
    public Server(OpenSilex instance, String host, int port, int adminPort, Path tomcatDirectory) {
        super();
        System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");

        this.instance = instance;
        this.baseDir = tomcatDirectory.toFile().getAbsolutePath();
        this.host = host;
        this.port = port;
        this.adminPort = adminPort;
    }

    @Override
    public void start() throws LifecycleException {
        setBaseDir(baseDir);
        setPort(port);
        setHostname(host);
        getHost().setAppBase(baseDir);
        getServer().setParentClassLoader(Thread.currentThread().getContextClassLoader());

        initApp("", "/", "/webapp", getClass());

        instance.getModulesImplementingInterface(ServerExtension.class).forEach((ServerExtension extension) -> {
            extension.initServer(this);
        });

        FileStorageService fs = instance.getServiceInstance("fs", FileStorageService.class);
        try {
            fs.listFilesByExtension(instance.getBaseDirectory() + "/webapps", "war", (File warfile) -> {
                String filename = warfile.getName();
                String context = "/" + filename.substring(0, filename.length() - 4);
                addWarApp(context, warfile);
            });

        } catch (Throwable t) {
            LOGGER.error("Errow while loading war applications", t);
        }

        getConnector();

        initAdminThread(adminPort);

        super.start();
    }

    /**
     * Initialize a new application from an opensilex module accessible througth the server
     * 
     * @param name Name of this application use an empty string to fine the ROOT application
     * @param contextPath Public URL path where the application will be served
     * @param baseDirectory Resource path of the web application whithin the module
     * @param moduleClass Class of the module where the application belong
     */
    public void initApp(String name, String contextPath, String baseDirectory, Class<?> moduleClass) {
        try {
            Context context = addWebapp(name, new File(".").getAbsolutePath());
            WebResourceRoot resource = new StandardRoot(context);
            File jarFile = ClassInfo.getJarFile(moduleClass);
            if (jarFile.isFile()) {
                resource.createWebResourceSet(WebResourceRoot.ResourceSetType.RESOURCE_JAR, contextPath, jarFile.getCanonicalPath(), null, baseDirectory);
                context.getJarScanner().setJarScanFilter((JarScanType jarScanType, String jarName) -> {
                    return jarName.equals(jarFile.getName());
                });
            } else {
                resource.createWebResourceSet(WebResourceRoot.ResourceSetType.PRE, contextPath, jarFile.getCanonicalPath(), null, baseDirectory);
            }
            context.getServletContext().setAttribute("opensilex", instance);
            context.setResources(resource);

        } catch (IOException ex) {
            if (name.equals("")) {
                name = "/";
            }
            LOGGER.error("Can't initialize application:" + name, ex);
        }
    }

    /**
     * Load war application at the given contextPath (root url of the war)
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
     * Init administration thread on the given port to listen for commands
     * execution from cli
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

}
