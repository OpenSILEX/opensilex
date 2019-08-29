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
import org.apache.tomcat.JarScanFilter;
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

    public static Server start(OpenSilex app, Path tomcatDirectory, String host, int port, int adminPort) throws LifecycleException {
        Server server = new Server(app, host, port, adminPort, tomcatDirectory);
        server.start();

        return server;
    }

    private final OpenSilex instance;

    /**
     * Construct OpenSilex server with host, port and adminPort adminPort is
     * used to communicate with the running server by the cli
     *
     * @param host
     * @param port
     * @param adminPort
     */
    public Server(OpenSilex instance, String host, int port, int adminPort, Path tomcatDirectory) {
        super();
        System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");

        this.instance = instance;
        String baseDir = tomcatDirectory.toFile().getAbsolutePath();
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

        } catch (IOException ex) {
            //TODO : Log error;
        }

        getConnector();

        initAdminThread(adminPort);
    }

    /**
     * Initialize OpenSilex Root webapp (swagger and jersey services) located at
     * the root "/" of the server.
     */
    public void initApp(String name, String rootPath, String baseDirectory, Class<?> moduleClass) {
        try {
            Context context = addWebapp(name, new File(".").getAbsolutePath());
            WebResourceRoot resource = new StandardRoot(context);
            File jarFile = ClassInfo.getJarFile(moduleClass);
            if (jarFile.isFile()) {
                resource.createWebResourceSet(WebResourceRoot.ResourceSetType.RESOURCE_JAR, rootPath, jarFile.getCanonicalPath(), null, baseDirectory);
                context.getJarScanner().setJarScanFilter(new JarScanFilter() {
                    @Override
                    public boolean check(JarScanType jarScanType, String jarName) {
                        return jarName.equals(jarFile.getName());
                    }
                });
            } else {
                resource.createWebResourceSet(WebResourceRoot.ResourceSetType.PRE, rootPath, jarFile.getCanonicalPath(), null, baseDirectory);
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

    public void stop() throws LifecycleException {
        instance.clean();
        super.stop();
    }

    /**
     * Load war application at the given contextPath (root url of the war)
     *
     * @param contextPath
     * @param warFile
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
     * @param adminPort
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
