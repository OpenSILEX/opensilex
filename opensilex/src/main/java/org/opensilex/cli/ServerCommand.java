//******************************************************************************
//                            ServerCommand.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 02 April 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli;

import org.opensilex.cli.help.HelpPrinterCommand;
import org.opensilex.cli.help.HelpOption;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.catalina.LifecycleException;
import org.codehaus.plexus.util.FileUtils;
import org.opensilex.OpenSilex;
import org.opensilex.server.Server;
import org.opensilex.server.ServerAdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This class regroup all commands concerning OpenSilex server operation start,
 * stop
 */
@Command(
        name = "server",
        header = "Subcommand to group OpenSILEX server operations"
)
public class ServerCommand extends HelpPrinterCommand implements SubCommand {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerCommand.class);
    
    /**
     * This function start the OpenSilex server with the given host, port and
     * adminPort If the daemon flag is set to true, this command will try to run
     * the server in another process
     *
     * @param host
     * @param port
     * @param adminPort
     * @param daemon
     * @param profileId
     * @param baseDirectory
     * @param tomcatDirectory
     * @param help
     * @throws Exception
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

        if (tomcatDirectory.toString().equals("")) {
            final Path tmpDirectory = Files.createTempDirectory("opensilex");
            tomcatDirectory = tmpDirectory;
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        FileUtils.deleteDirectory(tmpDirectory.toFile());
                    } catch (IOException ex) {
                        // TODO LOG ERROR
                    }
                }
            });
        } else {
            tomcatDirectory = OpenSilex.getInstance().getBaseDirectory();
        }

        if (daemon) {
            try {
                File jarFile = new File(ServerCommand.class.getProtectionDomain().getCodeSource().getLocation().toURI());

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

                pb.start();
            } catch (IOException ex) {
                LOGGER.error("Can't start OpenSilex server as a daemon process", ex);
                throw ex;
            } catch (URISyntaxException ex) {
                LOGGER.error("Can't find jar file to execute as a daemon process", ex);
                throw ex;
            }
        } else {
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
     * @param host
     * @param adminPort
     * @param help
     * @throws Exception
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
        ServerAdminClient adminClient = new ServerAdminClient(host, adminPort);
        adminClient.stopServer();
    }

    /**
     * Utility static function to start server in dev mode
     *
     * @param args
     */
    public static void main(String[] args) {
        MainCommand.main(new String[]{
            "server",
            "start",
             "--" + OpenSilex.PROFILE_ID_ARG_KEY + "=" + OpenSilex.DEV_PROFILE_ID
        });
    }
}
