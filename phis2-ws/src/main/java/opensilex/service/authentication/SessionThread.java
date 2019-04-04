//******************************************************************************
//                                SessionThread.java 
// SILEX-PHIS
// Copyright © INRA 2015
// Creation date: 25 November 2015
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.PropertiesFileManager;

/**
 * Thread to manage sessions.
 * @see DbConnector
 * @update [Arnaud Charleroy] May 2016: Add TokenDAO, logs. Modifications of
 * the property files
 * @author Samuël Chérimont
 */
public class SessionThread extends Thread {
    private static final String propsFileName = "service";
    final static Logger LOGGER = LoggerFactory.getLogger(SessionThread.class);
    
    private final String id, username;
    private boolean cmp;
    private Integer sessionTime; //sleep en millisecond

    public SessionThread(String id, String username) {
        this.id = id;
        this.username = username;
        cmp = true;     
        this.setTimeSession();
    }
    
    private void setTimeSession(){
        try {
            this.sessionTime = Integer.valueOf(PropertiesFileManager.getConfigFileProperty(propsFileName, "sessionTime")) * 1000;
        } catch (NumberFormatException e) {
            LOGGER.error("Error : No session time defined or file parsing error for "+ propsFileName +" properties file", e);
        }
    }

    /**
     * Runs the current thread. It is paused for a certain amount of time which 
     * can be extended according to the user's behaviour.
     * Deletes the corresponding session from the active sessions list.
     * @see phenomeapi.service.model.brapi.authentication.TokenDaoPhisBraphi,TokenManager.removeSession()
     * @update [Arnaud Charleroy] 9 Feb. 2016: doesn't update the database 
     * anymore. Moved to the manager + session time properties.
     */
    @Override
    public void run() {
        try {
            while (cmp != false) {
                cmp = false;
                SessionThread.sleep(this.sessionTime);
            }
        } catch (InterruptedException ex) {
             LOGGER.info("The session was interrupted", ex);
        }
        TokenManager.Instance().removeSession(this.id);
        this.interrupt();
    }

    /**
     * Increases a user connection time. It pauses the thread which delays the 
     * session end.
     */
    public void addTime() {
        this.cmp = true;
    }
    
    public String getSessionId() {
        return this.id;
    }
}
