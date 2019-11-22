//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import org.apache.catalina.*;
import org.apache.commons.io.*;
import org.opensilex.*;
import org.opensilex.cli.help.*;
import org.opensilex.server.Server;
import org.opensilex.server.*;
import org.slf4j.*;
import picocli.CommandLine.*;

/**
 * This class regroup all commands concerning OpenSilex server operation 
 * - start: Start the server
 * - stop: Stop the server (using the admin port defined on start call)
 */
@Command(
        name = "server",
        header = "Subcommand to group OpenSILEX server operations"
)
public class ServerCommands extends HelpPrinterCommand implements OpenSilexCommand {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerCommands.class);
    
    /**
     * This function start the OpenSilex server with the given host, port and
     * adminPort If the daemon flag is set to true, this command will try to run
     * the server in another process
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
            @Option(names = {"--host"}, description = "Define server host", defaultValue = "localhost") String host,
            @Option(names = {"-p", "--port"}, description = "Define server port", defaultValue = "8666") int port,
            @Option(names = {"--adminPort"}, description = "Server port on which server is listening for admin commands", defaultValue = "8888") int adminPort,
            @Option(names = {"-d", "--daemon"}, description = "Run server as a daemon", defaultValue = "false") boolean daemon,
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

                // Create external process with given arguments
                ProcessBuilder pb = new ProcessBuilder(
                        System.getProperty("java.home") + "/bin/java",
                        "-jar",
                        jarFile.getAbsolutePath(),
                        "server",
                        "start",
                        "--host=" + host,
                        "--port=" + port,
                        "--adminPort=" + adminPort,
                        tomcatDirectory.toAbsolutePath().toString()
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
                        OpenSilex.getInstance(),
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
     * This command stop the OpenSilex server using the ServerAdminClient
     * listening on the given host and adminPort
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
            @Option(names = {"--host"}, description = "Define server host", defaultValue = "localhost") String host,
            @Option(names = {"-ap", "--adminPort"}, description = "Server port on which server is listening for commands", defaultValue = "8888") int adminPort,
            @Mixin HelpOption help
    ) throws Exception {
        // Create the server admin client
        ServerAdminClient adminClient = new ServerAdminClient(host, adminPort);
        
        // Call the stop server method
        adminClient.stopServer();
    }

    /**
     * Utility static function to start server in dev mode
     *
     * @param args unused
     */
    public static void main(String[] args) {
        MainCommand.main(new String[]{
            "server",
            "start",
             "--" + OpenSilex.PROFILE_ID_ARG_KEY + "=" + OpenSilex.DEV_PROFILE_ID
        });
    }
}
