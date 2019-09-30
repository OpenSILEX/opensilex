//******************************************************************************
//                       ServerAdmin.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 02 April 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server administration tool used to execute recieved commands from the command
 * line while tomcat server is running
 */
public class ServerAdmin implements Runnable {

    /**
     * Command to shutdown tomcat server
     */
    public static final String SHUTDOWN_COMMAND = "SHUTDOWN_OPENSILEX";

    /**
     * Instance of the managed server
     */
    private final Server instance;

    /**
     * Socket on which this thread is listening
     */
    private final ServerSocket adminSocket;

    /**
     * Create the admin socket for the given server instance on the
     * corresponding adminPort
     *
     * @param instance Server instance controlled by this administration thread
     * @param adminPort Administration port to listen on
     * @throws IOException In case of failure while initializing server socket
     */
    public ServerAdmin(Server instance, int adminPort) throws IOException {
        this.instance = instance;
        this.adminSocket = new ServerSocket(adminPort);
    }

    /**
     * Main method listening for connections and interpreting commands
     */
    @Override
    public void run() {
        try {
            while (!adminSocket.isClosed()) {
                boolean stop = false;
                // blocks until a packet is received
                Socket socket = adminSocket.accept();
                try {
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    PrintStream output = new PrintStream(socket.getOutputStream());
                    while (!stop) {

                        String message = input.readUTF();
                        if (message.equals(SHUTDOWN_COMMAND)) {
                            // TODO redirect server output to the calling socket client
                            output.println("WTF");
                            output.flush();
                            System.setOut(output);
                            System.setErr(output);
                            instance.stop();
                            instance.destroy();
                            adminSocket.close();
                            stop = true;
                        }
                    }

                } catch (Exception socketEx) {
                    socketEx.printStackTrace(System.err);
                    //TODO manage exception
                } finally {
                    socket.close();
                }
            }
        } catch (Exception ex) {
            //TODO manage exception
            ex.printStackTrace(System.err);
        }
    }
}
