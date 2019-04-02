//******************************************************************************
//                                 Session.java 
// SILEX-PHIS
// Copyright © INRA 2015
// Creation date: 25 November 2015
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.authentication;

import java.sql.SQLException;
import java.util.ArrayList;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.dao.SessionDAO;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.ResponseFormGET;

/**
 * Session manager.
 * Manages sessions and authentications using the singleton pattern.
 * @author Samuël Chérimont
 */
public class TokenManager {

    static final Logger LOGGER = LoggerFactory.getLogger(TokenManager.class);
    private ArrayList<Session> listSession;
    private ArrayList<SessionThread> listThread;
    private static TokenManager _instance = null;

    /**
     * Constructs a unique instance.
     * @return a unique instance
     */
    public static TokenManager Instance() {
        if (_instance == null) {
            _instance = new TokenManager();
        }
        return _instance;
    }

    /**
     * Creates and runs a new thread for each new user.
     * @param sessionId
     * @param username
     */
    private void addThread(String sessionId, String username) {
        if (this.listThread == null) {
            this.listThread = new ArrayList();
        }
        this.removeEmptyThread();
        SessionThread newThread = new SessionThread(sessionId, username);
        listThread.add(newThread);
        newThread.start();
    }

    private void removeThread(String id) {
        int i = 0;
        boolean interrupted = false;
        while (i < listThread.size() && !interrupted) {
            if (listThread.get(i).getSessionId().equals(id)) {
                listThread.get(i).interrupt();
                listThread.remove(listThread.get(i));
                interrupted = true;
            }
            i++;
        }
    }
    
    /**
     * Increases a user connection time.
     * @param userConnectionId
     * @see SessionThread
     */
    public void reloadToken(String userConnectionId) {
        if (userConnectionId != null && (listThread != null && !listThread.isEmpty())) {
            int i = 0;
            while (i < listThread.size()) {
                if (this.listThread.get(i).getSessionId().equals(userConnectionId)) {
                    this.listThread.get(i).addTime();
                    return;
                }
                i++;
            }
        }
    }

    /**
     * Removes every finished session thread from the active thread list.
     */
    private void removeEmptyThread() {
        int i = 0;
        while (i < listThread.size()) {
            if (!listThread.get(i).isAlive()) {
                listThread.remove(listThread.get(i));
            }
            i++;
        }
    }

    /**
     * Searches if a user belongs to the active sessions list.
     * @param userName
     * @return the session id or nothing if not found.
     */
    public String searchSession(String userName) {
        if (listSession != null && !listSession.isEmpty()) {
            int i = 0;
            while (i < listSession.size()) {
                if (userName.equals(listSession.get(i).getName())) {
                    return listSession.get(i).getId();
                }
                i++;
            }
        }
        return null;
    }

    public Session getSession(String id) {
        if (listSession != null && !listSession.isEmpty()) {
            int i = 0;
            while (i < listSession.size()) {
                if (id.equals(listSession.get(i).getId())) {
                    return listSession.get(i);
                }
                i++;
            }
        }
        return null;
    }

    /**
     * Adds a session the active sessions list.
     * @param newActiveSession
     * @note The method can be private. In this case replace the unit test of 
     * checkAuthentification()
     * //SILEX:todo
     * Arnaud Charleroy: add a session to the manager = add in the database, 
     * or later DAO
     * //\SILEX:todo
     */
    public void addSession(Session newActiveSession) {
        if (this.listSession == null) {
            this.listSession = new ArrayList();
        }
        this.listSession.add(newActiveSession);

        SessionDAO sessionDao = new SessionDAO(); 
        try {
            newActiveSession.setDateStart(new DateTime().toString("yyyy-MM-dd HH:mm:ss"));
            sessionDao.insertOrUpdateOrDeleteQueryFromDAO("INSERT INTO session (email, id, date) VALUES ('" + newActiveSession.getName() + "', '" + newActiveSession.getId() + "', now())");
        } catch (SQLException ex) {
            final Status status = new Status("Can't create session token", StatusCodeMsg.ERR, ex.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ResponseFormGET(status)).build());
        } finally {
        }
    }

    /**
     * Removes the session from the active sessions list and updates the database
     * setting the session end date.
     * @param sessionId
     */
    public void removeSession(String sessionId) {
        if (listSession == null || listSession.isEmpty()) {
            return;
        }
        SessionDAO sessionDao = new SessionDAO(); 
        int i = 0;
        boolean find = false;
        while (i < listSession.size() && !find) {
           String sessionid = listSession.get(i).getId(); 
            if (sessionId.equals(sessionid)) { 
                sessionDao.endSession(sessionid); 
                listSession.remove(i);
                find = true;
            }
            i++;
        }
        this.removeThread(sessionId);
    }

    /**
     * Adds a session the the active sessions list and creates a new thread to 
     * handle this session.
     * @param newActiveSession
     */
    public void createToken(Session newActiveSession) {
        this.addSession(newActiveSession);
        this.addThread(newActiveSession.getId(), newActiveSession.getName());
    }

     /** 
     * Adds an former session to the active sessions list and creates a new 
     * thread to handle this session.
     * @param newActiveSession 
     */ 
    public void createTokenFromBD(Session newActiveSession) { 
        if (this.listSession == null) { 
            this.listSession = new ArrayList(); 
        } 
        this.listSession.add(newActiveSession); 
        this.addThread(newActiveSession.getId(), newActiveSession.getName()); 
    } 
    
    /**
     * Checks that the session is still valid and adds connection time if it is
     * the case
     * @param sessionId
     * @return true if valid
     *         false if not
     */
    public boolean checkAuthentication(String sessionId) {
        if (sessionId != null && (this.listSession != null && !this.listSession.isEmpty())) {
            int i = 0;
            while (i < listSession.size()) {
                if (sessionId.equals(listSession.get(i).getId())) {
                    return true;
                }
                i++;
            }
        }
        return false;
    }

    public void shutdown() {
        if (this.listSession != null && !this.listSession.isEmpty() && listThread != null && !listThread.isEmpty()) {
            int i = 0;
            while (i < listThread.size()) {
                this.removeSession(listThread.get(i).getSessionId()); 
                listThread.get(i).interrupt();
                listThread.remove(listThread.get(i));
            }
            listThread = null;
        }
    }
}
