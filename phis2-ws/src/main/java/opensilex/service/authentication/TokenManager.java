package opensilex.service.authentication;

import opensilex.service.dao.phis.UserDAO;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.PropertiesFileManager;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.dao.phis.SessionDAO;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.ResponseFormGET;

/**
 * Connection Manager - Permet la gestion de toutes les sessions et
 * authentification par l'intermédiaire d'une seule instance de cette classe
 *
 * @version1.0
 * @author Samuël Chérimont
 * @date 25/11/2015
 */
public class TokenManager {

    static final Logger LOGGER = LoggerFactory.getLogger(TokenManager.class);
    private ArrayList<Session> listSession;
    private ArrayList<SessionThread> listThread;
    private static TokenManager _instance = null;

    /**
     * Instance() - Méthode de classe permettant de créer une instance unique de
     * TokenManager Cette méthode doit être appelée à la place du constructeur
     * de la classe TokenManager
     *
     * exemple ConnectionManger instance = TokenManager.Instance();
     *
     * @return une instance unique de TokenManager
     * @date 26/11/2015
     */
    public static TokenManager Instance() {
        if (_instance == null) {
            _instance = new TokenManager();
        }
        return _instance;
    }

    /**
     * addThread() - Méthode privée appelée dans createToken() Crée un objet
     * SessionThread pour chaque nouvel utilisateur qui se connecte, l'ajoute à
     * une liste d'où il sera accessible et lance le nouveau thread
     *
     * @param id Identifiant de session
     * @param username Nom de l'utilisateur
     *
     * @see SessionThreadcreateToken()
     * @date 26/11/2015
     */
    private void addThread(String id, String username) {
        if (this.listThread == null) {
            this.listThread = new ArrayList();
        }
        this.removeEmptyThread();
        SessionThread newThread = new SessionThread(id, username);
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
     * reloadToken() - Rajoute du temps de connection à un utilisateur identifié
     * par son id
     *
     * @param id Identifiant de connection de l'utilisateur
     *
     * @see SessionThread
     * @date 26/11/2015
     */
    public void reloadToken(String id) {
        if (id != null && (listThread != null && !listThread.isEmpty())) {
            int i = 0;
            while (i < listThread.size()) {
                if (this.listThread.get(i).getSessionId().equals(id)) {
                    this.listThread.get(i).addTime();
                    return;
                }
                i++;
            }
        }
    }

    /**
     * removeEmptyThread - Supprime tout objet SessionThread ayant terminé son
     * execution de la liste de threads
     *
     * @date 26/11/2015
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
     * searchSession() - Méthode appelée au moment de l'authentification
     * Recherche si l'utilisateur identifié par name est présent dans la liste
     * des sessions actives
     *
     * @param name Nom de l'utilisateur
     * @return une String correspondant a l'identifiant de session de
     * l'utilisateur ou null si aucune session n'est active pour cet utilisateur
     *
     * @see Token.getConnection()
     * @date 26/11/2015
     */
    public String searchSession(String name) {
        if (listSession != null && !listSession.isEmpty()) {
            int i = 0;
            while (i < listSession.size()) {
                if (name.equals(listSession.get(i).getName())) {
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
     * addSession() - Méthode appelée par createToken() Ajoute un objet Session
     * à la liste des sessions actives
     *
     * @param session Un objet Session représentant une nouvelle session active
     *
     * @see createSession()
     * @date 25/11/2015
     * @note L'accès a cette méthode peut être remplacée par private, dans ce
     * cas il faut changer le test unitaire de checkAuthentification()
     *
     * @update 09/02/2016
     * @info AT, ajout d'une session au manager = ajout dans la bd, voir plus
     * tard dao
     */
    public void addSession(Session session) {
        if (this.listSession == null) {
            this.listSession = new ArrayList();
        }
        this.listSession.add(session);
//        logger.debug(this.listSession.toString());
        //BD
       SessionDAO sessionDao = new SessionDAO(); 
        try {
            session.setDateStart(new DateTime().toString("yyyy-MM-dd HH:mm:ss"));
            sessionDao.insertOrUpdateOrDeleteQueryFromDAO("INSERT INTO session (email, id, date) VALUES ('" + session.getName() + "', '" + session.getId() + "', now())");
        } catch (SQLException ex) {
            final Status status = new Status("Can't create session token", StatusCodeMsg.ERR, ex.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ResponseFormGET(status)).build());
        } finally {
        }
    }

    /**
     * removeSession - Méthode appelée à la fin de l'execution d'un
     * SessionThread Supprime un objet Session de la liste des sessions actives
     * et met a jour la bd en ajoutant la date de fin de validité de la session
     *
     * @param id Identifiant de session
     *
     * @see MyThread.run()
     * @date 25/11/2015
     *
     * @update 09/02/2016
     * @info AT lien bd pour invalider la session, déplacé ici
     */
    public void removeSession(String id) {
        if (listSession == null || listSession.isEmpty()) {
            return;
        }
        SessionDAO sessionDao = new SessionDAO(); 
        int i = 0;
        boolean find = false;
        while (i < listSession.size() && !find) {
           String sessionid = listSession.get(i).getId(); 
            if (id.equals(sessionid)) { 
                sessionDao.endSession(sessionid); 
                listSession.remove(i);
                find = true;
            }
            i++;
        }
        this.removeThread(id);
    }

    /**
     * createToken() - Méthode appelée à chaque nouvelle authentification
     * réussie Ajoute une session a la liste des sessions actives et crée un
     * nouveau thread qui va gérer cette session
     *
     * @param session Un objet Session représentant une nouvelle session active
     *
     * @see addSession(), addThread(), DbConnector.getConnection()
     * @date 25/11/2015
     */
    public void createToken(Session session) {
        this.addSession(session);
        this.addThread(session.getId(), session.getName());
    }

    
     /** 
     * createTokenFromBD() - Méthode appelée à chaque nouvelle authentification 
     * réussie Ajoute une ancienne session a la liste des sessions actives et crée un 
     * nouveau thread qui va gérer cette session 
     * 
     * @param session Un objet Session représentant une nouvelle session active 
     * 
     * @see addSession(), addThread(), DbConnector.getConnection() 
     * @date 25/11/2015 
     */ 
    public void createTokenFromBD(Session session) { 
        if (this.listSession == null) { 
            this.listSession = new ArrayList(); 
        } 
        this.listSession.add(session); 
        this.addThread(session.getId(), session.getName()); 
//        logger.debug(JsonConverter.ConvertToJson(this.listSession)); 
    } 
    
    /**
     * checkAuthentification() - Vérifie que la session déterminée par son id
     * est encore valable et rajoute du temps de connection si la session est
     * valide
     *
     * @param id L'identifiant de la session
     * @return true si la session est valide ou false si elle ne l'est pas
     *
     * @see reloadToken()
     * @date 25/11/2015
     */
    public boolean checkAuthentification(String id) {
        if (id != null && (this.listSession != null && !this.listSession.isEmpty())) {
            int i = 0;
            while (i < listSession.size()) {
                if (id.equals(listSession.get(i).getId())) {
//                    this.reloadToken(id);
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
