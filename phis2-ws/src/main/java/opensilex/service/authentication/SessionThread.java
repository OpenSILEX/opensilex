//******************************************************************************
//                                 Session.java 
// SILEX-PHIS
// Copyright © INRA 2015
// Creation date: November 2015
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.PropertiesFileManager;

/**
 * SessionThread - Extension de la classe Thread Prend en charge la gestion du temps
 de connection d'une session
 *
 * @version1.0
 *
 * @author Samuël Chérimont
 * @see DbConnector
 * @date 25/11/2015
 * @update AC 05/2016 Ajout TokenDAO, log et modification des fichiers de propriétés
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
    
    /**
     * Update 20/06/14 Temps en seconde *1000
     */
   private void setTimeSession(){
            try {
                this.sessionTime = Integer.valueOf(PropertiesFileManager.getConfigFileProperty(propsFileName, "sessionTime")) * 1000;
            } catch (NumberFormatException e) {
                LOGGER.error("Error : No session time defined or file parsing error for "+ propsFileName +" properties file", e);
            }
   }

    /**
     * run() - Execution du thread actuel Le thread est mis en pause un certain
     * temps, qui peut etre allongé en fonction du comportement de
     * l'utilisateur et
     * supprime l'objet Session correspondant de la liste des sessions actives
     *
     * @see phenomeapi.service.model.brapi.authentication.TokenDaoPhisBraphi,TokenManager.removeSession()
     * @date 25/11/2015
     * @update 09/02/2016 AT : ne met plus la bd à jour, déplacé dans le manager + properties du temps de session
     * 
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
     * addTime - Permet d'augmenter le temps de connection d'un utilisateur
     * Replace le compteur utilisé dans run() sur true ce qui met le thread sur
     * pause plus longtemps et donc retarde la fin de la session correspondante
     *
     * @see run()
     * @date 25/11/2015
     */
    public void addTime() {
        this.cmp = true;
    }

    /**
     * getSessionId - Récupère l'identifiant de la session gérée par cette
 instance de SessionThread
     *
     * @return l'identifiant de de la session
     *
     * @date 25/11/2015
     */
    public String getSessionId() {
        return this.id;
    }
}
