//******************************************************************************
//                          ServerCommands.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.catalina.LifecycleException;
import org.apache.commons.io.FileUtils;
import org.opensilex.OpenSilex;
import org.opensilex.server.Server;
import org.opensilex.server.admin.ServerAdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * OpenSilex server command group.
 *
 * <pre>
 * This class regroup all commands concerning OpenSilex server operations:
 * - start: Start the server
 * - stop: Stop the server (using the admin port defined on start call)
 * </pre>
 *
 * @author Vincent Migot
 */
@Command(
        name = "server",
        header = "Subcommand to group OpenSILEX server operations"
)
public class ServerCommands extends AbstractOpenSilexCommand implements OpenSilexCommand {

    /**
     * Class Logger.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ServerCommands.class);

    /**
     * This function start the OpenSilex server with the given host, port and adminPort If the daemon flag is set to
     * true, this command will try to run the server in another process.
     *
     * @param host Server host name (default: localhost)
     * @param port Server port (default: 8666)
     * @param adminPort Server administration port (default: 8888)
     * @param daemon Flag to determine if server must be started as a dameon process (default: false)
     * @param tomcatDirectory Tomcat working directory (default: create a temporary directory)
     * @param help Helper parameter to allow help usage display for this command
     * @throws Exception Propagate any exception that could occurs
     */
    @Command(
            name = "start",
            header = "Start OpenSILEX server",
            description = "Start OpenSILEX server with given hostname and port"
    )
    public void start(
            @Option(
                    names = {"--host"},
                    description = "Define server host",
                    defaultValue = "localhost"
            ) String host,
            @Option(
                    names = {"-p", "--port"},
                    description = "Define server port",
                    defaultValue = "8666"
            ) int port,
            @Option(
                    names = {"--adminPort"},
                    description = "Server port on which server is listening for admin commands",
                    defaultValue = "8888"
            ) int adminPort,
            @Option(
                    names = {"-d", "--daemon"},
                    description = "Run server as a daemon",
                    defaultValue = "false"
            ) boolean daemon,
            @Parameters(description = "Tomcat directory", defaultValue = "") Path tomcatDirectory,
            @Mixin HelpOption help
    ) throws Exception {

        // If tomcat working directory is not defined create a temporary one
        if (tomcatDirectory.toString().equals("")) {
            final Path tmpDirectory = Files.createTempDirectory("opensilex");
            tomcatDirectory = tmpDirectory;

            // Add shutdown hook to delete temporary directory on application stop
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        FileUtils.deleteDirectory(tmpDirectory.toFile());
                    } catch (IOException ex) {
                        LOGGER.error("Error while deleting tomcat temporary directory", ex);
                    }
                }
            });
        }

        if (daemon) {
            // If daemon flag, start an external process
            try {
                // Get location of current jar file
                File jarFile = new File(ServerCommands.class.getProtectionDomain().getCodeSource().getLocation().toURI());

                OpenSilex instance = getOpenSilex();

                List<String> processArgs = new ArrayList<String>() {
                    {
                        add(System.getProperty("java.home") + "/bin/java");
                        //These next two lines are related to the java17-tomcat9.0.99 bug TODO delete if we ever fix this properly
                        add("--add-opens");
                        add("java.base/java.io=ALL-UNNAMED");
                        add("-jar");
                        add(jarFile.getAbsolutePath());
                        add("server");
                        add("start");
                        add("--host=" + host);
                        add("--port=" + port);
                        add("--adminPort=" + adminPort);
                        if (instance.isDebug()) {
                            add("--DEBUG");
                        }
                    }

                };

                if (instance.getConfigFile() != null) {
                    processArgs.add("--CONFIG_FILE=" + instance.getConfigFile().getCanonicalPath());
                }

                processArgs.add(tomcatDirectory.toAbsolutePath().toString());
                // Create external process with given arguments
                ProcessBuilder pb = new ProcessBuilder(
                        processArgs
                );

                // Start server in a detached process
                pb.start();
            } catch (IOException ex) {
                LOGGER.error("Can't start OpenSilex server as a daemon process", ex);
                throw ex;
            } catch (URISyntaxException ex) {
                LOGGER.error("Can't find jar file to execute as a daemon process", ex);
                throw ex;
            }
        } else {
            // Otherwise start OpenSilex server synchronously
            try {
                new Server(
                        getOpenSilex(),
                        host,
                        port,
                        adminPort,
                        tomcatDirectory
                ).start();
            } catch (LifecycleException ex) {
                LOGGER.error("Tomcat exception while starting OpenSilex server", ex);
                throw ex;
            }
        }
    }

    /**
     * This command stop the OpenSilex server using the ServerAdminClient listening on the given host and adminPort.
     *
     * @param host Server host name (default: localhost)
     * @param adminPort Server administration port (default: 8888)
     * @param help Helper parameter to allow help usage display for this command
     * @throws Exception Propagate any exception that could occurs
     */
    @Command(
            name = "stop",
            header = "Stop OpenSILEX server",
            description = "Stop OpenSILEX server with given hostname using admin port"
    )
    public void stop(
            @Option(
                    names = {"--host"},
                    description = "Define server host",
                    defaultValue = "localhost"
            ) String host,
            @Option(
                    names = {"-ap", "--adminPort"},
                    description = "Server port on which server is listening for commands",
                    defaultValue = "8888"
            ) int adminPort,
            @Mixin HelpOption help
    ) throws Exception {
        // Create the server admin client
        ServerAdminClient adminClient = new ServerAdminClient(host, adminPort);

        // Call the stop server method
        adminClient.stopServer();
    }

    /**
     * Utility static function to start server in dev mode.
     *
     * @param args unused
     * @throws Exception In case of any error during command execution
     */
    public static void main(String[] args) throws Exception {
        MainCommand.main(new String[]{
            "server",
            "start",
            "--" + OpenSilex.PROFILE_ID_ARG_KEY + "=" + OpenSilex.DEV_PROFILE_ID
        });
    }
}
