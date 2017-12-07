package phis2ws.service.authentication;

import phis2ws.service.model.User;

/**
 * Session - Permet de créer des objets correspondant aux différentes sessions
 * utilisateur
 *
 * @version1.0
 *
 * @author Samuël Chérimont
 * @date 25/11/2015
 * @note Les champs dateStart et dateEnd ne sont pas utilisés pour le moment
 * @update Anraud CHARLEROY Définir SQLDBModel et les cahmps uniques dans
 * Session
 */
public class Session {

    private String dateStart;
    private String dateEnd;
    private String id;
    private String name;
    private User user;

    public Session() {
    }

    public Session(String dateStart, String dateEnd, String id, String name) {
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.id = id;
        this.name = name;
        this.user = new User(name);
    }

    public User getUser() {
        return user;
    }

    public String getDateStart() {
        return dateStart;
    }

    public String getDateEnd() {
        return dateEnd;
    }

    
    /**
     * Session() - Initialise tous les champs de l'objet Session
     *
     * @param id Identifiant de session
     * @param name Nom de l'utilisateur
     *
     * @see setDateStart(),setDateEnd(), setFamilyName(), setId()
     * @date 25/11/2015
     * @update AC 07/16 Modification utilisation des attributs privés plutôt que
     * de passer par des setters publiques
     */
    public Session(String id, String name) {
        this.dateStart = null;
        this.dateEnd = null;
        this.name = name;
        this.id = id;
        this.user = new User(name);
    }

    public Session(String id, String name, User u) {
        this.dateStart = null;
        this.dateEnd = null;
        this.name = name;
        this.id = id;
        this.user = u;
    }

    /**
     * getId() - Récupère l'identifiant ed la session
     *
     * @return l'identifiant de session
     * @date 25/11/2015
     */
    public String getId() {
        return this.id;
    }

    /**
     * getFamilyName() - Récupère le nom de l'utilisateur correspondant à la session
 représentée par l'instanec de Session
     *
     * @return le nom de l'utilisateur
     * @date 25/11/2015
     */
    public String getName() {
        return this.name;
    }

    /**
     * setDateStart() - Initialise le champ dateStart d'une instance de session
     * avec la date de connection de l'utilisateur
     *
     * @param dateStart Date de connection
     *
     * @date 25/11/2015
     * @note Inutilisée pour le moment
     */
    public void setDateStart(String dateStart) {
        this.dateStart = dateStart;
    }

    /**
     * setDateEnd() - Initialise le champ dateEnd d'une instance de Session avec
     * la date de fin de connection de l'utilisateur
     *
     * @param dateEnd date de fin de session
     *
     * @date 25/11/2015
     * @note Inutilisée pour le moment
     */
    public void setDateEnd(String dateEnd) {
        this.dateEnd = dateEnd;
    }

    /**
     * setid() - Initialise le champ id d'une instance de Session avec un
     * identifiant de session
     *
     * @param id identifiant de session
     * @date 25/11/2015
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * setFamilyName() - Initialise le champ name d'une instance de Session
     *
     * @param name Nom d'utilisateur
     * @date 25/11/2015
     */
    public void setName(String name) {
        this.name = name;
    }

}
