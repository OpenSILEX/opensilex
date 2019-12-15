//******************************************************************************
//                        ServerAdminClient.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;


/**
 * Client for the ServerAdmin class used to send commands to a runnning server.
 * 
 * @author Vincent Migot
 */
public class ServerAdminClient {

    /**
     * Socket to connect to the admin server socket
     */
    private Socket connection;

    /**
     * Reader for data send by the admin server
     */
    private BufferedReader reader;

    /**
     * Output stream to send commands to the admin server
     */
    private DataOutputStream output;

    /**
     * Initialize connection to the admin server socket
     *
     * @param host Server host to connect with
     * @param adminPort Server administration port to connect with
     * 
     * @throws IOException In case of connection problem to administration server
     */
    public ServerAdminClient(String host, int adminPort) throws IOException {
        connection = new Socket(host, adminPort);
        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        output = new DataOutputStream(connection.getOutputStream());
    }

    /**
     * Stop the tomcat server and close the connection after it
     *
     * @throws IOException In case of communication problem with administration server
     */
    public void stopServer() throws IOException {
        sendCommand(ServerAdmin.SHUTDOWN_COMMAND);
        closeConnection();
    }

    /**
     * Send a command and read all response from admin server
     *
     * @param command
     * @throws IOException
     */
    private void sendCommand(String command) throws IOException {
        output.writeUTF(command);
        output.flush();
        output.close();
        while (!connection.isClosed()) {
            try {
                // TODO read correctly data send by admin server
                System.out.println(reader.readLine());
            } catch (IOException ex) {
                // TODO manage exception
            }
        }
    }

    /**
     * Close the connection to the admin server
     *
     * @throws IOException
     */
    private void closeConnection() throws IOException {
        reader.close();
        output.close();
        connection.close();
    }
}
